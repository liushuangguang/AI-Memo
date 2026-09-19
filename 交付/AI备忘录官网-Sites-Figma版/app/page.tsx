'use client';

import Image from 'next/image';
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  Check,
  CircleAlert,
  Clock3,
  FileInput,
  Layers3,
  Link2,
  LockKeyhole,
  Mic,
  Network,
  RefreshCw,
  Search,
  ShieldCheck,
  Target,
  X,
  ZoomIn,
} from 'lucide-react';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import './case-study.css';

type EvidenceKind = 'verified' | 'author' | 'inference' | 'prototype';
type WritebackStage = 'select' | 'preview' | 'confirmed';

const chapters = [
  { id: 'overview', label: '定义' },
  { id: 'system', label: '系统' },
  { id: 'choices', label: '选择' },
  { id: 'writeback', label: '写回' },
  { id: 'memory', label: '记忆' },
  { id: 'reliability', label: '可靠性' },
];

const recordChain = [
  {
    index: '01',
    title: '先区分原文与生成内容',
    detail: '标题、分类与规整正文可以帮助阅读，但不能抹掉内容来自用户还是 AI。',
    relation: '只有原文进入主题整合；AI 结果先停在候选层，避免误生成污染长期记忆。',
  },
  {
    index: '02',
    title: '把行动从正文里提出来',
    detail: '待办拥有独立结构，可继续编辑完成状态、日期、提醒与重复规则。',
    relation: '让一条记录从“被保存”走向“可以被完成”。',
  },
  {
    index: '03',
    title: '建议只是一层候选',
    detail: 'AI 建议与正文、待办分层出现；建议可按条保留，正文替换仍是独立动作。',
    relation: '把模型正确率问题拆成可判断的小决定，避免一次性覆盖原文。',
  },
  {
    index: '04',
    title: '外部知识不混进个人事实',
    detail: '互联网参考被放在“猜你想看”，视觉上与原始记录分开。',
    relation: '视觉分层也建立处理边界：外部参考不会被当作个人原文送入主题整合。',
  },
  {
    index: '05',
    title: '旧记录先相关，再决定是否整合',
    detail: '智能整理页先把相关备忘录作为可保存候选；进入整合流程后，用户再逐条勾选来源并主动开始。',
    relation: '把“看起来相关”与“允许写入长期主题”分开。',
  },
  {
    index: '06',
    title: '把结果继续导向下一步',
    detail: '自动整理、语音讨论、内容辅写与 AI 配图出现在结果之后，而不是抢占记录入口。',
    relation: '同一份结构化上下文继续服务表达、思考与行动。',
  },
];

const capabilityReasons = [
  {
    title: '先推荐，不预设固定流水线',
    copy: '记录的长度、类型和意图不同，系统先判断最可能有价值的能力，再给出 5 项推荐。',
    system: '减少无效模型调用，也减少用户面对全部能力时的选择负担。',
  },
  {
    title: '解释为什么推荐',
    copy: '“推荐理由”把模型的介入依据放到界面里，让用户知道某项能力为何出现。',
    system: '推荐从黑箱结论变成可以检查、可以反对的提议。',
  },
  {
    title: '推荐与自选同时存在',
    copy: '用户可以增减能力；示例中系统推荐 5 项，最终由用户选择 8 项执行。',
    system: '模型负责缩小范围，用户负责决定本次任务边界。',
  },
  {
    title: '自动整理需要单独授权',
    copy: '一次执行与“以后新增或修改后自动整理”是两个决定，开关不会被推荐结果顺带开启。',
    system: '把单次许可与持续许可分开，避免主动能力悄悄扩大。',
  },
];

const journeyStages = [
  {
    index: '01',
    label: '随手记录',
    title: '“周末搬家，搬家公司，买拖把地毯，换窝。”',
    detail: '先接受不完整、无分类的自然语言，不要求用户在灵感出现时先整理。',
    rule: '保留原始输入',
  },
  {
    index: '02',
    label: '识别缺口',
    title: '“搬家”指什么场景？',
    detail: '只补问会改变标题、待办或后续检索的关键信息，无法确认的内容保持为空。',
    rule: 'R5 · 不擅自补全',
  },
  {
    index: '03',
    label: '选择能力',
    title: '推荐 5 项，最终选择 8 项',
    detail: 'AI 缩小范围并解释理由，用户决定本次任务；持续自动整理另行授权。',
    rule: 'R2 · 授权分层',
  },
  {
    index: '04',
    label: '形成结果',
    title: '事实、行动、建议与外部信息分层',
    detail: '不同来源不混成一段“看起来都正确”的答案，每一层拥有不同处理权限。',
    rule: 'R1 · 来源分层',
  },
  {
    index: '05',
    label: '选择写回',
    title: '生成不等于采纳',
    detail: '结果先停在候选层，按任务分块预览；用户确认后才改变正文。',
    rule: 'R3 · 显式写回',
  },
  {
    index: '06',
    label: '进入行动',
    title: '待办从正文中成为独立对象',
    detail: '自然语言里的行动被提取后，才能继续承接日期、提醒、重复等任务信息。',
    rule: '结构服务行动',
  },
  {
    index: '07',
    label: '再次利用',
    title: '相关记录、主题与搜索重新找到它',
    detail: '系统发现关系，用户选择进入长期主题的来源，并留下合并时间与出处。',
    rule: 'R4 · 关系可追溯',
  },
];

const contractLayers = [
  {
    kind: 'source',
    label: '来源证据',
    title: '用户原始内容',
    items: '原始文字 · 语音对话 · 被识别图片',
    permission: '保留，不被静默覆盖',
  },
  {
    kind: 'structure',
    label: '结构化解释',
    title: '可工作的记录',
    items: '标题 · 分类 · 正文 · 待办',
    permission: '可编辑，仍需核对推断',
  },
  {
    kind: 'candidate',
    label: 'AI 候选',
    title: '尚未成为事实',
    items: '建议 · 辅写 · 总结 · 相关记录',
    permission: '只展示，不自动写回',
  },
  {
    kind: 'accepted',
    label: '用户确认',
    title: '获得写入资格',
    items: '单块存入 · 全部存入 · 正文替换',
    permission: '显式动作后改变记录',
  },
  {
    kind: 'memory',
    label: '长期关系',
    title: '可追溯的语义视图',
    items: '主题 · 关系 · 搜索召回',
    permission: '选来源并保留时间、出处',
  },
];

const systemRules = [
  {
    id: 'R1',
    title: '来源分层',
    decision: '用户原文、AI 生成、外部信息与来源图片必须能被区分。',
    reaches: '智能整理 · 辅写 · 语音 · 截图 · 主题',
  },
  {
    id: 'R2',
    title: '授权分层',
    decision: '本次执行与以后持续运行是两个决定，不能由一个按钮顺带授权。',
    reaches: '能力推荐 · 自动整理 · 后台运行',
  },
  {
    id: 'R3',
    title: '写回隔离',
    decision: '生成先成为候选；AI 写回正文后，不得再次触发自动整理。',
    reaches: '辅写 · 语音 · 截图 · 自动整理',
  },
  {
    id: 'R4',
    title: '关系可追溯',
    decision: '语义相关不等于事实，进入长期主题前必须选择来源并留下出处。',
    reaches: '相关备忘录 · 主题 · AI 搜索',
  },
  {
    id: 'R5',
    title: '不确定性外显',
    decision: '会改变后续结果的语义缺口要补问；无法理解时保持原文，不勉强生成。',
    reaches: '信息补充 · 待办 · 辅写 · 关系检索',
  },
];

