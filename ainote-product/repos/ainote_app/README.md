# ainote_app

ainote v2 全新版本

## use get_cli

+ 安装 get_cli

- [use getx](https://pub.dev/packages/get)
- [use get_cli](https://pub.dev/packages/get_cli)

+ 使用 get_cli

```
    # 安装
    pub global activate get_cli 
    // (to use this add the following to system PATH: [FlutterSDKInstallDir]\bin\cache\dart-sdk\bin
    
    flutter pub global activate get_cli
    
    // To create a flutter project in the current directory:
    // Note: By default it will take the folder's name as project name
    // You can name the project with `get create project:my_project`
    // If the name has spaces use `get create project:"my cool project"`
    
    # 创建项目
    get create project
    
    // To generate the chosen structure on an existing project:
    get init
    
    // To create a page:
    // (Pages have controller, view, and binding)
    // Note: you can use any name, ex: `get create page:login`
    // Nota: use this option if the chosen structure was Getx_pattern
    
    # 新增页面
    get create page:home
    
    // To create a screen
    // (Screens have controller, view, and binding)
    // Note: you can use any name, ex: `get screen page:login`
    // Nota: use this option if the chosen structure was CLEAN (by Arktekko)
    get create screen:home 
    
    // To create a new controller in a specific folder:
    // Note: you don't need to reference the folder,
    // Getx will search automatically for the home folder
    // and add your controller there.
    
    # 新增controller
    get create controller:dialogcontroller on home
    
    // To create a new view in a specific folder:
    // Note: you don't need to reference the folder,
    // Getx will automatically search for the home folder
    // and insert your view there.
    
    # 在home下新增view
    get create view:dialogview on home
    
    // To create a new provider in a specific folder:
    get create provider:user on home
    
    // To generate a localization file:
    // Note: 'assets/locales' directory with your translation files in json format
    get generate locales assets/locales
    
    // To generate a class model:
    // Note: 'assets/models/user.json' path of your template file in json format
    // Note: on  == folder output file
    // Getx will automatically search for the home folder
    // and insert your class model there.
    get generate model on home with assets/models/user.json
    
    //to generate the model without the provider
    get generate model on home with assets/models/user.json --skipProvider
    
    //Note: the URL must return a json format
    get generate model on home from "https://api.github.com/users/CpdnCristiano"
    
    // To install a package in your project (dependencies):
    # 安装依赖
    get install camera
    
    // To install several packages from your project:
    get install http path camera
    
    // To install a package with specific version:
    get install path:1.6.4
    
    // You can also specify several packages with version numbers
    
    // To install a dev package in your project (dependencies_dev):
    # 安装开发依赖
    get install flutter_launcher_icons --dev
    
    // To remove a package from your project:
    # 删除依赖
    get remove http
    
    // To remove several packages from your project:
    get remove http path
    
    // To update CLI:
    get update
    // or `get upgrade`
    
    // Shows the current CLI version:
    get -v
    // or `get -version`
    
    // For help
    get help
```

## 使用 FlutterAssetsGenerator 管理 assets

+ https://plugins.jetbrains.com/plugin/15427-flutterassetsgenerator

+ Right-click on folder or file, then click
  Flutter: Configuring Paths.

+ Press Option(mac)/Alt(win) + G,It will generate assets.dart on lib/generated.

## 使用 SmartDialog

+ https://pub.dev/packages/flutter_smart_dialog


## 使用 toastification

+ https://pub.dev/packages/toastification

## android sign

ainote
zhou
ainote
ainote
sz
sz
cn
