"use client";

import React, { useState, useEffect } from 'react';
import './auto-organize.css';

// 拟真数据场景
const SCENARIOS = [
  {
    id: 'moving',
    title: '周末搬家与旧物处理',
    icon: '🚚',
    badge: '生活日常高频',
    draft: '周日准备搬家，先把客厅的大件理出来，叫个货拉拉或者找之前那个师傅李师傅电话多少来着？对了看看周日下不下雨，周六晚上提前把冰箱断电',
    todos: [
      { id: 'm1', text: '周六 20:00 前将冰箱断电并清空冷冻室', done: false },
      { id: 'm2', text: '周日 08:30 整理打包客厅大型家具', done: false },
      { id: 'm3', text: '联系搬家李师傅确认货车时间与到达定位', done: false },
      { id: 'm4', text: '准备雨天防护防潮薄膜包裹沙发与床垫', done: false }
    ],
    suggestion: '气象雷达预警：周日午后降水概率 75%，建议所有纸箱外层套大号防尘袋，并在电梯口铺设防滑纸板。',
    memoryTitle: '【历史备忘·2025-09-12】',
    memoryContent: '搬家师傅李师傅：138-0013-8821（自带液压板车，服务态度好，不乱收费）',
    topicName: '周末搬家',
    topicCount: 4,
    streamingConclusion: '已自动为你检索到 6 个月前存的【搬家李师傅电话】，检测到周日降雨概率 75%，已自动提炼 4 项硬核待办并生成防雨打包建议。',
    askQuestion: '帮我排一个周六周日的详细时间表',
    askAnswer: '📅 【搬家时间执行表】\n• 周六 19:30：厨房与冰箱食材收尾清空并拔电源\n• 周六 21:00：常用随身证件、贵重物品单独装背包\n• 周日 08:30：李师傅货车抵达楼下，大件封膜装车\n• 周日 11:30：到达新居开始卸货，核对大件数量'
  },
  {
    id: 'camera',
    title: '复古胶片机选购与避坑',
    icon: '📷',
    badge: '深度爱好探索 (参考图同款)',
    draft: '想买一台成色好的 Pentax K1000 胶片机，查查东京中古店大概多少钱，还有冲洗胶卷要注意什么，说明书去哪找',
    todos: [
      { id: 'c1', text: '对比新宿 Map Camera 与银座 Lemon 社 K1000 现货成色与免税价格', done: false },
      { id: 'c2', text: '现场检查机身测光表指针响应，以及 B 门慢速快门是否卡顿', done: false },
      { id: 'c3', text: '购买 3 卷柯达 UltraMax 400 胶卷用于首卷测机与测漏光', done: false }
    ],
    suggestion: '防误触提示：Pentax K1000 无快门锁定钮，放入随身相机包前务必将过片扳手完全收拢，防止挤压走光。',
    memoryTitle: '【出处与外部档案检索】',
    memoryContent: '已匹配到 Pentax K1000 官方英文说明书高清 PDF 与常见维修拆解指南（已加入附件列表）',
    topicName: '胶片摄影与器材',
    topicCount: 6,
    streamingConclusion: '为你找到了 Pentax K1000 官方说明书下载源与东京主流中古店比价，提炼了 3 项购机验货核心指标。',
    askQuestion: '去哪冲洗胶卷能保留原汁原味的色调？',
    askAnswer: '🎞️ 【胶卷冲印建议】\n• 负片日常冲扫：推荐富士或诺日士扫描仪，暗部通透\n• 首卷测机：建议到店冲洗，观察底片齿孔是否有齿轮划痕'
  },
  {
    id: 'camping',
    title: '秋季露营徒步装备清单',
    icon: '⛺',
    badge: '户外出行活动',
    draft: '下周六去莫干山露营，带双人帐篷和防潮垫，查查营地能不能生明火，山里晚上温度低带件厚外套',
    todos: [
      { id: 'p1', text: '检查双人帐篷铝合金地钉与防风绳是否有磨损', done: false },
      { id: 'p2', text: '周五充好 2 万毫安移动电源与高亮度营地照明灯', done: false },
      { id: 'p3', text: '准备一体式便携气炉与防风环（营地严禁地面明火）', done: false }
    ],
    suggestion: '山区夜间温差大：预报午夜最低温降至 11°C，普通棉被不耐潮，建议携带温标 5°C 的羽绒睡袋。',
    memoryTitle: '【历史备忘·2025-05-02】',
    memoryContent: '莫干山 3 号营地管家小张：微信号 mgs_camp03（可提前报备车牌直达营位）',
    topicName: '户外露营合集',
    topicCount: 3,
    streamingConclusion: '已核实营地管理规定严禁地面明火，山区夜间气温较低，已自动生成装备检查待办与防寒保暖提示。',
    askQuestion: '营地附近有没有徒步轻装路线？',
    askAnswer: '🌲 【轻装徒步推荐】\n• 营地后山竹林环线：全长 3.2 公里，爬升 120 米\n• 耗时约 1.5 小时，沿途有山泉水打卡点，适合早晨晨练'
  }
];