const runtimeStates = [
  {
    state: '保存成功',
    visible: '先确认用户内容已经保存，再开始长任务。',
    control: '保存状态不与 AI 成功绑定。',
  },
  {
    state: '智能整理中',
    visible: '显示任务名称、实时保存时间与当前 3000 字处理上限。',
    control: '退出界面后后台继续，避免把用户困在等待页。',
  },
  {
    state: '整理完成',
    visible: '保留完成时间，并提供重新生成 / 二次整理入口。',
    control: '新的生成是显式动作，不是静默覆盖。',
  },
  {
    state: '模块网络失败',
    visible: '截图识别等对应模块提示网络原因，并提供重试入口。',
    control: '失败反馈不抹去已经保存的原文。',
  },
];

const evolution = [
  {
    stage: 'MVP',
    hypothesis: '保存后立刻出现有用信息，能否让用户感知 AI 价值？',
    observation: '先把范围收紧到“低成本输入 → 可处理信息”的最短链路。',
    decision: 'AI 价值应发生在保存之后，不能反过来提高记录门槛。',
    change: '优先建立原始记录与整理结果之间的基础链路。',
  },
  {
    stage: 'V1',
    hypothesis: '扩大能力面，能否覆盖更多潜在需求？',
    observation: '能力增加也同时带来等待、调用成本和界面复杂度。',
    decision: '问题不再只是“有没有能力”，而是“用户是否理解 AI 为什么介入”。',
    change: '从固定能力堆叠转向按记录意图编排能力。',
  },
  {
    stage: 'V2',
    hypothesis: '用户能否理解 AI 为何介入，并在错误时恢复控制？',
    observation: '主动能力如果没有来源、状态和写回边界，会把模型风险留给用户承担。',
    decision: '版本目标从“增加 AI 能力”转为“管理 AI 行为”。',
    change: '增加推荐理由、作用域、状态、分块采纳、持续授权和防递归规则。',
  },
];

const proposalItems = [
  {
    id: 'ambiguity',
    title: '保留语义缺口',
    detail: '“搬家”具体指租房、购房、同城还是跨城，仍需用户确认',
    output: '语义待确认：搬家场景。',
  },
  {
    id: 'tasks',
    title: '形成待办候选',
    detail: '联系搬家公司、购买拖把和地毯',
    output: '待办候选：联系搬家公司；购买拖把和地毯。',
  },
  {
    id: 'reminder',
    title: '保留时间缺口',
    detail: '“周末”不够精确，不自动编造时间',
    output: '提醒时间：待确认。',
  },
];

function EvidenceTag({ kind, children }: { kind: EvidenceKind; children?: ReactNode }) {
  const labels: Record<EvidenceKind, string> = {
    verified: '设计稿可核对',
    author: '项目材料记录',
    inference: '设计推演',
    prototype: '历史原型',
  };

  return (
    <span className="evidence-tag" data-kind={kind}>
      {labels[kind]}
      {children ? <span>{children}</span> : null}
    </span>
  );
}

function ProductShot({
  src,
  alt,
  width = 390,
  height = 844,
  caption,
  className = '',
  priority = false,
}: {
  src: string;
  alt: string;
  width?: number;
  height?: number;
  caption: ReactNode;
  className?: string;
  priority?: boolean;
}) {
  return (
    <figure className={`product-shot ${className}`.trim()}>
      <a className="product-shot__viewport" href={src} target="_blank" rel="noreferrer" aria-label={`${alt}，打开原图`}>
        <Image
          src={src}
          alt={alt}
          width={width}
          height={height}
          sizes="(max-width: 760px) 86vw, (max-width: 1180px) 42vw, 390px"
          priority={priority}
        />
        <span className="product-shot__zoom" aria-hidden="true"><ZoomIn size={15} /> 查看原图</span>
      </a>
      <figcaption>{caption}</figcaption>
    </figure>
  );
}

function WritebackLab() {
  const [selected, setSelected] = useState<string[]>(['tasks']);
  const [stage, setStage] = useState<WritebackStage>('select');
  const previewLines = useMemo(() => {
    const lines = ['原文：周末搬家，搬家公司，买拖把地毯，换窝。'];
    proposalItems.forEach((item) => {
      if (selected.includes(item.id)) lines.push(item.output);
    });
    return lines;
  }, [selected]);

  const toggle = (id: string) => {
    setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
    setStage('select');
  };

  const advance = () => {
    if (stage === 'select') setStage('preview');
    if (stage === 'preview') setStage('confirmed');
    if (stage === 'confirmed') setStage('select');
  };

  return (
    <div className="writeback-lab" aria-label="生成、选择与写回的交互说明">
      <div className="writeback-lab__header">
        <div>
          <span>可操作的设计说明</span>
          <strong>试着取消某一项，观察写回预览如何变化。</strong>
        </div>
        <EvidenceTag kind="inference">案例页复原控制原则</EvidenceTag>
      </div>
      <div className="writeback-lab__body">
        <section aria-labelledby="proposal-title">
          <div className="lab-title"><span id="proposal-title">01 · AI 提议</span><small>{selected.length} 项待预览</small></div>
          <div className="proposal-list">
            {proposalItems.map((item) => {
              const isSelected = selected.includes(item.id);
              return (
                <button
                  key={item.id}
                  type="button"
                  aria-pressed={isSelected}
                  className={isSelected ? 'is-selected' : ''}
                  onClick={() => toggle(item.id)}
                >
                  <span aria-hidden="true">{isSelected ? <Check size={15} /> : <X size={15} />}</span>
                  <span><strong>{item.title}</strong><small>{item.detail}</small></span>
                  <em>{isSelected ? '采用' : '跳过'}</em>
                </button>
              );
            })}
          </div>
        </section>
        <section className="writeback-preview" aria-labelledby="preview-title">
          <div className="lab-title"><span id="preview-title">02 · 写回预览</span><small>{stage === 'confirmed' ? '本次演示已确认' : stage === 'preview' ? '等待确认' : '尚未生成'}</small></div>
          <div className="writeback-preview__copy" aria-live="polite">
            {stage === 'select'
              ? <p className="empty-copy">先选择值得保留的内容。场景与时间未确认时，系统不会凭空补齐。</p>
              : previewLines.map((line, index) => <p key={line} className={index === 0 ? 'source-line' : ''}>{line}</p>)}
          </div>
          <button type="button" className="writeback-action" onClick={advance} disabled={selected.length === 0}>
            {stage === 'select' ? '查看写回预览' : stage === 'preview' ? '确认本次写回' : '返回重新选择'}
            {stage === 'confirmed' ? <RefreshCw size={16} /> : <ArrowRight size={16} />}
          </button>
        </section>
      </div>
      <p className="writeback-lab__note">这里演示“生成 ≠ 采纳 ≠ 写回”的权限分层，不把案例网页中的交互冒充为现网功能。</p>
    </div>
  );
}

