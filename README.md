# AI 备忘录 (AI-Memo)

> **“随手记下来，需要时用得上。”**  
> 专为个人碎片信息随手捕获、无感整理与智能检索打造的下一代智能备忘录产品。全仓收录产品权威定义总则、全套高保真 UX 设计师作品集交付物、以及从端到端（Flutter + Spring Boot + Python）的完整真机研发源码。

---

## 🌟 核心资料、PPT、文档与相关站点导航

这里汇总了项目研发与设计过程中原汁原味的全部核心资料、商业计划书、演讲幻灯片与外部链接：

### 1. 外部在线站点与权威原型
- 📖 **原作者手记 · 产品背后的故事**：[https://ainote-behind-the-design.easy-fern-5574.chatgpt.site/](https://ainote-behind-the-design.easy-fern-5574.chatgpt.site/)  
  *深入阐释 5 大核心交互机制（截图确认、私人补问、信息分层、主题来源、失败恢复）与创业思考。*
- 🎨 **UX 设计师旗舰作品集（3D 展厅高阶版）**：本地目录 `交付/AI备忘录官网-Sites/app/portfolio`  
  *以 3D 展台、iPhone 16 Pro 机身、长虹条纹磨砂玻璃与 Apple Intelligence 呼吸微光呈现的 Goodnotes 岗位旗舰作品集。*
- ⚡ **AI 自动整理全链路高阶原型**：本地目录 `交付/AI备忘录官网-Sites/app/auto-organize`  
  *拟真 iPhone 视窗、全部可点击交互状态、四层解耦呈现、采纳弹窗与安全撤销机制。*
- 📱 **极奢展厅离线单文件原型**：[`交付/AI备忘录-自动整理-极奢展厅高阶原型/index.html`](./交付/AI备忘录-自动整理-极奢展厅高阶原型/index.html)  
  *双击直接在任何现代浏览器中打开，零依赖体验全套交互流程。*

### 2. 商业计划书与探索 PPT (PDF)
- 📑 [《备忘录产品商业计划书 (BP).pdf》](./docs/materials/pdf/备忘录产品商业计划书(BP).pdf)：剖析“迎合人性懒到底、直接给可用答案、不增加信息负担”的产品与商业模型。
- 📑 [《通用信息组织能力的探索 (PPT).pdf》](./docs/materials/pdf/通用信息组织能力的探索(PPT).pdf)：碎片捕获向高阶知识库演进的理论体系及 Goodnotes 赋能思考。
- 📑 [《投资人评测笔记 (investor-notes).pdf》](./docs/materials/pdf/投资人评测笔记(investor-notes).pdf)：多维度商业化与竞争力评测要点。

### 3. 完整 PPT 原件与 38 页高清幻灯片
- 📽️ [完整演示文稿 (source.pptx)](./docs/materials/ppt/ai-memo-deck/source.pptx) *(26.5 MB 完整 PPT 源文件)*
- 🖼️ [38 页逐页高清幻灯片切图库](./docs/materials/ppt/ai-memo-deck/source/) *(slide-1.png 至 slide-38.png 全览)*
- 📝 [PPT 逐页讲稿解说文本](./docs/materials/ppt/ai-memo-deck/text.txt)

### 4. 设计手稿与灵感参考
- 📐 [UX 设计师岗位作品集手绘思路导图](./docs/materials/design-references/UX设计师岗位作品集手绘思路导图.jpg)  
  *包含背景调研、产品定义边界、3 大核心链路、新旧对比、向 Goodnotes 能力迁移等全景手绘导图。*
- 💎 [极奢设计风格参考 1：暖象牙白 3D 展台与呼吸微光](./docs/materials/design-references/设计风格参考-3D展台与呼吸微光.jpg)
- 💎 [极奢设计风格参考 2：长虹条纹磨砂玻璃与真机视窗](./docs/materials/design-references/设计风格参考-长虹玻璃与手机视窗.jpg)
- 💎 [极奢设计风格参考 3：复古拟物胶卷相机与精致细节](./docs/materials/design-references/设计风格参考-胶卷相机与拟真材质.jpg)

### 5. 核心 API 与规范文档
- 🔗 **V3-API 渠道规范文档**：[https://api-gpt-ge.apifox.cn/381349186e0](https://api-gpt-ge.apifox.cn/381349186e0) *(模型：`gemini-2.0-flash-search`)*
- 🔗 **Next-API 渠道规范文档**：[https://docs.nextaicore.com/api-introduce.html](https://docs.nextaicore.com/api-introduce.html) *(模型：`gpt-4o-mini`)*

---

## 🛠️ 项目工程结构全景

```
AI-Memo/
├── PRODUCT.md                    # 唯一运行时产品权威总则（12条刚性设计原则与事实证据）
├── README.md                     # 项目官方全景总览
├── changelog Liu.md              # 需求交互与工程演进权威日志
├── docs/                         # 文档、规范与材料库
│   └── materials/                # 全套原始资料库（PDF、PPT、设计手绘图、外部链接汇总）
│       ├── pdf/                  # 商业计划书(BP)、通用信息组织探索PPT(PDF)、投资人笔记
│       ├── ppt/ai-memo-deck/     # PPT源文件(source.pptx)、38页高清切图、讲稿文本
│       ├── design-references/    # UX手绘思路导图、极奢展台设计风格参考图集
│       └── LINKS_AND_REFERENCES.md # 外部在线网站、API文档与原型链接速查
├── 交付/                         # UX作品集与设计高保真交付物
│   ├── AI备忘录官网-Sites/        # Next.js 14 + Tailwind 作品集与交互原型站点源码
│   │   ├── app/portfolio/        # 3D展厅级顶级UX设计师作品集页面
│   │   └── app/auto-organize/    # AI自动整理全链路拟真可交互原型页面
│   ├── AI备忘录-产品背后的故事/   # 原作者手写故事站本地工程源码
│   └── AI备忘录-自动整理-极奢展厅高阶原型/index.html # 零依赖离线单文件原型
└── ainote-product/repos/         # 真机研发全栈工程
    ├── ainote_app/               # Flutter 移动端真机 App 源码（含 Android 原生通道）
    ├── api/                      # Java Spring Boot 后端中台服务源码
    ├── ainote_semantic/          # Python 本地向量化与语义检索微服务
    └── desktop-app/              # 桌面端跨平台客户端工程源码
```

---

## 💡 核心设计与产品哲学

1. **迎合人性懒到底**：绝不在用户速记的冲动时刻弹出一堆表单、标签选择器；随手记下来是绝对的第一优先级。
2. **AI 静默推演，不越俎代庖**：整理发生在后台，原文稳态不动；结构化建议仅在用户主动进入整理视图时前置呈现。
3. **四层解耦信息模型**：
   - 第一层：**速记原文**（稳态不可篡改，给用户绝对安全感）
   - 第二层：**核心待办**（支持直接打勾交互，一目了然）
   - 第三层：**智能建议**（补全细节，支持一键点选追加）
   - 第四层：**上下文召回**（唤醒关联的历史碎片，如数月前的电话、地址、人名）
4. **绝对可逆性**：任何一次采纳、合并、重写，均配备永久存在的 Undo 撤销胶囊，让用户毫无心理压力。

---

## 🚀 本地运行体验

### 1. UX 作品集与高阶交互原型站点
```bash
cd "交付/AI备忘录官网-Sites"
npm install
npm run dev -- --port 3020
```
启动后在浏览器打开：
- 旗舰作品集：`http://localhost:3020/portfolio`
- 自动整理全链路原型：`http://localhost:3020/auto-organize`

### 2. 离线原型
直接双击打开 `交付/AI备忘录-自动整理-极奢展厅高阶原型/index.html` 即可畅爽体验。
