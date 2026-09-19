#!/bin/bash

usage() {
    echo "Usage: $0 -v <version> -p <platform>"
    echo "  -v <version>     Specify the version number (e.g., 0.7.1)"
    echo "  -p <platform>    Specify the platform (ios or apk or aab)"
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

if [[ "${PLATFORM}" != "ios" && "${PLATFORM}" != "apk"  && "${PLATFORM}" != "aab" ]]; then
    usage
fi

export PUB_HOSTED_URL=https://pub.dev
flutter clean

VERSION_AND_BUILD=$(awk -F"+" '/version:/{print $2}' pubspec.yaml)
BUILD_NUMBER=$(($VERSION_AND_BUILD + 1))

ENV=$(grep '^String env =' lib/env.dart | cut -d '"' -f 2)

awk -v vn="$VERSION_NUMBER" -v bn="$BUILD_NUMBER" '/version:/{$2=vn "+" bn}1' pubspec.yaml > temp && mv temp pubspec.yaml

if [ "$PLATFORM" == "ios" ]; then
    flutter build ios --release
elif [ "$PLATFORM" == "apk" ]; then
    flutter build apk --release
elif [ "$PLATFORM" == "aab" ]; then
    flutter build appbundle --release
else
    echo "Invalid argument. Please specify 'ios' or 'apk'."
fi