export default function AutoOrganizePage() {
  const [scenarioIdx, setScenarioIdx] = useState(0);
  const curScenario = SCENARIOS[scenarioIdx];

  // 全局交互状态
  const [activeStep, setActiveStep] = useState(1);
  const [autoOrganizeEnabled, setAutoOrganizeEnabled] = useState(true);
  const [todos, setTodos] = useState(curScenario.todos);
  const [isOrganizing, setIsOrganizing] = useState(false);
  const [agentStep, setAgentStep] = useState(4); // 0~4
  const [streamProgress, setStreamProgress] = useState(100);
  const [showVoiceListening, setShowVoiceListening] = useState(false);
  const [showAdoptionSheet, setShowAdoptionSheet] = useState(false);
  const [adoptMode, setAdoptMode] = useState<'append' | 'diff'>('append');
  const [selectedAdoptItems, setSelectedAdoptItems] = useState<string[]>(['todo', 'suggestion', 'memory']);
  const [isAdopted, setIsAdopted] = useState(false);
  const [showUndoCapsule, setShowUndoCapsule] = useState(false);
  const [showAskAnswer, setShowAskAnswer] = useState(false);
  const [originalDraft, setOriginalDraft] = useState(curScenario.draft);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // 场景切换时更新
  useEffect(() => {
    setOriginalDraft(curScenario.draft);
    setTodos(curScenario.todos);
    setIsAdopted(false);
    setShowUndoCapsule(false);
    setShowAskAnswer(false);
    triggerOrganizeAnimation();
  }, [scenarioIdx]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 2400);
  };

  // 触发后台多 Agent 协同整理动画
  const triggerOrganizeAnimation = () => {
    setIsOrganizing(true);
    setAgentStep(0);
    setStreamProgress(0);

    const timer1 = setTimeout(() => setAgentStep(1), 400);
    const timer2 = setTimeout(() => setAgentStep(2), 900);
    const timer3 = setTimeout(() => setAgentStep(3), 1400);
    const timer4 = setTimeout(() => {
      setAgentStep(4);
      setIsOrganizing(false);
      setStreamProgress(100);
    }, 1900);

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
      clearTimeout(timer3);
      clearTimeout(timer4);
    };
  };

  // 切换待办完成状态
  const toggleTodo = (id: string) => {
    setTodos(prev => prev.map(t => t.id === id ? { ...t, done: !t.done } : t));
    showToast('待办状态已更新');
  };

  // 采纳建议加入待办
  const addSuggestionToTodo = () => {
    const newId = 'sug_' + Date.now();
    setTodos(prev => [...prev, { id: newId, text: curScenario.suggestion, done: false }]);
    showToast('已将 AI 建议添加至核心待办');
  };

  // 采纳写回操作
  const handleAdopt = () => {
    setShowAdoptionSheet(false);
    setIsAdopted(true);
    setShowUndoCapsule(true);
    showToast(adoptMode === 'append' ? '已在原文末尾追加采纳内容' : '已按照逐段对照更新正文');
  };

  // 撤销采纳
  const handleUndo = () => {
    setIsAdopted(false);
    setShowUndoCapsule(false);
    showToast('已撤销本次采纳，原文完全恢复');
  };

  // 模拟中止整理
  const handleStopAgents = () => {
    setIsOrganizing(false);
    showToast('已停止后台整理，原始记录不受任何影响');
  };

  return (
    <div className="stage-viewport">
      {/* 3D 长虹条纹磨砂玻璃环境装饰板 (对标参考图 1/3) */}
      <div className="stage-fluted-glass-left" />
      <div className="stage-fluted-glass-right" />

      {/* 顶部控制与展厅导航 */}
      <header className="stage-header">
        <div className="stage-branding">
          <div className="stage-logo-pill">
            <div className="stage-logo-icon">✦</div>
            <span>AI备忘录 · 全新全链路高保真交互原型</span>
          </div>
          <div className="stage-title-wrap">
            <h1>AI 自动整理全链路 · 极奢展厅体验</h1>
            <p>基于 Apple Intelligence 流体幻彩微光与 3D 展台美学重构 · 功能全量保留</p>
          </div>
        </div>

        {/* 拟真数据场景切换器 */}
        <div className="stage-scenario-pills">
          {SCENARIOS.map((sc, i) => (
            <button
              key={sc.id}
              className={`scenario-pill-btn ${scenarioIdx === i ? 'active' : ''}`}
              onClick={() => {
                setScenarioIdx(i);
                showToast(`已切换至数据场景：${sc.title}`);
              }}
            >
              <span>{sc.icon}</span>
              <span>{sc.title}</span>
            </button>
          ))}
        </div>
      </header>

      {/* 展厅主体三大模块 */}
      <main className="stage-main-grid">
        {/* 左侧：全链路走查控制板与体验向导 */}
        <aside className="stage-controller-panel">
          <div className="panel-section-title">
            <span>📍 全链路状态快速直达</span>
          </div>

          <div className="flow-step-list">
            <div
              className={`flow-step-item ${activeStep === 1 ? 'active' : ''}`}
              onClick={() => { setActiveStep(1); showToast('已定位至：阶段 1 · 随手速记'); }}
            >
              <div className="flow-step-num">1</div>
              <div className="flow-step-content">
                <h4>随手速记与安全开关</h4>
                <p>支持文字/语音/截图输入，自动整理开关控制，保存本地秒级响应不被网络阻塞。</p>
              </div>
            </div>

            <div
              className={`flow-step-item ${activeStep === 2 ? 'active' : ''}`}
              onClick={() => {
                setActiveStep(2);
                triggerOrganizeAnimation();
                showToast('已定位至：阶段 2 · 多Agent协同运算');
              }}
            >
              <div className="flow-step-num">2</div>
              <div className="flow-step-content">
                <h4>后台多 Agent 协同整理</h4>
                <p>4 阶段步进状态实时可见，随时可停止，顶部打字机流式输出核心行动结论。</p>
              </div>
            </div>

            <div
              className={`flow-step-item ${activeStep === 3 ? 'active' : ''}`}
              onClick={() => { setActiveStep(3); showToast('已定位至：阶段 3 · 四层解耦呈现'); }}
            >
              <div className="flow-step-num">3</div>
              <div className="flow-step-content">
                <h4>四层结构化解耦呈现</h4>
                <p>原文稳态锚点、行动待办（必须做）、AI建议（参考项）、旧记忆出处层级分明。</p>
              </div>
            </div>

            <div
              className={`flow-step-item ${activeStep === 4 ? 'active' : ''}`}
              onClick={() => {
                setActiveStep(4);
                setShowAdoptionSheet(true);
                showToast('已定位至：阶段 4 · 逐项采纳与一键撤销');
              }}
            >
              <div className="flow-step-num">4</div>
              <div className="flow-step-content">
                <h4>写回原文掌权与撤销机制</h4>
                <p>逐项 Checkbox 点选采纳，支持追加模式与对照替换，采纳后常驻悬浮 Undo 胶囊。</p>
              </div>
            </div>

            <div
              className={`flow-step-item ${activeStep === 5 ? 'active' : ''}`}
              onClick={() => {
                setActiveStep(5);
                setShowAskAnswer(true);
                showToast('已定位至：阶段 5 · 历史记忆与延伸问答');
              }}
            >
              <div className="flow-step-num">5</div>
              <div className="flow-step-content">
                <h4>历史记忆召回与主题问答</h4>
                <p>多对一主题视图聚合，底部点击呼出智能助手，一键生成落地时间表。</p>
              </div>
            </div>
          </div>

          <div className="ux-guarantee-box">
            <h5>🛡️ PRODUCT.md 刚性原则保障</h5>
            <ul>
              <li>原文永远是稳定参照物，严禁擅自篡改</li>
              <li>保存不被 AI 阻塞，本地先存再异步整理</li>
              <li>待办与建议严格区分，采纳写回永远由用户掌权</li>
              <li>主题是视图而非搬家，不会物理剪切历史记录</li>
            </ul>
          </div>
        </aside>

        {/* 中间：3D 拟真 iPhone 16 Pro 展台 */}
        <section className="stage-phone-center">
          {/* 拟真手机机身 */}
          <div className="iphone-device-frame">
            {/* 屏幕内界面 */}
            <div className="iphone-screen">
              {/* 顶部状态栏与灵动岛 */}
              <div className="screen-statusbar">
                <span>09:41</span>
                <div className={`dynamic-island ${isOrganizing ? 'active-ai' : ''}`}>
                  <div className="island-ai-dot" />
                  <span>{isOrganizing ? 'AI 整理中 · 4 Agent' : 'AI 备忘录'}</span>
                  <span style={{ fontSize: '9px', opacity: 0.7 }}>5G</span>
                </div>
                <span>100% 🔋</span>
              </div>

              {/* 屏幕内可滚动主内容区 */}
              <div className="screen-scrollable">
                {/* 顶部问候与天气环境药丸 (对标参考图 1) */}
                <div className="screen-ambient-bar">
                  <div className="weather-pill">
                    <span className="icon">🌤️</span>
                    <span>24° · {curScenario.badge}</span>
                  </div>
                  <div className="user-avatar-pill">广</div>
                </div>

                {/* 阶段 1：随手速记主卡片 */}
                <div className="screen-capture-card">
                  <div className="capture-header">
                    <span className="capture-badge">快速记录 · 本地秒存</span>
                    <span style={{ fontSize: '11px', color: '#94a3b8' }}>今天 09:30</span>
                  </div>
                  <textarea
                    className="capture-textarea-sim"
                    value={originalDraft}
                    onChange={(e) => setOriginalDraft(e.target.value)}
                    placeholder="随手打下想法，文字、语音或截图..."
                  />
                  <div className="capture-actions-row">
                    <div className="capture-tools">
                      <button
                        className="capture-tool-btn"
                        title="语音录入"
                        onClick={() => {
                          setShowVoiceListening(!showVoiceListening);
                          showToast(showVoiceListening ? '语音拾音结束' : '正在聆听语音输入 (Listening)...');
                        }}
                      >
                        🎙️
                      </button>
                      <button
                        className="capture-tool-btn"
                        title="图片/截图导入"
                        onClick={() => showToast('已从相册选择最新微信截图')}
                      >
                        📷
                      </button>
                      <button
                        className="capture-tool-btn"
                        title="打标签"
                        onClick={() => showToast('已标记为重要任务')}
                      >
                        🏷️
                      </button>
                    </div>
                    <span style={{ fontSize: '11px', color: '#94a3b8' }}>{originalDraft.length} 字</span>
                  </div>
                </div>

                {/* 自动整理安全控制开关 (Switch) */}
                <div
                  className="auto-switch-row"
                  onClick={() => {
                    const next = !autoOrganizeEnabled;
                    setAutoOrganizeEnabled(next);
                    showToast(next ? '已开启自动整理：保存后后台异步处理，原文保持不变' : '已关闭自动整理：纯本地保存');
                  }}
                >
                  <div className="switch-label-block">
                    <h5>
                      <span>✨ 自动智能整理</span>
                      <span style={{ fontSize: '10px', color: '#10b981', fontWeight: 600 }}>[后台静默]</span>
                    </h5>
                    <p>保存后自动提取待办与匹配资料 · 原文永不篡改</p>
                  </div>
                  <div className={`pill-toggle-switch ${autoOrganizeEnabled ? 'on' : ''}`}>
                    <div className="toggle-knob" />
                  </div>
                </div>

                {/* 阶段 2：后台多 Agent 协同整理面板 */}
                <div className="agent-orchestration-card">
                  <div className="agent-card-header">
                    <div className="agent-live-badge">
                      <span style={{ animation: 'pulseGlow 1.5s infinite' }}>⚡</span>
                      <span>多 Agent 整理流水线</span>
                    </div>
                    {isOrganizing && (
                      <button className="agent-stop-btn" onClick={handleStopAgents}>
                        ⏹ 随时停止
                      </button>
                    )}
                  </div>

                  <div className="agent-steps-progress">
                    <div className={`agent-step-row ${agentStep >= 1 ? 'completed' : 'running'}`}>
                      <span>1. 意图与时间线索理解</span>
                      <span>{agentStep >= 1 ? '✓ 0.8s' : '处理中...'}</span>
                    </div>
                    <div className={`agent-step-row ${agentStep >= 2 ? 'completed' : agentStep === 1 ? 'running' : ''}`}>
                      <span>2. 核心待办项提取</span>
                      <span>{agentStep >= 2 ? '✓ 1.1s' : agentStep === 1 ? '提取中...' : '等待'}</span>
                    </div>
                    <div className={`agent-step-row ${agentStep >= 3 ? 'completed' : agentStep === 2 ? 'running' : ''}`}>
                      <span>3. 历史记忆库向量检索</span>
                      <span>{agentStep >= 3 ? '✓ 1.4s' : agentStep === 2 ? '召回中...' : '等待'}</span>
                    </div>
                    <div className={`agent-step-row ${agentStep >= 4 ? 'completed' : agentStep === 3 ? 'running' : ''}`}>
                      <span>4. 外部出处与行动信息匹配</span>
                      <span>{agentStep >= 4 ? '✓ 1.8s' : agentStep === 3 ? '匹配中...' : '等待'}</span>
                    </div>
                  </div>
                </div>

                {/* 流式核心行动结论卡 (对标参考图 2/3 幻彩光晕) */}
                <div className="streaming-conclusion-card">
                  <div className="conclusion-title-row">
                    <span style={{ color: '#8b5cf6' }}>✦</span>
                    <span>现在最值得看的信息 (结论前置)</span>
                  </div>
                  <p className="conclusion-text">
                    {streamProgress > 0 ? curScenario.streamingConclusion : '正在组织最具行动价值的结论...'}
                  </p>
                </div>

                {/* 阶段 3：四层解耦结构化呈现 */}
                {/* 第一层：原文稳态卡 */}
                <div className="tier-section-card">
                  <div className="tier-badge-label blue">
                    <span>层级 ①</span>
                    <span>原文稳态锚点 · 永远不变</span>
                  </div>
                  <p style={{ fontSize: '12px', color: '#475569', margin: 0, lineHeight: 1.6 }}>
                    “{originalDraft}”
                  </p>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                    <span style={{ fontSize: '10.5px', color: '#94a3b8' }}>支持原样留存，随时核对</span>
                  </div>
                </div>

                {/* 第二层：核心待办 (必须做) */}
                <div className="tier-section-card">
                  <div className="tier-badge-label emerald">
                    <span>层级 ②</span>
                    <span>核心待办 ({todos.filter(t => t.done).length}/{todos.length} 已办)</span>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    {todos.map(todo => (
                      <div
                        key={todo.id}
                        className={`todo-check-item ${todo.done ? 'done' : ''}`}
                        onClick={() => toggleTodo(todo.id)}
                      >
                        <div className="todo-checkbox">
                          {todo.done && '✓'}
                        </div>
                        <span className="todo-text">{todo.text}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* 第三层：AI 贴心辅助建议 (参考项) */}
                <div className="tier-section-card">
                  <div className="tier-badge-label amber">
                    <span>层级 ③</span>
                    <span>AI 贴心建议 · 仅作参考</span>
                  </div>
                  <div className="suggestion-card-box">
                    <div className="suggestion-content">
                      <span>💡 </span>
                      <span>{curScenario.suggestion}</span>
                    </div>
                    <button className="suggestion-add-btn" onClick={addSuggestionToTodo}>
                      + 采纳进待办
                    </button>
                  </div>
                </div>

                {/* 第四层：历史记忆召回与出处 (来源可溯源) */}
                <div className="tier-section-card">
                  <div className="tier-badge-label purple">
                    <span>层级 ④</span>
                    <span>关联历史记忆 · 来源可溯源</span>
                  </div>
                  <div className="memory-recalled-box">
                    <div className="memory-title-row">
                      <span>{curScenario.memoryTitle}</span>
                      <span style={{ fontSize: '10px', color: '#8b5cf6' }}>语义相似度 94%</span>
                    </div>
                    <div className="memory-desc">{curScenario.memoryContent}</div>
                    <div className="memory-btn-row">
                      <button className="memory-action-btn" onClick={() => showToast('已复制联系电话')}>
                        📋 复制信息
                      </button>
                      <button className="memory-action-btn" onClick={() => showToast('已呼叫李师傅')}>
                        📞 一键拨号
                      </button>
                    </div>
                  </div>
                </div>

                {/* 阶段 5：主题聚合卡片 */}
                <div className="tier-section-card" style={{ background: 'linear-gradient(145deg, #ffffff, #faf7f2)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{ fontSize: '15px' }}>📁</span>
                      <span style={{ fontSize: '13px', fontWeight: 700 }}>主题归集：【{curScenario.topicName}】</span>
                    </div>
                    <span style={{ fontSize: '11px', color: '#8b5cf6', fontWeight: 600 }}>
                      包含 {curScenario.topicCount} 条记录
                    </span>
                  </div>
                  <p style={{ fontSize: '11.5px', color: '#64748b', margin: '4px 0 0' }}>
                    主题是多对一的灵活映射视图，不会物理剪切或移动您的原备忘录。
                  </p>
                </div>

                {/* 智能问答生成的行动时间表 */}
                {showAskAnswer && (
                  <div className="tier-section-card" style={{ borderColor: 'rgba(56, 189, 248, 0.4)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 750, color: '#0284c7' }}>
                      <span>🗓️</span>
                      <span>已生成的行动执行时间表</span>
                    </div>
                    <pre style={{ margin: '6px 0 0', fontFamily: 'inherit', fontSize: '11.5px', lineHeight: 1.7, color: '#334155', whiteSpace: 'pre-wrap' }}>
                      {curScenario.askAnswer}
                    </pre>
                  </div>
                )}
              </div>

              {/* 屏幕常驻底部悬浮 Dock (对标参考图 2 的 Listening / 药丸底座) */}
              <div className="screen-floating-dock">
                <button
                  className="dock-left-sparkle"
                  onClick={() => {
                    setShowVoiceListening(!showVoiceListening);
                    showToast(showVoiceListening ? '已停止拾音' : '正在聆听语音 (Listening...)');
                  }}
                  title="Apple Intelligence 语音入口"
                >
                  {showVoiceListening ? '🎙️' : '✦'}
                </button>

                <div
                  className="dock-center-prompt"
                  onClick={() => {
                    setShowAskAnswer(!showAskAnswer);
                    showToast(showAskAnswer ? '收起时间表' : '已为你生成智能时间表');
                  }}
                >
                  {showVoiceListening ? (
                    <span style={{ color: '#f43f5e' }}>正在聆听您的输入...</span>
                  ) : (
                    <span>💬 {curScenario.askQuestion}</span>
                  )}
                </div>

                <button
                  className="dock-right-btn"
                  onClick={() => setShowAdoptionSheet(true)}
                >
                  采纳结果
                </button>
              </div>

              {/* 采纳后常驻一键撤销浮条 (Undo Capsule) */}
              {showUndoCapsule && (
                <div className="floating-undo-capsule">
                  <span>✓ 已应用采纳内容</span>
                  <span className="undo-action-link" onClick={handleUndo}>
                    撤销 (Undo)
                  </span>
                </div>
              )}

              {/* 采纳面板弹层 (Adoption Bottom Sheet) */}
              {showAdoptionSheet && (
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    background: 'rgba(0, 0, 0, 0.45)',
                    backdropFilter: 'blur(8px)',
                    zIndex: 100,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'flex-end'
                  }}
                  onClick={() => setShowAdoptionSheet(false)}
                >
                  <div
                    style={{
                      background: '#ffffff',
                      borderTopLeftRadius: '32px',
                      borderTopRightRadius: '32px',
                      padding: '24px 20px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '16px',
                      boxShadow: '0 -15px 40px rgba(0,0,0,0.15)'
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <h4 style={{ margin: 0, fontSize: '16px', fontWeight: 750 }}>
                        选择要采纳的整理成果
                      </h4>
                      <button
                        style={{ border: 'none', background: 'transparent', fontSize: '18px', cursor: 'pointer', color: '#94a3b8' }}
                        onClick={() => setShowAdoptionSheet(false)}
                      >
                        ✕
                      </button>
                    </div>

                    <p style={{ margin: 0, fontSize: '11.5px', color: '#64748b' }}>
                      用户拥有绝对控制权：逐项挑选需要合并的内容，支持一键撤销。
                    </p>

                    {/* 逐项点选复选框 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={selectedAdoptItems.includes('todo')}
                          onChange={(e) => {
                            if (e.target.checked) setSelectedAdoptItems(prev => [...prev, 'todo']);
                            else setSelectedAdoptItems(prev => prev.filter(i => i !== 'todo'));
                          }}
                        />
                        <span>4 项行动待办（结构化待办清单）</span>
                      </label>
                      <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={selectedAdoptItems.includes('suggestion')}
                          onChange={(e) => {
                            if (e.target.checked) setSelectedAdoptItems(prev => [...prev, 'suggestion']);
                            else setSelectedAdoptItems(prev => prev.filter(i => i !== 'suggestion'));
                          }}
                        />
                        <span>AI 贴心气象防雨建议</span>
                      </label>
                      <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={selectedAdoptItems.includes('memory')}
                          onChange={(e) => {
                            if (e.target.checked) setSelectedAdoptItems(prev => [...prev, 'memory']);
                            else setSelectedAdoptItems(prev => prev.filter(i => i !== 'memory'));
                          }}
                        />
                        <span>召回的搬家师傅联系方式与来源链接</span>
                      </label>
                    </div>

                    {/* 模式选择：追加模式 vs 对照替换模式 */}
                    <div style={{ display: 'flex', gap: '10px', marginTop: '4px' }}>
                      <button
                        style={{
                          flex: 1,
                          padding: '10px',
                          borderRadius: '14px',
                          border: adoptMode === 'append' ? '2px solid #3b82f6' : '1px solid #e2e8f0',
                          background: adoptMode === 'append' ? '#eff6ff' : '#ffffff',
                          fontSize: '12px',
                          fontWeight: 700,
                          color: adoptMode === 'append' ? '#1d4ed8' : '#64748b',
                          cursor: 'pointer'
                        }}
                        onClick={() => setAdoptMode('append')}
                      >
                        追加在原文后
                      </button>
                      <button
                        style={{
                          flex: 1,
                          padding: '10px',
                          borderRadius: '14px',
                          border: adoptMode === 'diff' ? '2px solid #3b82f6' : '1px solid #e2e8f0',
                          background: adoptMode === 'diff' ? '#eff6ff' : '#ffffff',
                          fontSize: '12px',
                          fontWeight: 700,
                          color: adoptMode === 'diff' ? '#1d4ed8' : '#64748b',
                          cursor: 'pointer'
                        }}
                        onClick={() => setAdoptMode('diff')}
                      >
                        逐段对照替换
                      </button>
                    </div>

                    {/* 确认采纳按钮 */}
                    <button
                      style={{
                        padding: '14px',
                        borderRadius: '16px',
                        border: 'none',
                        background: '#1e1e24',
                        color: 'white',
                        fontSize: '14px',
                        fontWeight: 750,
                        cursor: 'pointer',
                        boxShadow: '0 8px 20px rgba(0,0,0,0.2)'
                      }}
                      onClick={handleAdopt}
                    >
                      确认应用已选 {selectedAdoptItems.length} 项
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 3D 悬浮投影底座 */}
          <div className="stage-pedestal" />
        </section>

        {/* 右侧：交互走查决策与设计心理学解读 */}
        <aside className="stage-notes-panel">
          <div className="panel-section-title">
            <span>🧠 UX 交互考量与设计心理学</span>
          </div>

          <div className="decision-card">
            <span className="decision-badge">决策 1 · 视觉降噪</span>
            <h4>结论前置，细节后退</h4>
            <p>
              顶部通过流式打字机第一时间输出行动结论，绝不在首屏堆砌几十个复杂的工作流节点。用户不需要看 AI 如何烧脑，只需要看最终有用的结果。
            </p>
            <blockquote>“迎合人性懒到底，直接给可用答案，不增加信息负担。”</blockquote>
          </div>

          <div className="decision-card">
            <span className="decision-badge">决策 2 · 心理安全</span>
            <h4>原文稳态与反悔自由 (Undo)</h4>
            <p>
              无论 AI 如何整理，用户的原始随手草稿始终原封不动保留在第一层。采纳任何建议后，常驻悬浮 Undo 胶囊，让用户随时拥有无损撤销的绝对心理安全感。
            </p>
            <blockquote>“写回原文永远由用户掌权，杜绝技术狂热的强行覆盖。”</blockquote>
          </div>

          <div className="decision-card">
            <span className="decision-badge">决策 3 · 认知解耦</span>
            <h4>待办与建议严格区分</h4>
            <p>
              绿色为用户明确要办的事（行动项），黄色为 AI 发散的贴心建议（参考项）。在色彩与层级上坚决分流，防止用户将算法推测误当作真实日程。
            </p>
          </div>

          <div className="decision-card">
            <span className="decision-badge">决策 4 · 真实感美学</span>
            <h4>Apple Intelligence 呼吸流体光</h4>
            <p>
              底部采用如参考图所示的悬浮药丸底座与多光谱流体呼吸微光，赋予工具安静、高贵而有生命力的陪伴感，彻底消弭传统冰冷对话框的压迫感。
            </p>
          </div>
        </aside>
      </main>

      {/* 浮动 Toast 提示 */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            bottom: '30px',
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'rgba(25, 27, 34, 0.92)',
            color: 'white',
            padding: '10px 22px',
            borderRadius: '999px',
            fontSize: '13px',
            fontWeight: 650,
            boxShadow: '0 12px 30px rgba(0, 0, 0, 0.25)',
            backdropFilter: 'blur(12px)',
            zIndex: 9999,
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
        >
          <span>✦</span>
          <span>{toastMessage}</span>
        </div>
      )}
    </div>
  );
}
