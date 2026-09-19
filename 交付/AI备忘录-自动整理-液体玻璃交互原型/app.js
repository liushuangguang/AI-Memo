(() => {
  const $ = (s, root = document) => root.querySelector(s);
  const $$ = (s, root = document) => [...root.querySelectorAll(s)];
  const RM = matchMedia('(prefers-reduced-motion:reduce)').matches;
  let activeView = 'home';
  let detailOrigin = 'home';
  let agentTimer = null;
  let streamTimer = null;
  let toastTimer = null;
  let createdThisSession = false;
  let sheetTrigger = null;

  if (window.lucide) window.lucide.createIcons({ attrs: { 'aria-hidden': 'true' } });
  $$('.view').forEach(view => {
    const active = view.classList.contains('active');
    view.inert = !active;
    view.setAttribute('aria-hidden', String(!active));
  });

  const pad = n => String(n).padStart(2, '0');
  function updateClock() {
    const d = new Date();
    $('#clock').textContent = `${d.getHours()}:${pad(d.getMinutes())}`;
    $('#todayLabel').textContent = `${d.getMonth() + 1} 月 ${d.getDate()} 日 · ${['周日','周一','周二','周三','周四','周五','周六'][d.getDay()]}`;
  }
  updateClock();
  setInterval(updateClock, 30000);

  function toast(message) {
    const el = $('#toast');
    el.textContent = message;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 1900);
  }

  function showView(name, direction = 'push') {
    const current = $(`.view[data-view="${activeView}"]`);
    const next = $(`.view[data-view="${name}"]`);
    if (!next || current === next) return;
    $$('.view').forEach(v => v.classList.remove('push-enter', 'pop-exit'));
    if (direction === 'push') {
      next.classList.add('push-enter');
      requestAnimationFrame(() => requestAnimationFrame(() => next.classList.remove('push-enter')));
    } else {
      current.classList.add('pop-exit');
      setTimeout(() => current.classList.remove('pop-exit'), RM ? 0 : 430);
    }
    current?.classList.remove('active');
    if (current) {
      current.inert = true;
      current.setAttribute('aria-hidden', 'true');
    }
    next.inert = false;
    next.setAttribute('aria-hidden', 'false');
    next.classList.add('active');
    activeView = name;
    requestAnimationFrame(() => {
      const scroller = $('.scroll', next);
      if (scroller && name !== 'detail') scroller.scrollTop = 0;
    });
  }

  function openComposer() {
    closeSheets();
    showView('composer');
    setTimeout(() => $('#noteBody')?.focus({ preventScroll: true }), RM ? 0 : 330);
  }

  function openDetail({ fresh = false, origin = 'home' } = {}) {
    detailOrigin = origin;
    showView('detail');
    $('#detailScroll').scrollTop = 0;
    if (fresh) startAgents();
    else showCompletedResult();
  }

  function resetAgentUI() {
    $('#runStrip').classList.remove('done');
    $('#runTitle').textContent = '正在理解这条记录';
    $('#runSubtitle').textContent = '先识别行动，再查找真正有用的旧信息';
    $('#resultWrap').classList.remove('ready');
    $('#streamText').textContent = '';
    $('#streamCursor').hidden = false;
    $('#agentSummary').textContent = '4 个步骤 · 正在运行';
    $('#stopAgents').hidden = false;
    const states = [
      ['running', '进行中'], ['waiting', '等待'], ['waiting', '等待'], ['waiting', '等待']
    ];
    $$('.agent-step').forEach((step, i) => {
      step.className = `agent-step ${states[i][0]}`;
      $('.agent-time', step).textContent = states[i][1];
    });
  }

  const runMessages = [
    ['正在理解这条记录', '识别到 4 个行动，2 个时间线索'],
    ['正在整理明确待办', '只提取你写过的行动，不自动扩写'],
    ['正在查找相关记忆', '找到了之前保存的搬家公司联系方式'],
    ['正在准备行动信息', '筛掉泛泛建议，保留现在能用的内容']
  ];

  function startAgents() {
    clearInterval(agentTimer);
    clearInterval(streamTimer);
    resetAgentUI();
    let step = 0;
    const nodes = $$('.agent-step');
    const advance = () => {
      if (step > 0) {
        nodes[step - 1].className = 'agent-step success';
        $('.agent-time', nodes[step - 1]).textContent = `${(0.7 + step * .3).toFixed(1)}s`;
      }
      if (step < nodes.length) {
        nodes[step].className = 'agent-step running';
        $('.agent-time', nodes[step]).textContent = '进行中';
        $('#runTitle').textContent = runMessages[step][0];
        $('#runSubtitle').textContent = runMessages[step][1];
        step += 1;
      } else {
        clearInterval(agentTimer);
        showCompletedResult();
      }
    };
    advance();
    agentTimer = setInterval(advance, RM ? 80 : 920);
  }

  function streamAnswer() {
    const text = '这条旧记录比通用搬家攻略更能帮到你。我把联系方式放在最前面，其余资料收进了补充信息。';
    const target = $('#streamText');
    clearInterval(streamTimer);
    target.textContent = '';
    let i = 0;
    if (RM) {
      target.textContent = text;
      $('#streamCursor').hidden = true;
      return;
    }
    streamTimer = setInterval(() => {
      i += 2;
      target.textContent = text.slice(0, i);
      if (i >= text.length) {
        clearInterval(streamTimer);
        $('#streamCursor').hidden = true;
      }
    }, 32);
  }

  function showCompletedResult() {
    clearInterval(agentTimer);
    const nodes = $$('.agent-step');
    nodes.forEach((node, i) => {
      node.className = 'agent-step success';
      $('.agent-time', node).textContent = `${(0.8 + i * .4).toFixed(1)}s`;
    });
    $('#agentSummary').textContent = '4 个步骤 · 已完成';
    $('#stopAgents').hidden = true;
    $('#runStrip').classList.add('done');
    $('#runTitle').textContent = '整理完成';
    $('#runSubtitle').textContent = '1 条重要旧信息 · 3 个待办 · 1 个可选建议';
    $('#resultWrap').classList.add('ready');
    streamAnswer();
  }

  function stopAgents() {
    clearInterval(agentTimer);
    clearInterval(streamTimer);
    const running = $('.agent-step.running');
    if (running) {
      running.className = 'agent-step waiting';
      $('.agent-time', running).textContent = '已停止';
    }
    $('#agentSummary').textContent = '已停止 · 已完成的结果仍然保留';
    $('#stopAgents').hidden = true;
    $('#runTitle').textContent = '整理已停止';
    $('#runSubtitle').textContent = '原记录已经保存，你可以稍后继续';
    closeSheets();
    toast('已停止整理，原记录不受影响');
  }

  function openSheet(id) {
    sheetTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    $$('.sheet.open').forEach(s => {
      s.classList.remove('open');
      s.setAttribute('aria-hidden', 'true');
    });
    const sheet = $(id);
    $('#overlay').classList.add('open');
    $('.views').inert = true;
    sheet.setAttribute('aria-hidden', 'false');
    sheet.classList.add('open');
    requestAnimationFrame(() => $('.sheet-head button', sheet)?.focus({ preventScroll: true }));
  }

  function closeSheets({ restoreFocus = true } = {}) {
    $('#overlay').classList.remove('open');
    $$('.sheet.open').forEach(s => {
      s.classList.remove('open');
      s.setAttribute('aria-hidden', 'true');
    });
    $('.views').inert = false;
    if (restoreFocus && sheetTrigger?.isConnected) sheetTrigger.focus({ preventScroll: true });
    sheetTrigger = null;
  }

  $('#openComposer').addEventListener('click', e => { if (!e.target.closest('[data-capture-shortcut]')) openComposer(); });
  $('#fabCapture').addEventListener('click', openComposer);
  $$('[data-capture]').forEach(b => b.addEventListener('click', openComposer));
  $$('[data-capture-shortcut]').forEach(b => b.addEventListener('click', e => {
    e.stopPropagation();
    openComposer();
    toast(b.dataset.captureShortcut === 'voice' ? '语音记录已就绪' : b.dataset.captureShortcut === 'camera' ? '相机已就绪' : '已选择图片入口');
  }));

  $('#noteBody').addEventListener('input', e => $('#charCount').textContent = `${e.target.value.length} 字`);
  $('#autoSwitch').addEventListener('click', e => {
    e.currentTarget.classList.toggle('on');
    const on = e.currentTarget.classList.contains('on');
    e.currentTarget.setAttribute('aria-pressed', on);
    toast(on ? '保存后会自动整理，原文保持不变' : '已关闭自动整理');
  });

  $$('[data-back="home"]').forEach(b => b.addEventListener('click', () => showView('home', 'pop')));
  $('#saveNote').addEventListener('click', () => {
    createdThisSession = true;
    $('#originalText').textContent = $('#noteBody').value.trim() || '空记录';
    openDetail({ fresh: $('#autoSwitch').classList.contains('on'), origin: 'composer' });
    if (!$('#autoSwitch').classList.contains('on')) {
      $('#runStrip').classList.add('done');
      $('#runTitle').textContent = '已保存，未自动整理';
      $('#runSubtitle').textContent = '需要时可以点右上角重新整理';
    }
  });
  $('#openExistingDetail').addEventListener('click', () => openDetail({ fresh: false, origin: 'home' }));
  $('#detailBack').addEventListener('click', () => showView(detailOrigin === 'composer' ? 'home' : detailOrigin, 'pop'));
  $('#runStrip').addEventListener('click', () => openSheet('#agentSheet'));
  $('#openApply').addEventListener('click', () => openSheet('#applySheet'));
  $('#openContext').addEventListener('click', () => openSheet('#contextSheet'));
  $('#openAsk').addEventListener('click', () => openSheet('#askSheet'));
  $('#overlay').addEventListener('click', closeSheets);
  $$('[data-close-sheet]').forEach(b => b.addEventListener('click', closeSheets));
  $('#stopAgents').addEventListener('click', stopAgents);
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && $('.sheet.open')) closeSheets();
  });

  $$('.check').forEach(b => b.addEventListener('click', () => {
    b.classList.toggle('active');
    if (b.closest('#applySheet')) {
      const count = $$('.choice .check.active', $('#applySheet')).length;
      $('#applySelected span').textContent = `应用已选 ${count} 项`;
    }
  }));
  $$('.mode').forEach(b => b.addEventListener('click', () => {
    $$('.mode').forEach(x => x.classList.toggle('active', x === b));
    toast(b.dataset.mode === 'append' ? '会保留原文，在后面追加内容' : '应用前会先显示逐段对照');
  }));
  $('#applySelected').addEventListener('click', () => {
    const count = $$('.choice .check.active', $('#applySheet')).length;
    closeSheets();
    $('#openApply span').textContent = '已采纳 · 点击撤销';
    $('#openApply').dataset.applied = 'true';
    toast(`已应用 ${count} 项，原文仍然保留`);
  });
  $('#openApply').addEventListener('click', e => {
    if (e.currentTarget.dataset.applied === 'true') {
      e.stopImmediatePropagation();
      e.currentTarget.dataset.applied = '';
      $('#openApply span').textContent = '采纳整理结果';
      toast('已撤销本次采纳');
    }
  }, true);

  $('#addWeatherTask').addEventListener('click', e => {
    e.currentTarget.textContent = '已加入';
    e.currentTarget.disabled = true;
    toast('已加入待办，可随时撤销');
  });
  $$('[data-context-tab]').forEach(b => b.addEventListener('click', () => {
    $$('[data-context-tab]').forEach(x => x.classList.toggle('active', x === b));
    $$('[data-context-pane]').forEach(p => p.classList.toggle('active', p.dataset.contextPane === b.dataset.contextTab));
  }));
  $('#mergeTheme').addEventListener('click', () => { closeSheets(); toast('已加入“周末搬家”主题，源记录保持不变'); });
  $('#simulateAsk').addEventListener('click', () => { closeSheets(); toast('时间表已生成，并保存在这条记录旁'); });
  $('#editOriginal').addEventListener('click', () => { showView('composer', 'push'); $('#noteBody').value = $('#originalText').textContent; });
  $('#redoOrganize').addEventListener('click', () => {
    $('#loadingScreen').classList.add('show');
    setTimeout(() => {
      $('#loadingScreen').classList.remove('show');
      startAgents();
      toast('只更新未采纳的结果');
    }, RM ? 80 : 1450);
  });

  $$('[data-tab]').forEach(b => b.addEventListener('click', () => {
    const tab = b.dataset.tab;
    if (tab === 'home') showView('home', 'pop');
    else if (tab === 'topics') showView('topics');
    else if (tab === 'records') toast(createdThisSession ? '刚保存的记录排在第一条' : '记录库原型将在下一链路展开');
    else toast('个人偏好与自动整理设置');
  }));

  document.addEventListener('click', e => {
    const trigger = e.target.closest('[data-toast]');
    if (trigger) toast(trigger.dataset.toast);
  });
})();
