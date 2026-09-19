# AI 备忘录 · 核心资料、PPT、文档与相关站点汇总

本文档系统汇总了本项目全生命周期中的所有核心输入材料、商业计划书、演示演讲 PPT、设计手绘脑图、风格参考以及全网在线网站与 API 接口文档链接，供查阅与溯源。

---

## 一、 在线网站与原型链接

| 站点名称 | 访问链接 | 说明与定位 |
| :--- | :--- | :--- |
| **产品背后的故事（原作者手写站）** | [https://ainote-behind-the-design.easy-fern-5574.chatgpt.site/](https://ainote-behind-the-design.easy-fern-5574.chatgpt.site/) | 大哥亲自操刀撰写的产品故事站，包含 5 大核心机制动画与心路历程 |
| **UX 设计师旗舰作品集（3D 极奢展厅）** | 本地 `http://localhost:3020/portfolio` / 源码 `交付/AI备忘录官网-Sites/app/portfolio` | 专为 Goodnotes / 顶级 UX 岗位打造的作品集全案，含 4 大链路深度剖析 |
| **AI 自动整理全链路高阶原型** | 本地 `http://localhost:3020/auto-organize` / 源码 `交付/AI备忘录官网-Sites/app/auto-organize` | 暖象牙白 3D 展台 + iPhone 16 Pro 视窗 + 拟真可点击交互与安全开关 |
| **极奢离线单文件原型** | 本地文件 `交付/AI备忘录-自动整理-极奢展厅高阶原型/index.html` | 双击直接浏览器打开，零依赖体验全部交互流 |
| **GitHub 官方公开代码仓库** | [https://github.com/liushuangguang/AI-Memo](https://github.com/liushuangguang/AI-Memo) | 本项目全量源码、产品总则与设计资产官方托管主页 |

---

## 二、 商业计划书与探索 PPT（PDF 版）

保存在仓库 `docs/materials/pdf/` 目录下：

1. **《备忘录产品商业计划书 (BP)》**
   - 路径：[`docs/materials/pdf/备忘录产品商业计划书(BP).pdf`](./pdf/备忘录产品商业计划书(BP).pdf)
   - 核心内容：深入剖析“迎合人性懒到底、直接给可用答案、不增加信息负担”的底层逻辑与商业模式，确立四大核心主链路。
2. **《通用信息组织能力的探索 (PPT)》**
   - 路径：[`docs/materials/pdf/通用信息组织能力的探索(PPT).pdf`](./pdf/通用信息组织能力的探索(PPT).pdf)
   - 核心内容：从个人碎片信息捕获到高阶结构化知识库演进的产品思考，以及如何赋能 Goodnotes 等数字笔记平台。
3. **《投资人评测笔记》**
   - 路径：[`docs/materials/pdf/投资人评测笔记(investor-notes).pdf`](./pdf/投资人评测笔记(investor-notes).pdf)
   - 核心内容：投资人视角下的产品评测维度与机会点分析。

---

## 三、 演示演示稿（PPT 原件与 38 页高清切图）

保存在仓库 `docs/materials/ppt/ai-memo-deck/` 目录下：

1. **PPT 完整源文件**：
   - 路径：[`docs/materials/ppt/ai-memo-deck/source.pptx`](./ppt/ai-memo-deck/source.pptx)（26.5 MB）
2. **38 页高清幻灯片画板**：
   - 路径：[`docs/materials/ppt/ai-memo-deck/source/`](./ppt/ai-memo-deck/source/)（包含 `slide-1.png` 至 `slide-38.png`）
3. **逐页讲稿解说文案**：
   - 路径：[`docs/materials/ppt/ai-memo-deck/text.txt`](./ppt/ai-memo-deck/text.txt)

---

## 四、 设计手稿与风格灵感参考

保存在仓库 `docs/materials/design-references/` 目录下：

1. **UX 设计师岗位作品集手绘思路导图**
   - 路径：[`docs/materials/design-references/UX设计师岗位作品集手绘思路导图.jpg`](./design-references/UX设计师岗位作品集手绘思路导图.jpg)
   - 涵盖背景调研、产品形态边界、3 大链路、旧版反思 vs 新版重设计、设计沟通进度、通用信息组织迁移 Goodnotes、交互细节深挖与未来展望。
2. **极奢设计风格参考图集**
   - 参考图 1（3D 展台、圆角磨砂卡片、Apple Intelligence 流体微光）：[`docs/materials/design-references/设计风格参考-3D展台与呼吸微光.jpg`](./design-references/设计风格参考-3D展台与呼吸微光.jpg)
   - 参考图 2（长虹条纹磨砂玻璃背板、拟真手机视窗）：[`docs/materials/design-references/设计风格参考-长虹玻璃与手机视窗.jpg`](./design-references/设计风格参考-长虹玻璃与手机视窗.jpg)
   - 参考图 3（胶卷相机、复古拟物、细腻微距光泽）：[`docs/materials/design-references/设计风格参考-胶卷相机与拟真材质.jpg`](./design-references/设计风格参考-胶卷相机与拟真材质.jpg)

---

## 五、 后端与 AI 核心 API 接口文档

在开发与系统集成中所使用的关键 API 规范及渠道说明：

1. **V3-API 渠道规范文档**
   - 接口文档链接：[https://api-gpt-ge.apifox.cn/381349186e0](https://api-gpt-ge.apifox.cn/381349186e0)
   - 承载模型：`gemini-2.0-flash-search`（负责联网增强搜索与快速抽取）
2. **Next-API 渠道规范文档**
   - 接口文档链接：[https://docs.nextaicore.com/api-introduce.html](https://docs.nextaicore.com/api-introduce.html)
   - 承载模型：`gpt-4o-mini`（负责多轮逻辑消歧与结构化格式规整）
