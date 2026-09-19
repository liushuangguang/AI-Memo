"use client";

import React, { useState, useEffect } from 'react';
import './portfolio.css';

// 竞品象限数据
const QUADRANT_COMPETITORS = [
  {
    id: 'ainote',
    name: '🌟 AI备忘录 (本项目)',
    x: 85,
    y: 85,
    isHero: true,
    tag: '黄金象限 · 零摩擦输入 + 行动闭环',
    burden: '极低 (随手文字/语音/截图秒存)',
    value: '极高 (自动四层解耦，直接给可用待办与旧信息)',
    pros: '迎合人性懒到底，不强迫排版；结论前置，原文稳态，带一键撤销自由。',
    cons: '需要谨慎控制多模型调用开销与网络抖动降级。'
  },
  {
    id: 'notion',
    name: 'Notion AI',
    x: 25,
    y: 75,
    isHero: false,
    tag: '全能重型知识库',
    burden: '极高 (需维护数据库、属性、视图与复杂Block)',
    value: '高 (高度结构化，适合团队协作与长篇文档)',
    pros: '强大的块编辑器与多维度数据库。',
    cons: '随手记录心智负担极重，移动端打开繁琐，普通人难以长期坚持整理。'
  },
  {
    id: 'flomo',
    name: 'Flomo 浮墨笔记',
    x: 90,
    y: 30,
    isHero: false,
    tag: '极轻碎片记录工具',
    burden: '极低 (像发微博一样无压力记想法)',
    value: '中低 (需依赖人工打标签与定期主动回顾，断点在保存后)',
    pros: '极度克制，无排版负担，输入门槛降到最低。',
    cons: '记录堆积成山后难以自动变成下一步行动，缺少深层语义关联。'
  },
  {
    id: 'mebot',
    name: 'Mebot / 对话式AI',
    x: 65,
    y: 45,
    isHero: false,
    tag: 'AI个人伙伴',
    burden: '中等 (聊天式互动)',
    value: '中等 (长段AI文本容易产生阅读疲劳)',
    pros: '对话自然，有情绪价值。',
    cons: 'AI回复长篇大论，无法直接提取成结构化清单，用户改不动也用不上。'
  },
  {
    id: 'apple',
    name: '系统备忘录 + Siri',
    x: 75,
    y: 20,
    isHero: false,
    tag: '系统原生工具',
    burden: '极低 (系统级常驻入口)',
    value: '低 (单纯文本沉淀，无智能化后处理)',
    pros: '生态无处不在，调起速度极快。',
    cons: '存完即遗忘，没有任何智能整理与后续连接能力。'
  }
];

