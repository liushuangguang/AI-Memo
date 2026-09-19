#!/bin/bash

usage() {
    echo "Usage: $0 -v <version> -p <platform>"
    echo "  -v <version>     Specify the version number (e.g., 0.7.1)"
    echo "  -p <platform>    Specify the platform (ios or apk)"
    exit 1
}

while getopts ":v:p:" opt; do
    case ${opt} in
        v )
            VERSION_NUMBER=$OPTARG
            ;;
        p )
            PLATFORM=$OPTARG
            ;;
        \? )
            usage
            ;;
    esac
done
shift $((OPTIND -1))

if [ -z "${VERSION_NUMBER}" ] || [ -z "${PLATFORM}" ]; then
    usage
fi

if [[ "${PLATFORM}" != "ios" && "${PLATFORM}" != "apk" ]]; then
    usage
fi

export PUB_HOSTED_URL=https://pub.dev
flutter clean

update_ios_project_file() {
    local bundle_id="$1"
    local app_name="\"$2\""
    local project_file="ios/Runner.xcodeproj/project.pbxproj"

    sed -i '' "s/PRODUCT_BUNDLE_IDENTIFIER = .*;/PRODUCT_BUNDLE_IDENTIFIER = $bundle_id;/g" "$project_file"
    sed -i '' "s/INFOPLIST_KEY_CFBundleDisplayName = .*;/INFOPLIST_KEY_CFBundleDisplayName = $app_name;/g" "$project_file"
    sed -i '' "s/MARKETING_VERSION = .*;/MARKETING_VERSION = $VERSION_NUMBER;/g" "$project_file"
    sed -i '' "s/CURRENT_PROJECT_VERSION = .*;/CURRENT_PROJECT_VERSION = $BUILD_NUMBER;/g" "$project_file"
}

update_android_files() {
    local package_name="$1"
    local app_name="$2"

    local manifest_files=(
        "android/app/src/debug/AndroidManifest.xml"
        "android/app/src/main/AndroidManifest.xml"
        "android/app/src/profile/AndroidManifest.xml"
    )
    local build_gradle_file="android/app/build.gradle"
    local main_activity_file="android/app/src/main/kotlin/com/example/triplenty_frontend/MainActivity.kt"
    local kotlin_file="android/app/src/main/kotlin/TencentMapView.kt"
    local main_manifest_file="android/app/src/main/AndroidManifest.xml"

    for manifest_file in "${manifest_files[@]}"; do
        if [ -f "$manifest_file" ]; then
          sed -i '' 's/package=[^ ]*/package=\"'$package_name'\">/' "$manifest_file"
        else
            echo "File $manifest_file does not exist"
        fi
    done

    sed -i '' 's/applicationId [^ ]*/applicationId \"'$package_name'\"/' "$build_gradle_file"

    sed -i '' 's/package [^ ]*/package '$package_name'/' "$main_activity_file"

    sed -i '' 's/android:label=\"小旅星[^ ]*/android:label=\"'$app_name'\"/' "$main_manifest_file"

    sed -i '' 's/.*import com\.triplenty\.app.*/tobereplaced/' "$kotlin_file"

    sed -i '' '0,/tobereplaced/s//ccc/' "$kotlin_file"

#    awk '{if (c==0 && /tobereplaced/) {sub(/tobereplaced/, "import '$package_name'.MainActivity"); c++} print}' "$kotlin_file" > tmp && mv tmp "$kotlin_file"

    awk '{if (c==0 && /tobereplaced/) {sub(/tobereplaced/, "import '$package_name'.R"); c++} print}' "$kotlin_file" > tmp && mv tmp "$kotlin_file"

}

update_constants_file() {
    local constants_file="lib/constants.dart"
    sed -i '' "s/String appVersion = \".*\";/String appVersion = \"$VERSION_NUMBER\";/g" "$constants_file"
}

VERSION_AND_BUILD=$(awk -F"+" '/version:/{print $2}' pubspec.yaml)
BUILD_NUMBER=$(($VERSION_AND_BUILD + 1))

ENV=$(grep '^String env =' lib/env.dart | cut -d '"' -f 2)

update_constants_file

if [ "$PLATFORM" == "ios" ]; then
    if [ "$ENV" == "test" ]; then
        echo "Test environment detected"
        update_ios_project_file "com.triplenty.app.test" "小旅星-测试"
        /usr/libexec/PlistBuddy -c "Set :CFBundleDisplayName 小旅星-测试" ./ios/Runner/Info.plist
    elif [ "$ENV" == "dev" ]; then
        echo "Development environment detected"
        update_ios_project_file "com.triplenty.app.dev" "小旅星-Dev"
        /usr/libexec/PlistBuddy -c "Set :CFBundleDisplayName 小旅星-Dev" ./ios/Runner/Info.plist
    else
        echo "Production environment detected"
        update_ios_project_file "com.triplenty.app" "小旅星"
        /usr/libexec/PlistBuddy -c "Set :CFBundleDisplayName 小旅星" ./ios/Runner/Info.plist
    fi
    awk -v vn="$VERSION_NUMBER" -v bn="$BUILD_NUMBER" '/version:/{$2=vn "+" bn}1' pubspec.yaml > temp && mv temp pubspec.yaml
    flutter build ios --release
elif [ "$PLATFORM" == "apk" ]; then
    if [ "$ENV" == "test" ]; then
        echo "Test environment detected"
        update_android_files "com.triplenty.app.test" "小旅星-测试"
    elif [ "$ENV" == "dev" ]; then
        echo "Development environment detected"
        update_android_files "com.triplenty.app.dev" "小旅星-Dev"
    else
        echo "Production environment detected"
        update_android_files "com.triplenty.app" "小旅星"
    fi
    awk -v vn="$VERSION_NUMBER" -v bn="$BUILD_NUMBER" '/version:/{$2=vn "+" bn}1' pubspec.yaml > temp && mv temp pubspec.yaml
    flutter build apk --release
else
    echo "Invalid argument. Please specify 'ios' or 'apk'."
fi