export default function CaseStudyPage() {
  const [activeSection, setActiveSection] = useState('overview');

  useEffect(() => {
    const observer = new IntersectionObserver((entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
      if (visible) setActiveSection(visible.target.id);
    }, { rootMargin: '-18% 0px -68% 0px', threshold: [0.02, 0.16, 0.42] });

    chapters.forEach(({ id }) => {
      const section = document.getElementById(id);
      if (section) observer.observe(section);
    });
    return () => observer.disconnect();
  }, []);

  return (
    <main className="case-site">
      <a className="skip-link" href="#case-content">跳到案例正文</a>

      <header className="case-header">
        <div className="reading-progress" aria-hidden="true"><span /></div>
        <a className="case-brand" href="#top" aria-label="返回案例开头">
          <Image src="/logo.webp" alt="" width={34} height={34} priority />
          <span>AI备忘录</span>
          <small>AI 产品设计案例</small>
        </a>
        <nav aria-label="案例章节">
          {chapters.map((chapter) => (
            <a key={chapter.id} href={`#${chapter.id}`} aria-current={activeSection === chapter.id ? 'location' : undefined}>
              {chapter.label}
            </a>
          ))}
        </nav>
        <a className="author-chip" href="#about">刘双广 · 产品 / UX</a>
      </header>

      <section className="case-hero" id="top" aria-labelledby="case-title">
        <div className="hero-meta"><span>AI 原生产品 · 0→1</span><span>2024.10 至 2025.02</span></div>
        <div className="hero-grid">
          <div className="case-intro">
            <h1 id="case-title">我没有给备忘录加一个 AI，而是重新设计信息从记录到行动的路径。</h1>
            <p className="hero-lede">从一句含糊的随手记，到结构化正文、待办、相关记忆和下一步行动；我用四个问题审查每次 AI 介入：为什么现在出现、依据是什么、用户如何修改、结果何时才有权写回。</p>
            <div className="hero-thesis">
              <span>核心设计命题</span>
              <p>降低输入成本，不把整理成本、模型不确定性和错误后果重新转嫁给用户。</p>
            </div>
            <a className="hero-jump" href="#system">沿一条记录进入系统 <ArrowDown size={17} /></a>
          </div>

          <div className="hero-evidence" aria-label="智能整理结果设计稿">
            <ProductShot
              className="hero-product"
              src="/hero/ai-memo-hero.png"
              alt="AI备忘录产品视觉图，两台手机展示记录与智能整理结果"
              width={1280}
              height={960}
              priority
              caption={<><EvidenceTag kind="author">用户提供首屏图</EvidenceTag> 智能整理把记录、行动与后续能力放进同一工作现场</>}
            />
            <div className="hero-callout callout-structure"><span>01 · 分层</span><p>原文、待办、建议与外部信息不混写。</p></div>
            <div className="hero-callout callout-control"><span>02 · 控制</span><p>关键采纳、替换与写回都是显式动作。</p></div>
            <div className="hero-callout callout-loop"><span>03 · 延续</span><p>同一记录结构继续供搜索、主题、辅写与讨论使用。</p></div>
          </div>
        </div>

        <div className="role-ledger" aria-label="项目角色与证据边界">
          <div><span>我的职责</span><p>主导产品定位、全链路 UX/UI、Agent 任务拆分与版本推进。</p></div>
          <div><span>协作范围</span><p>协同前端、后端与 AI 成员推进研发；不将其表述为一人完成全部工程。</p></div>
          <div><span>系统规模</span><p>项目材料记录 30+ Agent、7+ 工作流；页面只陈述可核对的设计关系。</p><EvidenceTag kind="author" /></div>
        </div>
      </section>

      <div className="evidence-key" aria-label="证据标签说明">
        <strong>证据边界</strong>
        <span><EvidenceTag kind="verified" /> 可由当前 Figma 或留存设计稿核对</span>
        <span><EvidenceTag kind="author" /> 来自履历与项目陈述</span>
        <span><EvidenceTag kind="prototype" /> 来自画板状态或工作流注释，未必代表完整链路</span>
        <span><EvidenceTag kind="inference" /> 从既有设计延伸的方案，不冒充已上线</span>
      </div>

      <div id="case-content">
        <section className="case-section overview-section" id="overview" aria-labelledby="overview-title">
          <header className="section-heading">
            <div className="section-no">01 / 问题重构</div>
            <div>
              <h2 id="overview-title">记录动作只用了几秒，真正的成本却发生在保存以后。</h2>
              <p>多数记录体验把终点放在保存；我把设计对象继续延伸到保存后的理解、转化、找回和复用。</p>
            </div>
          </header>

          <div className="cost-ledger">
            <div className="cost-ledger__anchor">
              <Target size={27} />
              <p>设计目标不是让用户“写得更完整”，而是允许他先用自然语言留下不完整信息，再由系统逐步降低后续劳动。</p>
              <EvidenceTag kind="inference">从产品流程抽象</EvidenceTag>
            </div>
            <ol>
              <li><span>输入时</span><strong>不要求先分类、命名、补全</strong><p>减少记录被打断的概率。</p></li>
              <li><span>保存后</span><strong>识别歧义、行动与关系</strong><p>把隐藏在自然语言里的处理工作显性化。</p></li>
              <li><span>使用时</span><strong>只让用户确认关键判断</strong><p>AI 承担整理，人保留事实与授权。</p></li>
              <li><span>未来</span><strong>让旧信息回到当前任务</strong><p>衡量记录是否被再次利用，而非停留时长。</p></li>
            </ol>
          </div>

          <div className="scope-contrast" aria-label="产品取舍">
            <div><span>选择</span><strong>低成本捕获 → 渐进理解 → 可选择写回 → 关系再利用</strong></div>
            <div><span>放弃</span><strong>重型知识库、固定 AI 流水线、未确认的自动执行、为了显得智能而主动</strong></div>
          </div>

          <div className="journey-case" aria-labelledby="journey-title">
            <div className="journey-case__intro">
              <div>
                <span>一条记录贯穿全篇</span>
                <h3 id="journey-title">先看“周末搬家”如何从一句含糊记录，走到行动与长期记忆。</h3>
              </div>
              <p>后面的能力选择、写回、主题与搜索并不是四组功能，它们分别接管这条信息在不同阶段的风险。</p>
            </div>
            <ol className="journey-case__track">
              {journeyStages.map((stage) => (
                <li key={stage.index}>
                  <span className="journey-case__index">{stage.index}</span>
                  <div>
                    <span>{stage.label}</span>
                    <strong>{stage.title}</strong>
                    <p>{stage.detail}</p>
                    <small>{stage.rule}</small>
                  </div>
                </li>
              ))}
            </ol>
            <div className="journey-case__boundary">
              <EvidenceTag kind="verified">各环节均可在对应 Figma 画板核对</EvidenceTag>
              <p>这条串联顺序是案例叙事，用来说明模块关系，不把分散画板冒充为已验证的完整可点击原型。</p>
            </div>
          </div>
        </section>

        <section className="case-section system-section" id="system" aria-labelledby="system-title">
          <header className="section-heading section-heading--wide">
            <div className="section-no">02 / 系统视角</div>
            <div>
              <h2 id="system-title">先把产品看成一条信息管线，再决定每个页面应该出现什么。</h2>
              <p>截图、文字与语音只是入口。真正的产品关系是：统一成可理解的信息结构，再分别进入整理、行动、关系发现与持续沉淀。</p>
            </div>
          </header>

          <figure className="evidence-board">
            <div className="evidence-board__toolbar">
              <div><Network size={19} /><span>源文件中的产品系统与工作流</span></div>
              <a href="/evidence/system-workflow.png" target="_blank" rel="noreferrer"><ZoomIn size={15} /> 打开原图</a>
            </div>
            <div className="evidence-board__pan" tabIndex={0} aria-label="可横向滚动查看系统工作流图">
              <Image src="/evidence/system-workflow.png" alt="AI备忘录源文件中的完整产品工作流，从原始内容到信息补充、正文整理、整理报告、决策建议与模拟思考" width={2144} height={1657} sizes="(max-width: 900px) 1500px, 100vw" />
            </div>
            <figcaption><EvidenceTag kind="verified" /> 横向查看源文件中的系统设计图；它呈现记录处理、对话知识库、多备忘录整合与待办结构的设计关系。</figcaption>
          </figure>

          <div className="system-reading">
            <article><span>入口层</span><h3>降低捕获成本</h3><p>文本、语音、图片与微信聊天截图进入同一产品，不要求用户先把信息整理成标准格式。</p></article>
            <ArrowRight aria-hidden="true" />
            <article><span>理解层</span><h3>统一成信息对象</h3><p>我把标题、分类、正文、待办、原图与关系视为同一记录的不同层，减少后续能力各自重新询问和重复整理。</p></article>
            <ArrowRight aria-hidden="true" />
            <article><span>使用层</span><h3>分流到行动与记忆</h3><p>整理、提醒、搜索、主题、辅写和讨论读取同一记录，但按风险采用不同确认规则：搜索只呈现，辅写需采纳，主题整合还需选择来源。</p></article>
          </div>

          <div className="information-contract" aria-labelledby="contract-title">
            <div className="information-contract__intro">
              <div>
                <span>统一信息对象与权限模型</span>
                <h3 id="contract-title">模块可以共享上下文，但不能共享同一种写入权限。</h3>
              </div>
              <div>
                <p>同一条记录要同时服务整理、行动、表达与记忆，因此我先区分信息处于哪个阶段，再决定 AI 可以读什么、用户必须确认什么。</p>
                <EvidenceTag kind="inference">依据跨画板界面关系抽象，不代表现有数据库结构</EvidenceTag>
              </div>
            </div>
            <ol className="information-contract__flow">
              {contractLayers.map((layer, index) => (
                <li key={layer.label} data-kind={layer.kind}>
                  <span>{String(index + 1).padStart(2, '0')} · {layer.label}</span>
                  <strong>{layer.title}</strong>
                  <p>{layer.items}</p>
                  <small>{layer.permission}</small>
                </li>
              ))}
            </ol>
            <p className="information-contract__result"><strong>设计结果：</strong>语音、截图、辅写和主题无需各造一套对象；它们复用同一记录结构，只在读取、候选、写回和长期沉淀的权限上发生变化。</p>
          </div>

          <div className="risk-response" aria-label="AI能力、体验风险与设计机制的对应关系">
            <div className="risk-response__head">
              <span>关键设计推导</span>
              <h3>每增加一种认知能力，也会增加一种需要被设计的风险。</h3>
              <p>我没有先罗列功能，而是先判断 AI 在任务中多做了哪一步、这一步可能怎样伤害用户，再决定交互机制。</p>
              <EvidenceTag kind="inference">由设计稿中的跨模块规则归纳</EvidenceTag>
            </div>
            <div className="risk-response__rows">
              <article>
                <span>理解意图</span>
                <p><small>新增风险</small>含糊表达被合理但错误地补全</p>
                <p><small>设计回应</small>只补问会改变后续结果的歧义；“忽略”只跳过补问</p>
                <strong>继续约束标题、待办与关系检索</strong>
              </article>
              <article>
                <span>自动运行</span>
                <p><small>新增风险</small>等待、重复调用与 AI 写回后的递归触发</p>
                <p><small>设计回应</small>保存与运行分开反馈；持续授权独立；AI 写回不再触发整理</p>
                <strong>继续约束后台运行与失败恢复</strong>
              </article>
              <article>
                <span>生成内容</span>
                <p><small>新增风险</small>建议看起来像事实，或一次性覆盖用户原文</p>
                <p><small>设计回应</small>原文与生成内容分层；按块采纳；写回前再次预览</p>
                <strong>继续约束辅写、语音与截图结果</strong>
              </article>
              <article>
                <span>形成记忆</span>
                <p><small>新增风险</small>语义相似被当作事实，生成内容污染长期主题</p>
                <p><small>设计回应</small>用户选择来源；只合并原文；留下合并时间与出处</p>
                <strong>继续约束主题更新与 AI 搜索</strong>
              </article>
            </div>
          </div>

          <div className="rule-map" aria-labelledby="rule-map-title">
            <div className="rule-map__intro">
              <span>规则如何传播</span>
              <h3 id="rule-map-title">我没有逐页修补问题，而是让五条行为契约同时约束多个模块。</h3>
              <p>这张关系表是全篇的阅读索引。后面的关键决策会反复回到这些规则，证明模块之间不是偶然拼接。</p>
            </div>
            <ol>
              {systemRules.map((rule) => (
                <li key={rule.id}>
                  <span>{rule.id}</span>
                  <div><strong>{rule.title}</strong><p>{rule.decision}</p></div>
                  <small><Link2 size={14} /> {rule.reaches}</small>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="case-section choices-section" id="choices" aria-labelledby="choices-title">
          <header className="section-heading">
            <div className="section-no">03 / 能力编排</div>
            <div>
              <h2 id="choices-title">不是每条记录都跑同一套 AI：先推荐能力，再让用户决定本次任务边界。</h2>
              <p>“系统替用户选择”与“把全部选择丢给用户”都不够好。这里用推荐、自选和持续授权三层机制分担决策；用户不必理解全部 AI 能力，也不必为一条简单记录等待整套流程。</p>
            </div>
          </header>

          <div className="decision-layout">
            <ProductShot
              className="capability-shot"
              src="/evidence/capability-selector.png"
              alt="AI整理能力选择页，包含推荐五项、推荐理由、自选整理功能、自动整理开关与开始整理八项"
              width={390}
              height={1572}
              caption={<><EvidenceTag kind="verified" /> 推荐 5 项 → 用户最终选择 8 项；自动整理单独授权</>}
            />
            <div className="reasoning-stack">
              {capabilityReasons.map((item, index) => (
                <article key={item.title}>
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <div><h3>{item.title}</h3><p>{item.copy}</p><small><strong>影响下游：</strong>{item.system}</small></div>
                </article>
              ))}
              <div className="decision-equation">
                <span>任务分工</span>
                <p><strong>AI</strong> 判断哪些能力可能有价值</p>
                <p><strong>用户</strong> 决定本次做什么</p>
                <p><strong>开关</strong> 决定以后是否持续运行</p>
              </div>
            </div>
          </div>

          <div className="decision-tradeoff" aria-labelledby="capability-tradeoff-title">
            <div className="decision-tradeoff__intro">
              <span>关键取舍 01</span>
              <h3 id="capability-tradeoff-title">为什么不是“全部自动跑”，也不是把所有能力一次性丢给用户？</h3>
              <p>记录的意图、风险和成本并不相同。能力越多，默认全跑越浪费；完全自选又要求用户先理解整个 AI 能力库。</p>
            </div>
            <div className="decision-tradeoff__options">
              <article data-status="rejected"><span>方案 A</span><strong>固定 AI 流水线</strong><p>每条记录执行同一组能力。</p><small>放弃：等待和调用随能力数增长，简单记录也被过度处理。</small></article>
              <article data-status="rejected"><span>方案 B</span><strong>全部由用户选择</strong><p>把能力清单完整平铺。</p><small>放弃：选择成本被转嫁给用户，AI 没有承担判断工作。</small></article>
              <article data-status="chosen"><span>最终机制</span><strong>推荐后自选，持续授权另算</strong><p>AI 推荐并解释，用户调整本次任务。</p><small>保留：降低选择负担，同时守住单次与长期授权边界。</small></article>
            </div>
            <div className="decision-ownership">
              <div><span>我的判断</span><p>把“AI 能做什么”改成“这条记录现在值得做什么”。</p></div>
              <div><span>设计产出</span><p>推荐理由、能力分组、本次执行数量与自动整理独立开关。</p></div>
              <div><span>影响下游</span><p>决定模型调用范围、等待状态，也定义后台自动运行需要哪一级授权。</p></div>
            </div>
          </div>
        </section>

        <section className="case-section chain-section" aria-labelledby="chain-title">
          <header className="section-heading section-heading--wide">
            <div className="section-no">04 / 信息链</div>
            <div>
              <h2 id="chain-title">智能整理不是交付“一份答案”，而是把一条记录接入六个可以继续工作的层。</h2>
              <p>我用同一页面上的模块顺序表达信息可信度与行动优先级：先保留事实，再形成行动；外部内容与历史关系随后出现。</p>
            </div>
          </header>

          <div className="record-chain">
            <div className="record-chain__visual">
              <ProductShot
                src="/product/organize.png"
                alt="智能整理结果长页，依次包含标题分类、待办、AI建议、猜你想看、相关备忘录与后续工具"
                width={390}
                height={1536}
                caption={<><EvidenceTag kind="verified" /> 我用信息架构顺序建立可信度层级</>}
              />
            </div>
            <ol className="record-chain__steps">
              {recordChain.map((step) => (
                <li key={step.index}>
                  <span>{step.index}</span>
                  <div><h3>{step.title}</h3><p>{step.detail}</p><small><Link2 size={14} /> {step.relation}</small></div>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="case-section writeback-section" id="writeback" aria-labelledby="writeback-title">
          <header className="section-heading">
            <div className="section-no">05 / 触发与写回</div>
            <div>
              <h2 id="writeback-title">把“补问、运行、生成、采纳、写回”拆成不同事件，才能阻止 AI 自己触发自己。</h2>
              <p>这是 V2 最重要的系统级交互约束：保存后是否自动整理，由独立开关决定；AI 生成内容写回正文则不能再次成为触发源。</p>
            </div>
          </header>

          <div className="trigger-story">
            <div className="trigger-story__shots">
              <ProductShot
                src="/product/clarify.png"
                alt="AI信息补充界面，询问搬家的具体含义并提供四个候选"
                caption={<><EvidenceTag kind="verified" /> 只补问会改变后续结果的歧义</>}
              />
              <ProductShot
                src="/evidence/auto-organizing.png"
                alt="保存成功后的智能整理中状态，显示实时保存时间和当前处理三千字限制"
                caption={<><EvidenceTag kind="verified" /> 保存成功与 AI 运行状态分开反馈</>}
              />
            </div>

            <div className="trigger-rules">
              <article><span>01</span><div><h3>补问只处理语义缺口</h3><p>“忽略”代表跳过这次信息补充，不等于取消自动整理。是否继续运行由自动整理开关独立决定。</p></div></article>
              <article><span>02</span><div><h3>运行中允许离开</h3><p>源文件规定退出界面后自动保存并在后台继续。设计没有声称支持取消正在运行的整理。</p></div></article>
              <article><span>03</span><div><h3>AI 写回不成为新触发源</h3><p>所有“存入正文”操作都不触发自动整理；需要再次处理时，由用户手动一键整理。</p></div></article>
            </div>
          </div>

          <div className="event-boundary" aria-label="自动整理触发边界">
            <div className="event-boundary__title"><ShieldCheck size={25} /><div><span>V2 核心约束</span><h3>同样是正文变化，必须区分是谁造成的。</h3></div><EvidenceTag kind="inference">行为规则可核对，事件模型为案例抽象</EvidenceTag></div>
            <div className="event-boundary__flow">
              <div className="event-source is-user"><span>用户事件</span><strong>用户造成的正文变更</strong><p>根据自动整理开关，允许触发一次</p></div>
              <ArrowRight aria-hidden="true" />
              <div className="event-check"><span>触发来源校验</span><strong>确认变更来自用户</strong><p>自动整理仅执行一次</p></div>
              <ArrowRight aria-hidden="true" />
              <div className="event-result"><span>生成结果</span><strong>建议 / 待办 / 辅写 / 总结</strong><p>等待用户选择</p></div>
              <ArrowRight aria-hidden="true" />
              <div className="event-source is-ai"><span>AI 写回事件</span><strong>存入正文</strong><p>禁止再次触发整理</p></div>
            </div>
            <p className="event-boundary__why"><strong>为什么：</strong>如果 AI 写回再次触发 AI，会产生递归生成、重复消耗、内容漂移，并切断“哪次结果基于哪版原文”的因果关系。</p>
          </div>

          <WritebackLab />

          <div className="decision-tradeoff decision-tradeoff--dark" aria-labelledby="writeback-tradeoff-title">
            <div className="decision-tradeoff__intro">
              <span>关键取舍 02</span>
              <h3 id="writeback-tradeoff-title">为什么结果必须先停在候选层，再由用户写回？</h3>
              <p>流畅的生成很容易被误认成事实。写回机制要同时保护原意、降低核对成本，并切断 AI 写回再次触发 AI 的循环。</p>
            </div>
            <div className="decision-tradeoff__options">
              <article data-status="rejected"><span>方案 A</span><strong>生成后直接覆盖正文</strong><p>路径最短，但错误立即获得事实身份。</p><small>放弃：用户难以比较原文，也无法只拒绝其中一部分。</small></article>
              <article data-status="rejected"><span>方案 B</span><strong>每次生成一份完整副本</strong><p>保住原文，但仍是“全收或全拒”。</p><small>放弃：多个任务混成一份答案，局部有用也难以低成本采纳。</small></article>
              <article data-status="chosen"><span>最终机制</span><strong>原文并置 + 分块预览 + 显式写回</strong><p>生成、采纳、写回成为三个不同事件。</p><small>保留：错误被拆成小决定；写回后不再次触发自动整理。</small></article>
            </div>
            <div className="decision-ownership">
              <div><span>我的判断</span><p>模型输出首先是提议，不因表达流畅就自动成为用户事实。</p></div>
              <div><span>设计产出</span><p>全文/局部作用域、正文/AI 双视图、分块存入、失败时不改写。</p></div>
              <div><span>影响下游</span><p>同一写回契约继续约束语音、截图、辅写与自动整理触发。</p></div>
            </div>
          </div>

          <div className="writing-scope">
            <ProductShot
              className="writing-scope__shot"
              src="/evidence/writing-scope.png"
              alt="AI内容辅写结果，展示选中内容辅写、三项任务、待办、辅写文案和分块存入正文"
              width={390}
              height={977}
              caption={<><EvidenceTag kind="verified" /> 选中内容辅写 · 分块预览与写回</>}
            />
            <div className="writing-scope__copy">
              <span>同一条写回原则，继续约束 AI 辅写</span>
              <h3>先限定作用范围与任务，再允许模型生成。</h3>
              <p>AI 辅写不是一次改写，而是一条逐步收紧权限的链路：先确认处理全文还是局部，再选择本次任务；生成结果与原文并置，并按任务分块等待采纳。</p>
              <dl>
                <div><dt>作用域</dt><dd>全文，或用户选中的局部内容</dd></div>
                <div><dt>任务</dt><dd>按修改、评估、转化分组选择；结果画板示例包含两项预设任务与一项自定义需求</dd></div>
                <div><dt>结果</dt><dd>正文与 AI 生成双视图，按所选任务分块</dd></div>
                <div><dt>权限</dt><dd>单块存入 / 全部存入；写回后不触发自动整理</dd></div>
              </dl>
              <p className="writing-scope__failure"><CircleAlert size={16} /> 无法理解时明确“不做任何改写”并保持原文。这里的失败策略，是阻止一份勉强答案获得写回资格。</p>
            </div>
          </div>
        </section>

        <section className="case-section memory-section" id="memory" aria-labelledby="memory-title">
          <header className="section-heading section-heading--wide">
            <div className="section-no">06 / 关系与记忆</div>
            <div>
              <h2 id="memory-title">主题不是文件夹，相关性也不是事实：它们都是需要来源、边界和确认的语义视图。</h2>
              <p>系统负责发现关系，用户决定哪些来源进入主题；源文件进一步定义了新记录按同一语义边界持续匹配的后续规则。</p>
            </div>
          </header>

          <div className="decision-tradeoff decision-tradeoff--memory" aria-labelledby="memory-tradeoff-title">
            <div className="decision-tradeoff__intro">
              <span>关键取舍 03</span>
              <h3 id="memory-tradeoff-title">为什么主题需要“名称 + 描述 + 来源确认”，而不只是标签或自动聚类？</h3>
              <p>标签只能说明“像什么”，无法说明用户想持续关注哪一段语义；纯自动聚类又会把相似误当作应该永久合并。</p>
            </div>
            <div className="decision-tradeoff__options">
              <article data-status="rejected"><span>方案 A</span><strong>文件夹 / 标签</strong><p>确定、熟悉，但依赖用户持续维护。</p><small>放弃：输入成本回来了，标签也无法表达细致的关注边界。</small></article>
              <article data-status="rejected"><span>方案 B</span><strong>全自动语义聚类</strong><p>系统直接把相似记录归在一起。</p><small>放弃：相似不等于事实相关，错误关系会污染长期记忆。</small></article>
              <article data-status="chosen"><span>最终机制</span><strong>描述定义边界，候选来源由用户确认</strong><p>AI 负责检索与总结，合并留下时间和出处。</p><small>保留：降低维护成本，也让主题内容可核对、可追溯。</small></article>
            </div>
            <div className="decision-ownership">
              <div><span>我的判断</span><p>主题是持续更新的语义视图，不是给笔记换一个文件夹。</p></div>
              <div><span>设计产出</span><p>主题名称、描述、候选记录选择、合并结果与来源记录。</p></div>
              <div><span>影响下游</span><p>主题边界决定语义召回范围，也影响 AI 搜索能否说明“为何找到这些记录”。</p></div>
            </div>
          </div>

          <div className="memory-flow" aria-label="主题形成与更新流程">
            <div><span>定义</span><strong>主题名称 + 详细描述</strong><p>同名词可能指向不同场景，描述同时约束检索与后续合并。</p></div>
            <ArrowRight aria-hidden="true" />
            <div><span>检索</span><strong>匹配相关备忘录</strong><p>AI 给候选，不直接合并。</p></div>
            <ArrowRight aria-hidden="true" />
            <div><span>确认</span><strong>用户选择来源</strong><p>只把选中的原文交给整合流程。</p></div>
            <ArrowRight aria-hidden="true" />
            <div><span>沉淀</span><strong>生成主题 + 合并记录</strong><p>保留时间与来源笔记。</p></div>
            <ArrowRight aria-hidden="true" />
            <div><span>更新</span><strong>计划中的持续匹配</strong><p>设计规则要求只处理新增、未整合的信息。</p></div>
          </div>
          <div className="memory-flow__evidence"><EvidenceTag kind="prototype">设计注释可核对，持续更新未由完整原型验证</EvidenceTag></div>

          <div className="memory-evidence">
            <ProductShot
              src="/evidence/merge-select.png"
              alt="相关备忘录选择页，多条记录带独立勾选框并提供开始整合操作"
              caption={<><EvidenceTag kind="verified" /> 先选择来源，再开始整合</>}
            />
            <ProductShot
              src="/product/themes.png"
              alt="主题页，包含系统主题、自定义主题和手动分类"
              caption={<><EvidenceTag kind="verified" /> 自定义主题保存合并时间与来源</>}
            />
            <div className="memory-contract">
              <LockKeyhole size={28} />
              <span>设计稿定义的整合输入边界</span>
              <h3>只合并原文，不把既有 AI 内容再次送入整合。</h3>
              <p>主题工作流说明要求排除既有 AI 生成信息，并筛掉已经整合过的内容。这让主题更新避免生成内容污染、循环摘要与重复合并。</p>
              <EvidenceTag kind="prototype">工作流注释可核对，不是该界面截图中的直接行为</EvidenceTag>
              <dl>
                <div><dt>进入</dt><dd>用户选中的原始备忘录</dd></div>
                <div><dt>排除</dt><dd>既有 AI 生成信息、已经整合过的信息</dd></div>
                <div><dt>留下</dt><dd>何时从哪条备忘录合入的记录</dd></div>
              </dl>
            </div>
          </div>

          <div className="search-design">
            <div className="search-design__copy">
              <Search size={28} />
              <span>同一个搜索动作，拆成三路结果</span>
              <h3>找不到文字、找不到关系、AI 无法回答，是三件不同的事。</h3>
              <p>关键词搜索负责确定性召回，相关备忘录负责语义关联，AI 回答则预留生成式解释的位置。无结果时三路分别反馈；即使其中一路失败，用户仍能判断问题出在文字召回、关系检索还是回答生成。</p>
              <ul>
                <li><strong>关键词</strong><span>“有没有这段文字”</span></li>
                <li><strong>相关记录</strong><span>“有没有语义相关的旧记录”</span></li>
                <li><strong>AI 回答</strong><span>“现有信息是否足以形成回答”</span></li>
              </ul>
              <EvidenceTag kind="verified">未发现非空 AI 回答画板，不宣称已验证生成答案</EvidenceTag>
            </div>
            <div className="search-design__shots">
              <ProductShot src="/evidence/search-result.png" alt="AI搜索有结果页面，分别展示关键词搜索与相关备忘录" caption="有结果 · 精确与语义并置" />
              <ProductShot src="/evidence/search-empty.png" alt="AI搜索无结果页面，分别展示关键词搜索零篇、相关备忘录零篇与AI回答无结果" caption="无结果 · 三路分别反馈" />
            </div>
          </div>
        </section>

        <section className="case-section multimodal-section" aria-labelledby="multimodal-title">
          <header className="section-heading">
            <div className="section-no">07 / 多模态捕获</div>
            <div>
              <h2 id="multimodal-title">语音和截图不是两个炫技入口，而是同一信息模型的两条低成本输入通道。</h2>
              <p>它们最终都被拆成“备忘录信息 + 待办 / 计划”，再复用同一套选择、写回与整理规则。</p>
            </div>
          </header>

          <div className="channel-matrix" aria-labelledby="channel-matrix-title">
            <div className="channel-matrix__intro">
              <span>架构复用检验</span>
              <h3 id="channel-matrix-title">输入不同，特有风险不同；形成的信息对象与写回契约保持一致。</h3>
              <p>这不是在产品里多放两个 AI 入口，而是在验证同一底层结构能否承接连续思考和外部证据。</p>
            </div>
            <div className="channel-matrix__table" role="table" aria-label="语音与截图设计对照">
              <div role="row" className="channel-matrix__head"><span role="columnheader">设计问题</span><strong role="columnheader">语音讨论</strong><strong role="columnheader">截图识别</strong></div>
              <div role="row"><span role="rowheader">输入特征</span><p role="cell">多轮、连续、可能被打断的思考过程</p><p role="cell">来自外部聊天、噪声更高、上下文可能缺失</p></div>
              <div role="row"><span role="rowheader">特有约束</span><p role="cell">聆听 / 说话 / 思考状态，以及打断、结束、失败重试</p><p role="cell">系统权限、截图检测、处理状态，并保留被识别图片</p></div>
              <div role="row"><span role="rowheader">统一输出</span><p role="cell">备忘录信息 + 待办 / 计划</p><p role="cell">生成的备忘录 + 提取的待办 / 计划</p></div>
              <div role="row"><span role="rowheader">统一规则</span><p role="cell">AI 结果分层，用户选择性存入正文</p><p role="cell">AI 结果与原图分开，用户局部或全部写回</p></div>
              <div role="row"><span role="rowheader">证明什么</span><p role="cell">统一信息模型可以承接思考过程</p><p role="cell">统一信息模型可以承接外部来源证据</p></div>
            </div>
            <EvidenceTag kind="prototype">状态和结果画板可核对；完整跨页面原型未验证</EvidenceTag>
          </div>

          <div className="channel-story">
            <article className="channel-copy">
              <div className="channel-icon"><Mic size={22} /></div>
              <span>语音讨论</span>
              <h3>把一条记录扩展成思考过程，而不是只做录音转文字。</h3>
              <p>多轮对话围绕当前记录继续；原型状态覆盖打断、结束和生成失败后的单项重试。结果被拆成备忘录信息与待办，两者分别“存入正文”；对话回复不会直接变成事实，仍要经过分块采纳与写回。</p>
              <ol>
                <li>聆听中 / 说话中 / 思考中各有明确反馈</li>
                <li>AI 正在说话时仍保留“打断”和“结束”</li>
                <li>对话原文、AI 总结与结构化结果分层</li>
                <li>总结可编辑、重新生成、删除或选择性写回</li>
              </ol>
              <EvidenceTag kind="prototype">状态覆盖可核对，未宣称已验证完整原型链路</EvidenceTag>
            </article>
            <ProductShot
              className="channel-shot"
              src="/evidence/voice-conversation.png"
              alt="语音讨论多轮状态，同时显示对话、备忘录信息、待办、思考中、失败重试、打断与结束"
              width={390}
              height={1056}
              caption="多轮语音讨论 · 结构化结果与控制同屏"
            />
          </div>

          <div className="channel-story channel-story--reverse">
            <article className="channel-copy">
              <div className="channel-icon"><FileInput size={22} /></div>
              <span>聊天截图识别</span>
              <h3>从系统级捕获开始，经过后台处理，最后保留原图证据。</h3>
              <p>识别链覆盖权限准备、检测截图、处理中、隐藏到后台、完成查看与网络失败重试。截图不会只作为附件保存，而是转成可编辑的备忘录与待办。</p>
              <ol>
                <li>解释无障碍、悬浮窗、后台活动等权限用途</li>
                <li>长任务可以隐藏到后台，不阻塞当前工作</li>
                <li>识别结果保留“被识别图片”，修正提取内容时仍可回看原始证据</li>
                <li>单块或全部写回，复用统一的人机确认机制</li>
              </ol>
              <EvidenceTag kind="verified">权限、处理、失败与结果状态均有设计稿</EvidenceTag>
            </article>
            <ProductShot
              className="channel-shot"
              src="/evidence/screenshot-structured.png"
              alt="截图识别结构化结果，展示生成的备忘录、提取的待办、被识别图片和全部存入正文"
              width={390}
              height={1317}
              caption="聊天截图 → 备忘录 / 待办 → 保留原图 → 用户写回"
            />
          </div>
        </section>

        <section className="case-section reliability-section" id="reliability" aria-labelledby="reliability-title">
          <header className="section-heading section-heading--wide">
            <div className="section-no">08 / 可靠性与设计债务</div>
            <div>
              <h2 id="reliability-title">AI 犯错不是异常页；等待、退出、失败、重试和版本变化都属于主流程。</h2>
              <p>这部分只展示源文件覆盖到的状态，再把仍缺少的机制明确列为下一轮问题，不用案例包装掩盖它们。</p>
            </div>
          </header>

          <div className="runtime-layout">
            <ProductShot
              className="runtime-shot"
              src="/evidence/auto-organizing.png"
              alt="自动整理运行中界面"
              caption={<><EvidenceTag kind="verified" /> 可见的异步任务状态</>}
            />
            <ol className="runtime-states">
              {runtimeStates.map((item, index) => (
                <li key={item.state}>
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <div><h3>{item.state}</h3><p>{item.visible}</p><small><ShieldCheck size={14} /> {item.control}</small></div>
                </li>
              ))}
            </ol>
          </div>

          <div className="honesty-ledger">
            <div className="honesty-ledger__head"><CircleAlert size={24} /><div><span>仍待补齐的产品问题</span><h3>把设计债务写出来，比虚构一个完美闭环更能说明判断力。</h3></div></div>
            <div className="honesty-ledger__rows">
              <article><span>取消能力</span><p>源稿规定退出后后台继续，但没有找到“取消正在运行的整理”。下一轮需要评估任务取消、费用提示和中间结果保留。</p></article>
              <article><span>完成文案</span><p>截图处理的完成态仍沿用“正在提取”的主文案，状态与操作“点击查看”不完全一致，是明确的细节债务。</p></article>
              <article><span>版本恢复</span><p>已设计时间戳、整理历史与重新生成，但没有证据支持 diff、旧版恢复或完整回滚，案例不越界宣称。</p></article>
              <article><span>验证规模</span><p>两轮小范围验证来自项目材料；没有原始样本与量表，因此不表述为 PMF、准确率或规模化研究结论。</p></article>
            </div>
          </div>

          <div className="late-response">
            <div className="late-response__intro"><Clock3 size={25} /><div><span>设计推演 · 非现网能力陈述</span><h3>正确的结果，也必须回到正确的对象与版本。</h3><p>下面是从现有写回边界继续推演的可靠性方案，用来说明产品下一阶段需要守住什么。</p></div><EvidenceTag kind="inference">需代码与测试验证</EvidenceTag></div>
            <ol>
              <li><time>00:00</time><strong>笔记 A 开始生成</strong><p>记录对象 ID 与内容版本。</p></li>
              <li><time>00:02</time><strong>用户切换到笔记 B</strong><p>当前上下文已经变化。</p></li>
              <li><time>00:04</time><strong>笔记 A 的结果返回</strong><p>对象不匹配，禁止写入当前界面。</p></li>
              <li><time>SAFE</time><strong>笔记 B 保持原样</strong><p>结果回到 A 的历史，或被安全丢弃。</p></li>
            </ol>
          </div>
        </section>

        <section className="case-section iteration-section" aria-labelledby="iteration-title">
          <header className="section-heading">
            <div className="section-no">09 / 版本学习</div>
            <div>
              <h2 id="iteration-title">从“增加 AI 能力”转向“管理 AI 行为”，是这个项目真正的版本演进。</h2>
              <p>版本不是三张封面，而是一条假设、暴露问题、改变系统规则的学习链。</p>
              <EvidenceTag kind="author">基于版本稿与项目复盘</EvidenceTag>
            </div>
          </header>

          <ol className="evolution-ledger">
            {evolution.map((item, index) => (
              <li key={item.stage}>
                <div className="evolution-stage"><span>{String(index + 1).padStart(2, '0')}</span><strong>{item.stage}</strong></div>
                <div><span>原假设</span><p>{item.hypothesis}</p></div>
                <div><span>项目观察</span><p>{item.observation}</p></div>
                <div><span>设计判断</span><p>{item.decision}</p></div>
                <div><span>具体改动</span><p>{item.change}</p></div>
              </li>
            ))}
          </ol>
          <p className="evolution-boundary">版本关系来自设计稿与项目复盘，用来说明决策如何变化；缺少原始研究样本的部分不包装成量化验证结论。</p>

          <div className="measurement-framework">
            <div className="measurement-intro"><Target size={27} /><span>下一阶段验证框架</span><h3>指标应该判断“信息是否被用起来”，不是证明用户在产品里待得更久。</h3><p>产品目标恰恰是减少整理时间，同时提高行动完成和旧信息再利用。</p><EvidenceTag kind="inference">建议指标 · 尚未测量</EvidenceTag></div>
            <dl className="metric-layers">
              <div><dt>北极星假设</dt><dd>无需手动整理，之后仍能形成有效行动或被再次利用的记录占比。</dd></div>
              <div><dt>任务体验</dt><dd>形成可用记录的耗时、补问一次完成率、局部采纳率、相关记录打开率。</dd></div>
              <div><dt>智能质量</dt><dd>实体与行动字段准确率、错误补全率、无帮助关系率、用户修改率。</dd></div>
              <div><dt>信任护栏</dt><dd>跨笔记写入、未确认写回、无权限来源暴露应以零为目标。</dd></div>
              <div><dt>系统效率</dt><dd>首个可用结果延迟、模块失败率、重试恢复率与单次有用结果成本。</dd></div>
            </dl>
          </div>
        </section>

        <section className="case-section method-section" id="about" aria-labelledby="method-title">
          <div className="method-lead"><Layers3 size={31} /><span>我的 AI 产品设计方法</span><h2 id="method-title">模型能力只是材料；产品设计负责把它变成可理解、可控制、可恢复的协作。</h2></div>

          <div className="quality-model" aria-label="AI UX 五个质量维度及本项目证据">
            <div className="quality-model__formula"><span>传统 UX</span><strong>界面可用性 + 任务流程体验</strong><ArrowRight aria-hidden="true" /><span>AI UX</span><strong>再加入智能行为、人机协作与结果可靠性</strong></div>
            <ol>
              <li><span>界面可用性</span><p>先保存、再处理；状态和操作始终贴近对应模块。</p><a href="#writeback">证据：保存与整理分离</a></li>
              <li><span>任务流程体验</span><p>捕获、理解、行动、关联和复用形成连续路径。</p><a href="#system">证据：系统工作流</a></li>
              <li><span>智能行为质量</span><p>明确什么可以推断，什么必须补问，什么应保持为空。</p><a href="#choices">证据：能力推荐与语义补问</a></li>
              <li><span>人机协作质量</span><p>AI 提议，用户选择；生成、采纳与写回拥有不同权限。</p><a href="#writeback">证据：可操作写回演示</a></li>
              <li><span>结果可靠性</span><p>来源、对象、版本、失败和迟到响应都进入主流程。</p><a href="#reliability">证据：状态与设计债务</a></li>
            </ol>
          </div>

          <ol className="method-stack" aria-label="AI共生系统的设计流程">
            <li><span>目标与风险</span><p>先确定要减少的真实劳动，以及错误结果可能造成的后果。</p></li>
            <li><span>人机分工</span><p>AI 负责理解、推荐与生成；人保留事实判断和持续授权。</p></li>
            <li><span>智能行为</span><p>把补问、推荐、运行和生成放到任务中最合适的时机。</p></li>
            <li><span>行为契约</span><p>明确上下文、作用范围、触发来源、写回权限和数据边界。</p></li>
            <li><span>评测校准</span><p>用采纳、修改、错误补全、失败恢复与信息复用持续校准。</p></li>
            <li><span>确定控制</span><p>让结果可核对、操作可撤回、失败可恢复；未完成的机制如实标注。</p></li>
          </ol>
        </section>

        <section className="case-ending" aria-labelledby="ending-title">
          <div>
            <span>结论</span>
            <h2 id="ending-title">AI UX 的价值，不是让模型显得更聪明，而是让智能行为值得被采用。</h2>
          </div>
          <aside>
            <p>这个项目体现的不是页面数量，而是我能从模糊问题出发，完成产品定义、系统拆解、关键交互、状态设计、跨职能推进与证据边界管理。</p>
            <a href="#top">回到案例开头 <ArrowUpRight size={17} /></a>
          </aside>
        </section>
      </div>

      <footer className="case-footer">
        <div><Image src="/logo.webp" alt="" width={28} height={28} /><strong>AI备忘录 · 产品设计案例</strong></div>
        <nav aria-label="站点服务入口"><a href="/download">下载状态</a><a href="/share">分享状态</a><a href="/support">服务说明</a></nav>
        <span>刘双广 · 2026</span>
      </footer>
    </main>
  );
}