export default function PortfolioMasterPage() {
  // 滚动阅读进度
  const [scrollProgress, setScrollProgress] = useState(0);
  const [activeSection, setActiveSection] = useState('hero');

  // 全局大图查看模态框
  const [zoomImage, setZoomImage] = useState<{ src: string; title: string; desc?: string } | null>(null);

  // 首屏设备交互 Pin
  const [activeHeroPin, setActiveHeroPin] = useState(1);

  // 象限选中的竞品
  const [selectedCompetitor, setSelectedCompetitor] = useState(QUADRANT_COMPETITORS[0]);

  // 链路 1 交互沙盒：微信截图 OCR 流程
  const [captureStage, setCaptureStage] = useState<'chat' | 'floating' | 'ocr_done'>('ocr_done');

  // 链路 2 交互沙盒：含糊词追问
  const [clarifyState, setClarifyState] = useState<'pending' | 'dog' | 'cat' | 'keep'>('dog');
  const [showClarifyUndo, setShowClarifyUndo] = useState(true);

  // 链路 3 交互沙盒：四层解耦整理
  const [organizeTodos, setOrganizeTodos] = useState([
    { id: 1, text: '周六 20:00 前冰箱断电化霜并清空食材', done: false },
    { id: 2, text: '周日 08:30 整理打包客厅大型家具', done: false },
    { id: 3, text: '联系搬家李师傅确认货车时间与到达定位', done: true },
    { id: 4, text: '准备雨天防护防潮薄膜包裹布艺沙发', done: false }
  ]);
  const [suggestionAdopted, setSuggestionAdopted] = useState(false);
  const [showOrganizeUndo, setShowOrganizeUndo] = useState(false);

  // 链路 4 交互沙盒：主题合成
  const [selectedThemeSource, setSelectedThemeSource] = useState<string | null>(null);

  // 监听滚动与进度条
  useEffect(() => {
    const handleScroll = () => {
      const total = document.documentElement.scrollHeight - window.innerHeight;
      if (total > 0) {
        setScrollProgress((window.scrollY / total) * 100);
      }

      // 计算当前处于哪个章节
      const sections = ['hero', 'quadrant', 'core-flows', 'evolution', 'goodnotes', 'reliability', 'evidence'];
      for (const id of sections) {
        const el = document.getElementById(id);
        if (el) {
          const rect = el.getBoundingClientRect();
          if (rect.top <= 200 && rect.bottom >= 200) {
            setActiveSection(id);
            break;
          }
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="portfolio-app-root">
      {/* 顶部悬浮毛玻璃导航条 */}
      <nav className="top-nav-bar">
        <div className="nav-reading-progress" style={{ width: `${scrollProgress}%` }} />
        <div className="nav-brand-group">
          <div className="nav-brand-badge">
            <div className="nav-brand-dot" />
            <span>AI备忘录 · UX 旗舰设计作品集</span>
          </div>
        </div>

        <div className="nav-chapter-links">
          <a href="#hero" className={`nav-chapter-link ${activeSection === 'hero' ? 'active' : ''}`}>00. 概览</a>
          <a href="#quadrant" className={`nav-chapter-link ${activeSection === 'quadrant' ? 'active' : ''}`}>01. 挑战与定位</a>
          <a href="#core-flows" className={`nav-chapter-link ${activeSection === 'core-flows' ? 'active' : ''}`}>02. 四大核心链路</a>
          <a href="#evolution" className={`nav-chapter-link ${activeSection === 'evolution' ? 'active' : ''}`}>03. 新旧突破对比</a>
          <a href="#goodnotes" className={`nav-chapter-link ${activeSection === 'goodnotes' ? 'active' : ''}`}>04. Goodnotes 迁移</a>
          <a href="#reliability" className={`nav-chapter-link ${activeSection === 'reliability' ? 'active' : ''}`}>05. 可靠性体系</a>
          <a href="#evidence" className={`nav-chapter-link ${activeSection === 'evidence' ? 'active' : ''}`}>06. 事实与边界</a>
        </div>

        <a href="/auto-organize" className="nav-action-cta" target="_blank" rel="noreferrer">
          <span>体验极奢全链路原型</span>
          <span>↗</span>
        </a>
      </nav>

      {/* 3D 长虹条纹磨砂玻璃环境背景板 */}
      <div className="fluted-ambient-glass fluted-glass-hero-left" />
      <div className="fluted-ambient-glass fluted-glass-hero-right" />

      {/* 页面主展厅容器 */}
      <div className="portfolio-master-wrap">
        
        {/* ====================================================
           SECTION 00: HERO & 3D SHOWROOM
           ==================================================== */}
        <section id="hero" className="hero-stage-grid">
          <div className="hero-narrative-card">
            <div className="hero-role-pill-row">
              <span className="role-pill">资深产品经理 & UX 设计师作品集</span>
              <span className="role-pill">Goodnotes 旗舰案例</span>
              <span className="role-pill" style={{ color: '#059669', background: '#ecfdf5', borderColor: '#a7f3d0' }}>
                v0.1.5 Build 7 真机验证已闭环
              </span>
            </div>

            <h1 className="hero-main-heading">
              AI 备忘录：让一条随手记录，继续发挥价值
            </h1>

            <blockquote className="hero-lead-quote">
              “用户愿意在当下随手记下一句话，真正的断点往往发生在记录之后。信息被存下来了，事情依然没有着落。”
            </blockquote>

            <p style={{ fontSize: '15px', color: '#475569', lineHeight: 1.7, margin: 0 }}>
              从低成本捕捉、克制可取舍的 AI 整理，到旧信息语义唤醒与后续行动闭环。我负责从问题定义、核心链路交互设计、全套高保真原型制作，到研发落地与真机验收的完整闭环。
            </p>

            {/* 4 大核心设计判断 */}
            <div className="hero-judgment-grid">
              <div className="judgment-mini-card">
                <span className="judgment-num">判断 01 · 认知时机</span>
                <h4>时机服从认知</h4>
                <p>AI 出现的时机严格服从用户记录当下的稀缺注意力，先秒存，整理在后台异步发生。</p>
              </div>

              <div className="judgment-mini-card">
                <span className="judgment-num">判断 02 · 原文稳态</span>
                <h4>用户绝对掌权</h4>
                <p>用户的原始草稿是神圣不可侵犯的参照物，AI 建议必须可点选、可采纳、可随时一键 Undo。</p>
              </div>

              <div className="judgment-mini-card">
                <span className="judgment-num">判断 03 · 出处可溯</span>
                <h4>关联必须给依据</h4>
                <p>AI 总结的结论与召回的历史记忆必须提供原始来源链接，坚决消灭 AI 幻觉。</p>
              </div>

              <div className="judgment-mini-card">
                <span className="judgment-num">判断 04 · 行动闭环</span>
                <h4>终点必须连行动</h4>
                <p>一次 AI 整理的终点，绝不是生成长篇大论的死文本，而是直接输出可打钩办结的行动项。</p>
              </div>
            </div>
          </div>

          {/* 3D 展台中央手机展示 */}
          <div className="hero-stage-device-wrap">
            <div className="iphone-pro-chassis">
              <div className="iphone-pro-screen">
                {/* 状态栏 */}
                <div style={{ height: '44px', padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: '#ffffff', fontSize: '12px', fontWeight: 700, zIndex: 10 }}>
                  <span>09:41</span>
                  <div style={{ width: '108px', height: '28px', background: '#000', borderRadius: '18px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '10px' }}>
                    AI备忘录
                  </div>
                  <span>5G 100%</span>
                </div>

                {/* 真实 UI 预览图 */}
                <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
                  <img
                    src="/ui/entry.png"
                    alt="AI备忘录主界面真实切图"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />

                  {/* 4 个呼吸交互热点 */}
                  <div
                    className={`device-hotspot-pin ${activeHeroPin === 1 ? 'active' : ''}`}
                    style={{ top: '160px', left: '48px' }}
                    onClick={() => setActiveHeroPin(1)}
                  >
                    1
                  </div>

                  <div
                    className={`device-hotspot-pin ${activeHeroPin === 2 ? 'active' : ''}`}
                    style={{ top: '270px', right: '48px' }}
                    onClick={() => setActiveHeroPin(2)}
                  >
                    2
                  </div>

                  <div
                    className={`device-hotspot-pin ${activeHeroPin === 3 ? 'active' : ''}`}
                    style={{ top: '440px', left: '60px' }}
                    onClick={() => setActiveHeroPin(3)}
                  >
                    3
                  </div>

                  <div
                    className={`device-hotspot-pin ${activeHeroPin === 4 ? 'active' : ''}`}
                    style={{ bottom: '120px', right: '60px' }}
                    onClick={() => setActiveHeroPin(4)}
                  >
                    4
                  </div>

                  {/* 联动详情浮层卡 */}
                  <div className="hotspot-active-detail-card">
                    {activeHeroPin === 1 && (
                      <>
                        <span className="hotspot-badge">Pin 1 · 零摩擦快速捕获</span>
                        <h4>微信截图自动送识别</h4>
                        <p>支持随手文字、语音与微信截图。默认不监听，开启后弹出悬浮预览确认才 OCR，把控制权还给用户。</p>
                      </>
                    )}
                    {activeHeroPin === 2 && (
                      <>
                        <span className="hotspot-badge">Pin 2 · 上下文消除歧义</span>
                        <h4>备忘录信息完善 (含糊词补问)</h4>
                        <p>以“给旺财洗澡”为例，只补问会改变分类的关键缺口（宠物还是亲友），卡片化单选，支持一键撤销。</p>
                      </>
                    )}
                    {activeHeroPin === 3 && (
                      <>
                        <span className="hotspot-badge">Pin 3 · 四层解耦组织</span>
                        <h4>一键智能整理</h4>
                        <p>四层结构：①原文稳态锚点、②核心行动待办、③AI贴心建议、④历史旧记忆出处，结论前置，细节后退。</p>
                      </>
                    )}
                    {activeHeroPin === 4 && (
                      <>
                        <span className="hotspot-badge">Pin 4 · 多维知识聚合</span>
                        <h4>信息合并（主题功能）</h4>
                        <p>主题是灵活映射视图而非物理搬家剪切，多篇笔记自动聚合为指南卡片，所有建议出处均可溯源。</p>
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* 3D 展台悬浮投影 */}
            <div className="device-pedestal-shadow" />
          </div>
        </section>

        {/* ====================================================
           SECTION 01: PROBLEM & 2D QUADRANT
           ==================================================== */}
        <section id="quadrant">
          <div className="chapter-header">
            <div className="chapter-number-pill">Chapter 01 · 市场盲区与体验定位</div>
            <h2 className="chapter-title">
              传统备忘录只解决了“存”，<br />
              真正的断点发生在记录之后
            </h2>
            <p className="chapter-subtitle">
              人们在当下愿意快速记下一句话或截一张图，但极少有人愿意在日后持续手动分类、贴标签和整理。碎片堆积如山，最终沦为信息的数字墓地。
            </p>
          </div>

          <div className="quadrant-master-container">
            {/* 2D 象限图交互画布 */}
            <div className="quadrant-canvas-box">
              <div className="quadrant-axes-cross">
                <div className="quadrant-axis-x" />
                <div className="quadrant-axis-y" />
                <div className="quadrant-target-golden-zone" />
              </div>

              {/* 轴线文案标注 */}
              <span className="quadrant-axis-label" style={{ top: '10px', left: '50%', transform: 'translateX(-50%)' }}>
                ▲ 行动落地闭环 (直接可用)
              </span>
              <span className="quadrant-axis-label" style={{ bottom: '10px', left: '50%', transform: 'translateX(-50%)' }}>
                ▼ 单纯碎片归档 (容易遗忘)
              </span>
              <span className="quadrant-axis-label" style={{ top: '50%', right: '10px', transform: 'translateY(-50%)' }}>
                心智负担极轻 (随便记) ►
              </span>
              <span className="quadrant-axis-label" style={{ top: '50%', left: '10px', transform: 'translateY(-50%)' }}>
                ◄ 心智负担极重 (复杂排版)
              </span>

              {/* 各产品气泡落位 */}
              {QUADRANT_COMPETITORS.map(comp => (
                <div
                  key={comp.id}
                  className={`quadrant-product-bubble ${comp.isHero ? 'hero-product' : ''} ${selectedCompetitor.id === comp.id ? 'active' : ''}`}
                  style={{ left: `${comp.x}%`, top: `${100 - comp.y}%` }}
                  onClick={() => setSelectedCompetitor(comp)}
                >
                  <span>{comp.name}</span>
                </div>
              ))}
            </div>

            {/* 右侧竞品深度剖析与产品突破 */}
            <div className="quadrant-analysis-panel">
              <div className="competitor-detail-card">
                <div className="competitor-title-row">
                  <h3>{selectedCompetitor.name}</h3>
                  <span style={{ fontSize: '12px', fontWeight: 750, color: selectedCompetitor.isHero ? '#10b981' : '#64748b' }}>
                    {selectedCompetitor.tag}
                  </span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginTop: '4px' }}>
                  <div style={{ background: '#ffffff', padding: '10px 12px', borderRadius: '12px' }}>
                    <span style={{ fontSize: '11px', color: '#94a3b8', display: 'block' }}>记录心智负担：</span>
                    <span style={{ fontSize: '12.5px', fontWeight: 700, color: '#0f172a' }}>{selectedCompetitor.burden}</span>
                  </div>
                  <div style={{ background: '#ffffff', padding: '10px 12px', borderRadius: '12px' }}>
                    <span style={{ fontSize: '11px', color: '#94a3b8', display: 'block' }}>行动落地价值：</span>
                    <span style={{ fontSize: '12.5px', fontWeight: 700, color: '#0f172a' }}>{selectedCompetitor.value}</span>
                  </div>
                </div>

                <div style={{ marginTop: '6px' }}>
                  <h5 style={{ margin: '0 0 4px', fontSize: '12.5px', color: '#059669' }}>优势：</h5>
                  <p style={{ margin: 0, fontSize: '12px', color: '#475569', lineHeight: 1.5 }}>{selectedCompetitor.pros}</p>
                </div>

                <div>
                  <h5 style={{ margin: '0 0 4px', fontSize: '12.5px', color: '#dc2626' }}>体验痛点与局限：</h5>
                  <p style={{ margin: 0, fontSize: '12px', color: '#475569', lineHeight: 1.5 }}>{selectedCompetitor.cons}</p>
                </div>
              </div>

              {/* 用户状态转移矩阵 */}
              <div style={{ background: 'linear-gradient(135deg, rgba(255,255,255,0.9), rgba(248,250,252,0.9))', padding: '20px', borderRadius: '24px', border: '1px solid rgba(15,23,42,0.06)' }}>
                <h4 style={{ margin: '0 0 8px', fontSize: '14px', fontWeight: 800 }}>用户状态比用户标签更重要</h4>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px', fontSize: '11.5px' }}>
                  <div style={{ background: '#ffffff', padding: '8px', borderRadius: '10px', textAlign: 'center' }}>
                    <strong style={{ color: '#0284c7' }}>怕忘</strong>
                    <span style={{ display: 'block', color: '#64748b', marginTop: '2px' }}>尽快留下少填字段</span>
                  </div>
                  <div style={{ background: '#ffffff', padding: '8px', borderRadius: '10px', textAlign: 'center' }}>
                    <strong style={{ color: '#d97706' }}>没空整理</strong>
                    <span style={{ display: 'block', color: '#64748b', marginTop: '2px' }}>自动分层保留选择</span>
                  </div>
                  <div style={{ background: '#ffffff', padding: '8px', borderRadius: '10px', textAlign: 'center' }}>
                    <strong style={{ color: '#7c3aed' }}>想找回</strong>
                    <span style={{ display: 'block', color: '#64748b', marginTop: '2px' }}>语义原因唤醒旧条</span>
                  </div>
                  <div style={{ background: '#ffffff', padding: '8px', borderRadius: '10px', textAlign: 'center' }}>
                    <strong style={{ color: '#059669' }}>要推进</strong>
                    <span style={{ display: 'block', color: '#64748b', marginTop: '2px' }}>看见明确下一步</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ====================================================
           SECTION 02: THE 4 CORE MASTERCLASS DEEP DIVES
           ==================================================== */}
        <section id="core-flows">
          <div className="chapter-header">
            <div className="chapter-number-pill">Chapter 02 · 四大核心链路深入拆解</div>
            <h2 className="chapter-title">
              像大师课一样讲透：<br />
              为什么做、方案选择与取舍哲学
            </h2>
            <p className="chapter-subtitle">
              针对微信截图捕获、含糊词消歧完善、四层解耦整理与多记录主题合并，完整呈现真实 Figma 3x 原图、可动手交互沙盒与 UX 深度决策三问。
            </p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '48px' }}>
            
            {/* 链路 1: 微信截图自动送识别 */}
            <div className="flow-deep-dive-card">
              <div className="flow-header-row">
                <div className="flow-title-block">
                  <span className="flow-tag-pill">核心链路 01 · 零摩擦捕获</span>
                  <h3>微信截图自动送识别 (OCR & Capture)</h3>
                </div>
                <div style={{ fontSize: '13px', color: '#64748b' }}>
                  对应真机界面：<strong>entry.png</strong> · 真实源文件 3 倍切图导出
                </div>
              </div>

              <div className="flow-split-layout">
                {/* 真实切图展窗 */}
                <div className="figma-screen-viewport-box">
                  <div className="figma-viewport-header">
                    <span>Figma V3 设计稿 · 截图自动捕获浮层</span>
                    <span>1170 × 2532 px</span>
                  </div>
                  <div className="figma-image-container" onClick={() => setZoomImage({ src: '/ui/entry.png', title: '微信截图自动送识别 · entry.png', desc: '展示了微信后台截屏后，应用弹出的悬浮确认气泡、自动OCR预览与意图补充卡片。' })}>
                    <img src="/ui/entry.png" alt="微信截图自动识别界面" />
                    <div className="figma-zoom-badge">
                      <span>🔍 点击查看 3X 原图</span>
                    </div>
                  </div>
                </div>

                {/* 交互沙盒与 UX 剖析 */}
                <div className="flow-sandbox-and-rationale">
                  <div className="interactive-sandbox-container">
                    <div className="sandbox-header">
                      <span>🎮 可动手交互演示 · 模拟截图识别全流程</span>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          style={{ padding: '4px 10px', borderRadius: '6px', border: 'none', background: captureStage === 'chat' ? '#0f172a' : '#e2e8f0', color: captureStage === 'chat' ? '#fff' : '#64748b', cursor: 'pointer', fontSize: '11px' }}
                          onClick={() => setCaptureStage('chat')}
                        >
                          1. 微信聊天截图
                        </button>
                        <button
                          style={{ padding: '4px 10px', borderRadius: '6px', border: 'none', background: captureStage === 'floating' ? '#0f172a' : '#e2e8f0', color: captureStage === 'floating' ? '#fff' : '#64748b', cursor: 'pointer', fontSize: '11px' }}
                          onClick={() => setCaptureStage('floating')}
                        >
                          2. 悬浮微光弹出
                        </button>
                        <button
                          style={{ padding: '4px 10px', borderRadius: '6px', border: 'none', background: captureStage === 'ocr_done' ? '#0f172a' : '#e2e8f0', color: captureStage === 'ocr_done' ? '#fff' : '#64748b', cursor: 'pointer', fontSize: '11px' }}
                          onClick={() => setCaptureStage('ocr_done')}
                        >
                          3. 本地识别待办
                        </button>
                      </div>
                    </div>

                    {/* 模拟手机画面 */}
                    <div style={{ background: '#ffffff', borderRadius: '18px', padding: '16px', border: '1px solid #e2e8f0', minHeight: '140px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                      {captureStage === 'chat' && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#07c160', color: '#fff', display: 'grid', placeItems: 'center', fontSize: '18px' }}>💬</div>
                          <div>
                            <strong style={{ fontSize: '13px' }}>微信聊天截屏动作已触发</strong>
                            <p style={{ margin: '2px 0 0', fontSize: '11.5px', color: '#64748b' }}>系统检测到相册新增一张会议纪要截图...</p>
                          </div>
                        </div>
                      )}

                      {captureStage === 'floating' && (
                        <div style={{ background: 'linear-gradient(135deg, #f8fafc, #f1f5f9)', border: '1.5px solid #38bdf8', padding: '14px', borderRadius: '14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                          <div>
                            <span style={{ fontSize: '11px', color: '#0284c7', fontWeight: 750 }}>⚡ 悬浮智能感知气泡</span>
                            <p style={{ margin: '2px 0 0', fontSize: '12.5px', fontWeight: 650, color: '#0f172a' }}>检测到会议任务：“下周二上午发PRD”</p>
                          </div>
                          <button style={{ padding: '6px 12px', borderRadius: '999px', background: '#0f172a', color: '#fff', border: 'none', fontSize: '11px', fontWeight: 700, cursor: 'pointer' }} onClick={() => setCaptureStage('ocr_done')}>
                            确认导入
                          </button>
                        </div>
                      )}

                      {captureStage === 'ocr_done' && (
                        <div style={{ background: '#f0fdf4', border: '1px solid #86efac', padding: '14px', borderRadius: '14px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                            <span style={{ fontSize: '11px', color: '#15803d', fontWeight: 750 }}>✓ 离线 OCR 生成成功</span>
                            <span style={{ fontSize: '10.5px', color: '#86efac', background: '#166534', padding: '1px 6px', borderRadius: '4px' }}>端侧安全处理</span>
                          </div>
                          <p style={{ margin: 0, fontSize: '12.5px', color: '#166534', fontWeight: 600 }}>
                            已生成备忘录：“下周二 10:00 前完成 AI 备忘录 PRD 初版并发送至团队邮箱”
                          </p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* UX 三问卡片 */}
                  <div className="ux-three-questions-grid">
                    <div className="ux-question-card">
                      <span className="ux-q-badge why">为什么做</span>
                      <h4>击碎输入阻力</h4>
                      <p>微信聊天、公众号和外卖单是国内用户最常见的信息碎片。手动打字成本极高，截图是最低摩擦的捕获方式。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge how">方案选择</span>
                      <h4>单次确认与离线预览</h4>
                      <p>默认不监听后台相册；开启权限后仍保留“单次悬浮确认”，先在本地 OCR 展示预览，确认后再入库，杜绝误读与隐私恐惧。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge tradeoff">优势与取舍</span>
                      <h4>图片识别 ≠ 用户意图</h4>
                      <p>绝不把截图中所有文字全盘当成待办。始终同时保留原截图与提取字段，支持随时对照修改与一键跳过。</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* 链路 2: 备忘录信息完善 */}
            <div className="flow-deep-dive-card">
              <div className="flow-header-row">
                <div className="flow-title-block">
                  <span className="flow-tag-pill">核心链路 02 · 上下文消除歧义</span>
                  <h3>备忘录信息完善 (Disambiguation & Clarify)</h3>
                </div>
                <div style={{ fontSize: '13px', color: '#64748b' }}>
                  对应真机界面：<strong>clarify.png</strong> · 真实源文件 3 倍切图导出
                </div>
              </div>

              <div className="flow-split-layout">
                {/* 真实切图展窗 */}
                <div className="figma-screen-viewport-box">
                  <div className="figma-viewport-header">
                    <span>Figma V3 设计稿 · 含糊词补问交互</span>
                    <span>1170 × 2532 px</span>
                  </div>
                  <div className="figma-image-container" onClick={() => setZoomImage({ src: '/ui/clarify.png', title: '备忘录信息完善 · clarify.png', desc: '展示了针对“旺财”歧义词，系统弹出的卡片式低摩擦候选补问，以及轻量化点选界面。' })}>
                    <img src="/ui/clarify.png" alt="含糊词追问界面" />
                    <div className="figma-zoom-badge">
                      <span>🔍 点击查看 3X 原图</span>
                    </div>
                  </div>
                </div>

                {/* 交互沙盒与 UX 剖析 */}
                <div className="flow-sandbox-and-rationale">
                  <div className="interactive-sandbox-container">
                    <div className="sandbox-header">
                      <span>🎮 可动手交互演示 · 点选候选词与即时撤销</span>
                      <span style={{ color: '#10b981', fontWeight: 600 }}>点击下方选项体验平滑替换</span>
                    </div>

                    {/* 正文模拟卡 */}
                    <div style={{ background: '#ffffff', borderRadius: '18px', padding: '16px', border: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      <div style={{ fontSize: '14px', lineHeight: 1.6, color: '#0f172a' }}>
                        “周末搬家，给
                        <span style={{ 
                          background: clarifyState === 'dog' ? '#dcfce7' : clarifyState === 'cat' ? '#fef3c7' : '#f1f5f9',
                          color: clarifyState === 'dog' ? '#15803d' : clarifyState === 'cat' ? '#b45309' : '#0f172a',
                          padding: '2px 8px',
                          borderRadius: '6px',
                          fontWeight: 750,
                          borderBottom: '2px solid currentColor'
                        }}>
                          {clarifyState === 'dog' ? '🐶 宠物小狗旺财' : clarifyState === 'cat' ? '🐱 宠物猫旺财' : '旺财'}
                        </span>
                        换窝，记得买拖把。”
                      </div>

                      {/* 选项卡片点选 */}
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', paddingTop: '8px', borderTop: '1px solid #f1f5f9' }}>
                        <span style={{ fontSize: '11px', color: '#94a3b8', alignSelf: 'center' }}>补问：旺财是谁？</span>
                        <button
                          style={{ padding: '6px 12px', borderRadius: '999px', border: clarifyState === 'dog' ? '1.5px solid #10b981' : '1px solid #e2e8f0', background: clarifyState === 'dog' ? '#ecfdf5' : '#fff', color: clarifyState === 'dog' ? '#047857' : '#334155', fontSize: '11.5px', fontWeight: 700, cursor: 'pointer' }}
                          onClick={() => { setClarifyState('dog'); setShowClarifyUndo(true); }}
                        >
                          🐶 宠物小狗
                        </button>
                        <button
                          style={{ padding: '6px 12px', borderRadius: '999px', border: clarifyState === 'cat' ? '1.5px solid #f59e0b' : '1px solid #e2e8f0', background: clarifyState === 'cat' ? '#fffbeb' : '#fff', color: clarifyState === 'cat' ? '#b45309' : '#334155', fontSize: '11.5px', fontWeight: 700, cursor: 'pointer' }}
                          onClick={() => { setClarifyState('cat'); setShowClarifyUndo(true); }}
                        >
                          🐱 宠物猫
                        </button>
                        <button
                          style={{ padding: '6px 12px', borderRadius: '999px', border: clarifyState === 'keep' ? '1.5px solid #64748b' : '1px solid #e2e8f0', background: clarifyState === 'keep' ? '#f8fafc' : '#fff', color: '#475569', fontSize: '11.5px', fontWeight: 700, cursor: 'pointer' }}
                          onClick={() => { setClarifyState('keep'); setShowClarifyUndo(false); }}
                        >
                          保留原词
                        </button>
                      </div>

                      {/* 撤销胶囊 */}
                      {showClarifyUndo && (
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#0f172a', color: '#fff', padding: '6px 14px', borderRadius: '999px', fontSize: '11px', marginTop: '4px' }}>
                          <span>✓ 正文已自动补全上下文</span>
                          <span style={{ color: '#38bdf8', cursor: 'pointer', fontWeight: 750 }} onClick={() => { setClarifyState('keep'); setShowClarifyUndo(false); }}>
                            撤销修改 (Undo)
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* UX 三问卡片 */}
                  <div className="ux-three-questions-grid">
                    <div className="ux-question-card">
                      <span className="ux-q-badge why">为什么做</span>
                      <h4>消灭算法盲猜</h4>
                      <p>用户语言高度口语化且充斥代称。“旺财”是狗还是人，会彻底改变后续分类（宠物用品 vs 朋友聚会）与日程输出。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge how">方案选择</span>
                      <h4>只问会改变结果的问题</h4>
                      <p>把追问限制在“会改变行动结果”的致命缺口。绝不连环盘问，做成一键点选的胶囊按钮，不打断记录节奏。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge tradeoff">优势与取舍</span>
                      <h4>保留原词与反悔自由</h4>
                      <p>允许用户直接跳过或保留原词；点选替换后屏幕立即常驻 Undo 胶囊，让用户拥有绝对的安全感与掌控感。</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* 链路 3: 一键智能整理 */}
            <div className="flow-deep-dive-card">
              <div className="flow-header-row">
                <div className="flow-title-block">
                  <span className="flow-tag-pill">核心链路 03 · 结构化价值跃迁</span>
                  <h3>一键智能整理 (4-Tier Decoupled Organization)</h3>
                </div>
                <div style={{ fontSize: '13px', color: '#64748b' }}>
                  对应真机界面：<strong>organize.png</strong> · 4K 级 696KB 完整长图
                </div>
              </div>

              <div className="flow-split-layout">
                {/* 真实切图展窗 */}
                <div className="figma-screen-viewport-box">
                  <div className="figma-viewport-header">
                    <span>Figma V3 设计稿 · 四层解耦长屏全览</span>
                    <span>1170 × 4608 px (长图支持滚动)</span>
                  </div>
                  <div className="figma-image-container" style={{ maxHeight: '560px', overflowY: 'auto' }} onClick={() => setZoomImage({ src: '/ui/organize.png', title: '一键智能整理 · organize.png (长屏全图)', desc: '四层解耦完整架构：顶部结论前置、原文稳态、核心行动项清单、AI 建议便签与历史记忆出处卡片。' })}>
                    <img src="/ui/organize.png" alt="一键智能整理长图" style={{ width: '100%', height: 'auto' }} />
                    <div className="figma-zoom-badge">
                      <span>🔍 点击全屏展开 4K 长图</span>
                    </div>
                  </div>
                </div>

                {/* 交互沙盒与 UX 剖析 */}
                <div className="flow-sandbox-and-rationale">
                  <div className="interactive-sandbox-container">
                    <div className="sandbox-header">
                      <span>🎮 可动手交互演示 · 四层解耦操作台</span>
                      <span style={{ color: '#0284c7' }}>可点击打钩待办 / 采纳建议</span>
                    </div>

                    {/* 四层卡片演示 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      {/* 层级 1 */}
                      <div style={{ background: '#ffffff', borderRadius: '14px', padding: '12px 14px', border: '1px solid #e2e8f0' }}>
                        <span style={{ fontSize: '10.5px', fontWeight: 800, color: '#0284c7', background: '#e0f2fe', padding: '2px 8px', borderRadius: '4px' }}>层级 ① 原文稳态锚点</span>
                        <p style={{ margin: '6px 0 0', fontSize: '12px', color: '#475569' }}>“周末搬家，给小狗旺财换窝，记得买拖把。” (原文永远不变)</p>
                      </div>

                      {/* 层级 2 */}
                      <div style={{ background: '#ffffff', borderRadius: '14px', padding: '12px 14px', border: '1px solid #e2e8f0' }}>
                        <span style={{ fontSize: '10.5px', fontWeight: 800, color: '#059669', background: '#dcfce7', padding: '2px 8px', borderRadius: '4px' }}>层级 ② 核心行动待办 ({organizeTodos.filter(t => t.done).length}/{organizeTodos.length})</span>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '8px' }}>
                          {organizeTodos.map(t => (
                            <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '12px' }} onClick={() => setOrganizeTodos(prev => prev.map(x => x.id === t.id ? { ...x, done: !x.done } : x))}>
                              <div style={{ width: '16px', height: '16px', borderRadius: '4px', border: '1.5px solid #94a3b8', background: t.done ? '#10b981' : '#fff', color: '#fff', display: 'grid', placeItems: 'center', fontSize: '10px' }}>{t.done && '✓'}</div>
                              <span style={{ textDecoration: t.done ? 'line-through' : 'none', color: t.done ? '#94a3b8' : '#0f172a' }}>{t.text}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* 层级 3 */}
                      <div style={{ background: '#fffbeb', borderRadius: '14px', padding: '12px 14px', border: '1px solid #fde68a' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                          <span style={{ fontSize: '10.5px', fontWeight: 800, color: '#b45309' }}>层级 ③ AI 贴心建议 (参考项)</span>
                          {!suggestionAdopted ? (
                            <button style={{ padding: '3px 10px', borderRadius: '999px', background: '#d97706', color: '#fff', border: 'none', fontSize: '10.5px', fontWeight: 700, cursor: 'pointer' }} onClick={() => { setSuggestionAdopted(true); setShowOrganizeUndo(true); }}>
                              + 采纳进待办
                            </button>
                          ) : (
                            <span style={{ fontSize: '11px', color: '#059669', fontWeight: 700 }}>✓ 已采纳</span>
                          )}
                        </div>
                        <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#92400e' }}>💡 气象预警：周日午后降水概率 75%，建议使用防雨薄膜封包纸箱。</p>
                      </div>

                      {/* 层级 4 */}
                      <div style={{ background: '#faf5ff', borderRadius: '14px', padding: '12px 14px', border: '1px solid #e9d5ff' }}>
                        <span style={{ fontSize: '10.5px', fontWeight: 800, color: '#7c3aed' }}>层级 ④ 历史旧记忆出处召回 (可溯源)</span>
                        <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#581c87', fontWeight: 600 }}>📞 搬家李师傅：138-0013-8821（检索自 2025 年 9 月保存的便签）</p>
                      </div>

                      {showOrganizeUndo && (
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#0f172a', color: '#fff', padding: '6px 14px', borderRadius: '999px', fontSize: '11px' }}>
                          <span>✓ 建议已并入核心待办</span>
                          <span style={{ color: '#38bdf8', cursor: 'pointer', fontWeight: 750 }} onClick={() => { setSuggestionAdopted(false); setShowOrganizeUndo(false); }}>
                            撤销采纳 (Undo)
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* UX 三问卡片 */}
                  <div className="ux-three-questions-grid">
                    <div className="ux-question-card">
                      <span className="ux-q-badge why">为什么做</span>
                      <h4>结论前置，细节后退</h4>
                      <p>用户打开整理结果，绝不想看几十行 AI 思考过程。首屏必须第一时间亮出最有价值的结论与立即可办清单。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge how">方案选择</span>
                      <h4>四层严格色彩与逻辑解耦</h4>
                      <p>绿色行动待办（必须做）与黄色建议便签（参考项）在视觉与状态上坚决分流，绝不把模型猜测当作用户真实日程。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge tradeoff">优势与取舍</span>
                      <h4>写回掌权与可撤销自由</h4>
                      <p>AI 整理内容独立留存，不采纳也能享受辅助价值；采纳写回支持“追加”与“对照替换”，且提供一键 Undo 胶囊。</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* 链路 4: 信息合并（主题功能） */}
            <div className="flow-deep-dive-card">
              <div className="flow-header-row">
                <div className="flow-title-block">
                  <span className="flow-tag-pill">核心链路 04 · 知识聚合网络</span>
                  <h3>信息合并（主题功能，多笔记聚合与溯源）</h3>
                </div>
                <div style={{ fontSize: '13px', color: '#64748b' }}>
                  对应真机界面：<strong>themes.png</strong> · 真实源文件 3 倍切图导出
                </div>
              </div>

              <div className="flow-split-layout">
                {/* 真实切图展窗 */}
                <div className="figma-screen-viewport-box">
                  <div className="figma-viewport-header">
                    <span>Figma V3 设计稿 · 主题聚合与来源抽屉</span>
                    <span>1170 × 2556 px</span>
                  </div>
                  <div className="figma-image-container" onClick={() => setZoomImage({ src: '/ui/themes.png', title: '信息合并（主题功能） · themes.png', desc: '展示了跨时间跨记录的“周末搬家”主题视图，多笔记聚合指南卡片与支持点击查看的来源笔记抽屉。' })}>
                    <img src="/ui/themes.png" alt="主题功能界面" />
                    <div className="figma-zoom-badge">
                      <span>🔍 点击查看 3X 原图</span>
                    </div>
                  </div>
                </div>

                {/* 交互沙盒与 UX 剖析 */}
                <div className="flow-sandbox-and-rationale">
                  <div className="interactive-sandbox-container">
                    <div className="sandbox-header">
                      <span>🎮 可动手交互演示 · 多源笔记装订与出处溯源</span>
                      <span style={{ color: '#7c3aed' }}>点击来源可展开原始草稿证据</span>
                    </div>

                    {/* 主题装订本卡片 */}
                    <div style={{ background: '#ffffff', borderRadius: '18px', padding: '16px', border: '1.5px solid #e2e8f0', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 800, color: '#0f172a' }}>📖 主题指南：【周末搬家全攻略】</h4>
                        <span style={{ fontSize: '11px', color: '#7c3aed', background: '#f5f3ff', padding: '2px 8px', borderRadius: '999px', fontWeight: 700 }}>多对一视图</span>
                      </div>

                      <p style={{ margin: 0, fontSize: '12px', color: '#475569', lineHeight: 1.6 }}>
                        基于您过去 6 个月记录的 3 篇相关碎片（房东退租单、买拖把备忘、打包清单）自动提炼出的整体执行手册。
                      </p>

                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '4px' }}>
                        <button
                          style={{ padding: '6px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', background: selectedThemeSource === 'lease' ? '#eff6ff' : '#f8fafc', color: '#1e293b', fontSize: '11.5px', fontWeight: 650, cursor: 'pointer' }}
                          onClick={() => setSelectedThemeSource(selectedThemeSource === 'lease' ? null : 'lease')}
                        >
                          📎 溯源 1：房东退租约定
                        </button>
                        <button
                          style={{ padding: '6px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', background: selectedThemeSource === 'items' ? '#eff6ff' : '#f8fafc', color: '#1e293b', fontSize: '11.5px', fontWeight: 650, cursor: 'pointer' }}
                          onClick={() => setSelectedThemeSource(selectedThemeSource === 'items' ? null : 'items')}
                        >
                          📎 溯源 2：客厅大件打包项
                        </button>
                      </div>

                      {/* 展开的来源抽屉 */}
                      {selectedThemeSource && (
                        <div style={{ background: '#f1f5f9', padding: '12px', borderRadius: '10px', fontSize: '11.5px', color: '#334155', borderLeft: '3px solid #3b82f6' }}>
                          <strong>原始备忘录快照：</strong>
                          {selectedThemeSource === 'lease' ? (
                            <p style={{ margin: '4px 0 0' }}>“退租时需提前 3 天交钥匙，水电表拍照结清，押金在交钥匙后 24 小时内微信退还。” (记录于 2026-03-10)</p>
                          ) : (
                            <p style={{ margin: '4px 0 0' }}>“客厅的电视柜和餐桌要用气泡膜包两层，师傅说大理石台面容易磕角。” (记录于 2026-04-02)</p>
                          )}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* UX 三问卡片 */}
                  <div className="ux-three-questions-grid">
                    <div className="ux-question-card">
                      <span className="ux-q-badge why">为什么做</span>
                      <h4>打通孤岛记忆</h4>
                      <p>用户记录通常高度离散。同一件事情可能分 5 次记录在不同星期里，需要一个容器把碎片连成连贯知识体系。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge how">方案选择</span>
                      <h4>主题是视图而非搬家</h4>
                      <p>自动归纳到主题绝不物理剪切、移动或破坏原记录。记录与主题是多对一灵活映射，原笔记始终呆在时间流里。</p>
                    </div>

                    <div className="ux-question-card">
                      <span className="ux-q-badge tradeoff">优势与取舍</span>
                      <h4>杜绝幻觉，每一句都能溯源</h4>
                      <p>主题生成的结论卡片中，每一项建议都挂载原始出处链接，点击即可弹窗回溯原始文本，建立不可动摇的信任感。</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </section>

        {/* ====================================================
           SECTION 03: BEFORE VS AFTER COMPARISON
           ==================================================== */}
        <section id="evolution">
          <div className="chapter-header">
            <div className="chapter-number-pill">Chapter 03 · 深度反思与设计突破</div>
            <h2 className="chapter-title">
              第一版反思 vs 新版重设计：<br />
              从技术狂热走向克制优雅
            </h2>
            <p className="chapter-subtitle">
              作为产品经理与 UX 设计师，最有价值的能力在于能够跳出“AI 技术能做到什么”的狂热陷阱，站在真实用户的人性弱点与心理安全感上重新权衡取舍。
            </p>
          </div>

          <div className="before-after-dual-board">
            {/* 左侧：第一版教训 */}
            <div className="comparison-column-card before">
              <span className="comparison-badge red">🔴 第一版思路硬伤 (技术狂热的反思)</span>
              <h3>“以为用户想要全自动，结果制造了巨大负担”</h3>

              <div className="comparison-points-list">
                <div className="comparison-point-item">
                  <h5>教训 01 · AI 喧宾夺主写长文</h5>
                  <p>AI 自动把随手记录扩展成 500 字的小作文，充斥大量无用的客套话和空洞百科，用户连看完的耐心都没有。</p>
                </div>

                <div className="comparison-point-item">
                  <h5>教训 02 · 满屏复选框像做考卷</h5>
                  <p>整理结果一出来就弹出 10 几个选择题和分类单选，用户原本只想随手存一下，却被迫花 3 分钟做选择题。</p>
                </div>

                <div className="comparison-point-item">
                  <h5>教训 03 · 改不动也退不回</h5>
                  <p>AI 一旦写回原文就直接覆盖了用户的原始文字。用户想找回自己当初的原话却再也找不到了，产生极强的不安全感。</p>
                </div>
              </div>
            </div>

            {/* 右侧：新版突破 */}
            <div className="comparison-column-card after">
              <span className="comparison-badge green">🟢 新版克制突破 (产品思维的胜利)</span>
              <h3>“迎合人性懒到底，只给干货，用户永远掌权”</h3>

              <div className="comparison-points-list">
                <div className="comparison-point-item">
                  <h5>突破 01 · 结论前置，直接给可用清单</h5>
                  <p>第一屏用流式打字机第一时间亮出行动待办（必须做）和关键电话，删掉所有废话，3 秒钟看清下一步。</p>
                </div>

                <div className="comparison-point-item">
                  <h5>突破 02 · 原文神圣稳态，绝不篡改</h5>
                  <p>用户的原话始终固定在第一层作为对照锚点。AI 整理的产物独立存在，用户即便什么都不选，也完全不耽误查阅。</p>
                </div>

                <div className="comparison-point-item">
                  <h5>突破 03 · 随时随处的一键 Undo 自由</h5>
                  <p>采纳任何建议或修改含糊词后，界面始终悬浮常驻 Undo 撤销胶囊。让用户在任何环节都有反悔退回的底气。</p>
                </div>
              </div>
            </div>
          </div>

          {/* 三阶段推进交付物展示 */}
          <div style={{ marginTop: '40px', background: 'rgba(255,255,255,0.8)', padding: '28px', borderRadius: '28px', border: '1px solid rgba(15,23,42,0.08)' }}>
            <h4 style={{ margin: '0 0 16px', fontSize: '16px', fontWeight: 800, color: '#0f172a' }}>
              📌 设计推进演进时序（口 → 图 → 原型）
            </h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px' }}>
              <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '18px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '11px', fontWeight: 800, color: '#0284c7' }}>阶段 1 · 口 (需求与痛点)</span>
                <h5 style={{ margin: '6px 0 4px', fontSize: '13.5px', color: '#0f172a' }}>手绘思维导图原稿</h5>
                <p style={{ margin: 0, fontSize: '11.5px', color: '#64748b' }}>从生活对话中提炼“怕忘、没空整理、找不到”的底层人性，制定 12 条刚性产品设计底线。</p>
              </div>
              <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '18px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '11px', fontWeight: 800, color: '#d97706' }}>阶段 2 · 图 (架构与流转)</span>
                <h5 style={{ margin: '6px 0 4px', fontSize: '13.5px', color: '#0f172a' }}>信息架构与状态机</h5>
                <p style={{ margin: 0, fontSize: '11.5px', color: '#64748b' }}>确立【现在】与【记录】双空间，理顺随手速记、OCR识别、四层解耦与主题聚合状态流转。</p>
              </div>
              <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '18px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '11px', fontWeight: 800, color: '#059669' }}>阶段 3 · 原型 (高保真真机)</span>
                <h5 style={{ margin: '6px 0 4px', fontSize: '13.5px', color: '#0f172a' }}>APK v0.1.5 Build 7 交付</h5>
                <p style={{ margin: 0, fontSize: '11.5px', color: '#64748b' }}>完成 11 组核心交互场景真机视频对账闭环，确保高保真原型可落地、可交付、可复现。</p>
              </div>
            </div>
          </div>
        </section>

        {/* ====================================================
           SECTION 04: GOODNOTES IPAD PRO MIGRATION MODEL
           ==================================================== */}
        <section id="goodnotes">
          <div className="chapter-header">
            <div className="chapter-number-pill">Chapter 04 · 通用能力迁移</div>
            <h2 className="chapter-title">
              通用信息组织模型 →<br />
              Goodnotes iPad 数字纸笔生态迁移
            </h2>
            <p className="chapter-subtitle">
              这套“低摩擦捕获 → 上下文补齐 → 四层解耦 → 知识溯源”的体系不仅适用于便签备忘录，更能完美迁移至 Goodnotes 的手写数字纸笔场景中。
            </p>
          </div>

          <div className="ipad-stage-container">
            {/* iPad Pro 13 寸机身展示 */}
            <div className="ipad-pro-chassis">
              <div className="ipad-pro-screen">
                {/* 左侧：手写自由画布 (神圣不可侵犯) */}
                <div className="ipad-handwriting-canvas">
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: '11px', fontWeight: 800, color: '#0f172a', background: '#e2e8f0', padding: '2px 8px', borderRadius: '6px' }}>
                      Goodnotes 核心书写画布 · 笔迹 100% 神圣不可侵犯
                    </span>
                    <span style={{ fontSize: '11px', color: '#94a3b8' }}>Apple Pencil Pro 已连接 ✏️</span>
                  </div>

                  {/* 模拟手写笔记 */}
                  <div style={{ marginTop: '20px', fontFamily: 'Georgia, serif', color: '#1e293b' }}>
                    <h3 style={{ fontSize: '24px', fontWeight: 700, margin: '0 0 10px', fontStyle: 'italic' }}>
                      2026.09 用户体验与人机控制权研讨
                    </h3>
                    <p style={{ fontSize: '15px', lineHeight: 2, margin: 0 }}>
                      1. 用户的纸笔手写是具有高度空间自由度的心流思考过程。<br />
                      2. AI 绝不能直接修改手写字迹的线条、排版和几何坐标。<br />
                      3. AI 的价值在于在画布右侧边缘默默“准备好下一步所需的资料”。
                    </p>
                  </div>
                </div>

                {/* 右侧：AI 伴随式辅助便签 (懂分寸的助理) */}
                <div className="ipad-ai-companion-pane">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 750, color: '#7c3aed' }}>
                    <span>✦</span>
                    <span>AI 伴随助理 (右侧独立停靠)</span>
                  </div>

                  <div style={{ background: '#ffffff', padding: '12px', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 2px 6px rgba(0,0,0,0.03)' }}>
                    <strong style={{ fontSize: '11.5px', color: '#059669' }}>自动提取的行动项：</strong>
                    <ul style={{ margin: '6px 0 0 16px', padding: 0, fontSize: '11px', color: '#334155', lineHeight: 1.6 }}>
                      <li>将研讨会核心结论录入 Goodnotes 模板库</li>
                      <li>下周一前复盘墨水笔迹与打字混合排版</li>
                    </ul>
                  </div>

                  <div style={{ background: '#fffbeb', padding: '12px', borderRadius: '12px', border: '1px solid #fde68a' }}>
                    <strong style={{ fontSize: '11.5px', color: '#b45309' }}>关联到的历史旧笔记本：</strong>
                    <p style={{ margin: '4px 0 0', fontSize: '11px', color: '#92400e', lineHeight: 1.5 }}>
                      检索到 3 个月前《AI备忘录交互架构》手写草图，支持一键分屏对比参考。
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* 迁移优势总结 */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', width: '100%', maxWidth: '960px' }}>
              <div style={{ background: '#ffffff', padding: '18px', borderRadius: '18px', border: '1px solid rgba(15,23,42,0.06)' }}>
                <strong style={{ fontSize: '13px', color: '#0f172a' }}>1. 空间心流不被打扰</strong>
                <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#64748b' }}>Goodnotes 用户处于深度专注书写中，AI 采用边缘停靠伴随形态，杜绝弹窗打断。</p>
              </div>
              <div style={{ background: '#ffffff', padding: '18px', borderRadius: '18px', border: '1px solid rgba(15,23,42,0.06)' }}>
                <strong style={{ fontSize: '13px', color: '#0f172a' }}>2. 墨水字迹与数字化双轨</strong>
                <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#64748b' }}>手写墨水保留艺术感与温情，后台端侧识别并抽离为结构化待办，双轨并行。</p>
              </div>
              <div style={{ background: '#ffffff', padding: '18px', borderRadius: '18px', border: '1px solid rgba(15,23,42,0.06)' }}>
                <strong style={{ fontSize: '13px', color: '#0f172a' }}>3. 旧笔记语义无感连接</strong>
                <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#64748b' }}>书写某课题时，自动召回过去一年里写过的相关笔记页面，盘活整座数字笔记本库。</p>
              </div>
            </div>
          </div>
        </section>

        {/* ====================================================
           SECTION 05: RELIABILITY & TIMELINE MATRIX
           ==================================================== */}
        <section id="reliability">
          <div className="chapter-header">
            <div className="chapter-number-pill">Chapter 05 · 可靠性与体感微交互</div>
            <h2 className="chapter-title">
              严苛的工程边界：<br />
              0.8s 延迟优化与状态保护体系
            </h2>
            <p className="chapter-subtitle">
              真正的 UX 高水准不仅体现在顺畅流程中，更体现在面对弱网、接口超时、AI 返回混乱与并发编辑等极端场景下的防御性设计。
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
            <div style={{ background: '#ffffff', padding: '24px', borderRadius: '24px', border: '1px solid rgba(15,23,42,0.08)', boxShadow: '0 4px 16px rgba(0,0,0,0.03)' }}>
              <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: '#eff6ff', color: '#2563eb', display: 'grid', placeItems: 'center', fontSize: '18px', marginBottom: '12px' }}>⏱️</div>
              <h4 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 800, color: '#0f172a' }}>0.8s 体感延迟分段优化</h4>
              <p style={{ margin: 0, fontSize: '12px', color: '#64748b', lineHeight: 1.6 }}>
                T0 本地秒级入库与震动反馈 → T1 顶部灵动岛微光提示后台处理 → T2 优先吐出核心待办 → T3 按需流式加载历史长篇引用。
              </p>
            </div>

            <div style={{ background: '#ffffff', padding: '24px', borderRadius: '24px', border: '1px solid rgba(15,23,42,0.08)', boxShadow: '0 4px 16px rgba(0,0,0,0.03)' }}>
              <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: '#fef2f2', color: '#dc2626', display: 'grid', placeItems: 'center', fontSize: '18px', marginBottom: '12px' }}>🛡️</div>
              <h4 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 800, color: '#0f172a' }}>弱网与超时安全降级</h4>
              <p style={{ margin: 0, fontSize: '12px', color: '#64748b', lineHeight: 1.6 }}>
                保存绝不受 AI 接口阻塞。断网时以纯本地文本完整落盘；网络恢复后在后台静默发起重试，失败仅展示轻量重试胶囊，绝不阻断用户。
              </p>
            </div>

            <div style={{ background: '#ffffff', padding: '24px', borderRadius: '24px', border: '1px solid rgba(15,23,42,0.08)', boxShadow: '0 4px 16px rgba(0,0,0,0.03)' }}>
              <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: '#f0fdf4', color: '#16a34a', display: 'grid', placeItems: 'center', fontSize: '18px', marginBottom: '12px' }}>🔒</div>
              <h4 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 800, color: '#0f172a' }}>早到与晚到状态保护矩阵</h4>
              <p style={{ margin: 0, fontSize: '12px', color: '#64748b', lineHeight: 1.6 }}>
                若用户在 AI 整理完成前已手动修改了原正文，晚到的 AI 响应绝不自动覆盖或合并，而是作为附注气泡提醒用户按需查看，避免覆盖劳动成果。
              </p>
            </div>
          </div>
        </section>

        {/* ====================================================
           SECTION 06: EVIDENCE & FOOTER
           ==================================================== */}
        <section id="evidence" style={{ borderTop: '1px solid rgba(15,23,42,0.08)', paddingTop: '60px' }}>
          <div style={{ background: 'linear-gradient(135deg, #0f172a, #1e293b)', color: '#ffffff', borderRadius: '36px', padding: '48px', display: 'flex', flexDirection: 'column', gap: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <span style={{ fontSize: '12px', color: '#38bdf8', fontWeight: 800, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                  诚实证据边界声明 · 杜绝虚构数据
                </span>
                <h3 style={{ fontSize: '26px', fontWeight: 850, margin: '6px 0 0' }}>
                  做有扎实落地的产品，做经得起推敲的 UX 设计
                </h3>
              </div>

              <a
                href="/auto-organize"
                target="_blank"
                rel="noreferrer"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '12px 24px', borderRadius: '999px', background: '#ffffff', color: '#0f172a', fontWeight: 800, textDecoration: 'none', boxShadow: '0 10px 24px rgba(0,0,0,0.3)' }}
              >
                <span>立即上手把玩全链路极奢交互原型</span>
                <span>↗</span>
              </a>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px', borderTop: '1px solid rgba(255,255,255,0.12)', paddingTop: '24px', fontSize: '12.5px', lineHeight: 1.6, color: '#94a3b8' }}>
              <div>
                <strong style={{ color: '#ffffff', display: 'block', marginBottom: '4px' }}>真实真机实现：</strong>
                APK v0.1.5 Build 7 已通过 11 组核心场景完整自动化验证，代码已入库。
              </div>
              <div>
                <strong style={{ color: '#ffffff', display: 'block', marginBottom: '4px' }}>高保真交互设计：</strong>
                Figma V3 全套 3x 导出切图（entry/clarify/organize/themes）与交互原型全量在线。
              </div>
              <div>
                <strong style={{ color: '#ffffff', display: 'block', marginBottom: '4px' }}>概念推演边界：</strong>
                Goodnotes 迁移方案属于前瞻性产品设计概念推演，尚未在商业真机中大规模部署。
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid rgba(255,255,255,0.12)', paddingTop: '20px', fontSize: '12px', color: '#64748b' }}>
              <span>设计手记与作品集 · 刘双广 (资深 PM & UX 设计师) · 2026</span>
              <a href="#hero" style={{ color: '#94a3b8', textDecoration: 'none' }}>回到顶部 ↑</a>
            </div>
          </div>
        </section>

      </div>

      {/* 全屏高清大图查看模态框 (Image Zoom Modal) */}
      {zoomImage && (
        <div className="image-zoom-modal-overlay" onClick={() => setZoomImage(null)}>
          <div className="image-zoom-modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close-button" onClick={() => setZoomImage(null)}>✕ 关闭</button>
            <div style={{ color: '#ffffff', marginBottom: '12px', textAlign: 'center' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800 }}>{zoomImage.title}</h3>
              {zoomImage.desc && <p style={{ margin: '4px 0 0', fontSize: '12.5px', color: '#94a3b8' }}>{zoomImage.desc}</p>}
            </div>
            <img src={zoomImage.src} alt={zoomImage.title} />
          </div>
        </div>
      )}
    </div>
  );
}
