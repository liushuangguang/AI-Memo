# AI备忘录 0.1.5 / build 7 安装与验证说明

2026-09-06 14:05 构建。免登录、局域网测试版，非正式发布，未完成 Android 真机验收。

## 安装

[下载本轮 APK](C:/Users/Administrator/Documents/ChatGPT/AI备忘录/交付/AI备忘录-v0.1.5-build7-局域网测试版-20260906.apk)

- 直接覆盖安装旧测试版，不要先卸载或清除应用数据，以免丢失本机 Guest 身份及待处理图片。
- 已核对与 build 5 同包名、同签名；若手机仍提示签名不兼容，请保留提示，不要为安装而先删除原应用。
- 手机与电脑需处在可互访的局域网；接口地址为 `http://192.168.31.213:8080`。电脑休眠、关机或服务停止后，APP 的 AI 请求会失败。
- 设置中开启截图识别后，按引导授予照片、通知和悬浮窗权限。默认每张截图确认；后台自动生成须额外单独同意，且只处理之后的新截图。
- 系统后台限制可能需要在手机的应用电池/后台管理中放行。没有进行各品牌真机测试，不能保证所有厂商都按同一路径授权。

## 安装包身份

| 项目 | 核对结果 |
|---|---|
| 包名 | `aifunc.top.ainote_app.debug` |
| 版本 | `0.1.5` / versionCode `7` |
| 大小 | 215,031,746 字节，约 205.1 MiB |
| SHA-256 | `F0A16F6C18C3AC5EC803065A0B0028992E37811BA081D331DD79BC48BDC4E052` |
| 签名 | APK v2 验证通过，与 build 5 证书一致 |
| 最低 / 目标系统 | Android API 24 / 36 |
| CPU 架构 | arm64-v8a、armeabi-v7a、x86_64 |
| 原生库 | 三种架构均包含 Flutter、super_native_extensions、irondash、中文 OCR 必需库 |
| Android 16 相关检查 | arm64/x86_64 全部原生库 PT_LOAD 段至少 16 KB 对齐；zipalign 16 KB 检查通过 |
| 构建配置 | Flutter 与原生端均为 Guest 模式，API 指向同一局域网后端 |

最终应用构建日志：`L:/ainote-qa-20260906/apk-build7-native-complete.log`，`:app:assembleDebug` 成功。构建使用已安装 Gradle 和本地 Rust 工具，未修改系统代理或共享 Flutter SDK。

打包中发现备用命令缺少 `FLUTTER_ROOT`，可能生成没有完整原生依赖的 APK。现已修正构建环境并重新编译，增加实际 APK 内部库文件和 ELF 对齐检查。缺件候选包保留于 `L:/ainote-qa-20260906/build7-candidate-missing-native-not-for-delivery.apk`，不用于交付；最终交付包是上表哈希。这个发现不能直接认定为此前手机白屏的唯一原因。

## 功能与测试

详细逐环节记录：[四段视频修复对照](C:/Users/Administrator/Documents/ChatGPT/AI备忘录/交付/AI备忘录-v0.1.5-build7-逐视频修复对照.md)。包括截图浮层/授权/原图、信息完善、智能整理、内容辅助、自动整理、合并来源及保存并发修复。

- Flutter 全量 187 项、后端 679 项、Android 原生 24 项测试通过。
- 已用合成笔记真实调用整理、信息完善、链接搜索、配图、相关备忘录、回顾、分类、内容辅助和主题合并。
- 定向 Dart 静态检查没有 error，仍有 18 条 warning/info，未宣称全仓库 lint 全绿。
- UI 组件渲染和自动化不能替代 Android 16 手机点击、后台监听、麦克风和系统语音验收。
- 核对 APK 里存在并正确对齐的原生库，也不等于已在 16 KB 页大小手机上实际运行通过。

## 当前服务与数据

最终服务重新启动后，本机及局域网健康接口均返回 `UP`。运行新 JAR：`L:/ainote-qa-20260906/api-source/note/target/note-0.0.1-SNAPSHOT.jar`；运行目录为原后端目录 `C:/Users/Administrator/Documents/ChatGPT/AI备忘录/ainote-product/repos/api`，沿用 `local-data/note.mv` 和原私有图片。

本轮较早的重启前后 57 条记录逐条比较无差异；本次最终启动日志也确认打开同一个持久数据库文件。没有删除旧数据、旧包、回滚备份，未提交或推送源码。原 C 目录的旧编译 JAR 不能当作本轮新服务使用；源码修复已回填，重新构建可生成新版。

后端密钥只注入 Java 进程，不在 APK 内。电脑重启后不会保留这个进程中的密钥，需要重新通过启动脚本安全输入。这版不适合直接暴露公网：正式上线仍需要 HTTPS、正式身份认证和服务部署。

## 仍待确认

AI 当前仍使用已有 Dify / Coze / DeepSeek，没有切换 TabToTask。建议采用 TabToTask 相同供应商、备忘录独立密钥及额度；若选择直接共用其线上网关，需要允许扩展该产品的任务协议。专用供应商接入配置已准备好，但不能把配置入口等同于已经切换成功。

[逐项验收与反馈](http://127.0.0.1:4176/bug-workbench.html)。按照验收技能，修复记录保留为“待用户验收”，不由自动测试代替点击“通过”。
