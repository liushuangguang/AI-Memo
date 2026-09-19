'use client';

import { useEffect, useState } from 'react';
import Image from 'next/image';
import { ArrowDown, ArrowRight, Check, CheckCheck, FileText, ImageIcon, Mic, Pause, Play, RotateCcw, ZoomIn } from 'lucide-react';

type Zoom = (src: string, title: string) => void;

/** Motion is an enhancement: every scene is complete before observers run. */
export function useStoryMotion(paused: boolean) {
  useEffect(() => {
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    const apply = () => {
      document.documentElement.dataset.storyMotion = paused || preference.matches ? 'still' : 'play';
    };
    apply();
    preference.addEventListener('change', apply);
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        const node = entry.target as HTMLElement;
        node.dataset.inview = String(entry.isIntersecting);
        if (entry.isIntersecting) node.dataset.entered = 'true';
      });
    }, { threshold: 0.18 });
    document.querySelectorAll('[data-scene]').forEach(node => observer.observe(node));
    return () => {
      preference.removeEventListener('change', apply);
      observer.disconnect();
      delete document.documentElement.dataset.storyMotion;
    };
  }, [paused]);
}

export function MotionButton({paused, toggle}: {paused: boolean; toggle: () => void}) {
  const [systemStill, setSystemStill] = useState(false);
  useEffect(() => {
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    const sync = () => setSystemStill(preference.matches);
    sync();
    preference.addEventListener('change', sync);
    return () => preference.removeEventListener('change', sync);
  }, []);
  return <button className="motion-toggle" onClick={toggle} disabled={systemStill} aria-pressed={paused || systemStill} aria-label={systemStill ? '已遵循系统减少动态效果设置' : paused ? '播放页面动效' : '暂停页面动效'}>
    {paused && !systemStill ? <Play size={14}/> : <Pause size={14}/>}<span>{systemStill ? '静态阅读' : paused ? '播放动效' : '暂停动效'}</span>
  </button>;
}

function ThreadLine({className = ''}: {className?: string}) {
  return <svg className={`thread-line ${className}`} viewBox="0 0 220 110" fill="none" aria-hidden="true">
    <path className="ink-trail" pathLength="1" d="M8 19C89-3 204 10 175 55C155 88 108 64 132 44C165 16 211 65 191 92"/>
    <path className="ink-tip" d="m179 83 12 11 14-7"/>
  </svg>;
}

export function StoryHero({zoom}: {zoom: Zoom}) {
  return <section className="story-hero" id="top" data-scene="hero">
    <div className="story-hero-copy">
      <p className="edition"><span/>AI 备忘录 · 产品背后的故事</p>
      <h1>记下来的事，<br/>值得有<span className="title-mark">下文<svg viewBox="0 0 230 18" aria-hidden="true"><path pathLength="1" d="M3 12C75 0 138 4 225 8M19 16C99 8 165 11 213 12"/></svg></span>。</h1>
      <p className="hero-story-intro">周末要搬家，你随手记了一句话。<br/>接下来，一个 AI 备忘录能为你做些什么？</p>
      <a className="primary-link" href="#story">从这句话开始 <ArrowDown size={17}/></a>
    </div>
    <div className="hero-composition">
      <div className="hero-orbit" aria-hidden="true"/>
      <figure className="hero-real-screen">
        <button className="screen-button" onClick={() => zoom('/ui/entry.png', '生活速记助手 · V2 完整设计稿')} aria-label="放大查看首页完整设计稿">
          <Image src="/ui/entry.png" alt="AI 备忘录首页完整设计稿，记录入口与待办位于首页" width={1170} height={2532} priority/>
          <span className="zoom-hint"><ZoomIn size={14}/>完整界面</span>
        </button>
        <figcaption>V2 设计稿 · 当前实现差异见正文</figcaption>
      </figure>
      <div className="hero-paper-story">
        <div className="paper-memo">
          <span className="paper-fold" aria-hidden="true"/>
          <span className="memo-time">周四，下班路上</span>
          <p>周末搬家，<br/>给旺财换窝，<br/>记得买个拖把。</p>
          <span className="memo-sign">先记下来。</span>
        </div>
        <ThreadLine/>
        <div className="paper-followup">
          <span className="followup-icon"><CheckCheck size={20}/></span>
          <p>等到真正要用时，<br/><strong>它还能接着帮忙。</strong></p>
        </div>
        <span className="hero-scene-caption">一条记录的故事，从这里开始</span>
      </div>
    </div>
    <div className="hero-bottom"><span>从生活里的小麻烦，走进产品里的大选择。</span><span>向下，跟着故事走 <ArrowDown size={13}/></span></div>
  </section>;
}

function MovingDayDrawing({later}: {later: boolean}) {
  return <svg className={`moving-day ${later ? 'moving-day-later' : ''}`} viewBox="0 0 360 250" aria-label={later ? '搬家纸箱旁，散落的记录等待重新整理' : '搬家纸箱、盆栽和小狗，生活里随手记下的小事'}>
    <ellipse cx="185" cy="224" rx="154" ry="12" className="drawing-shadow"/>
    <g className="plant-sketch">
      <path d="M56 163c-3-37 5-68 21-91M62 134c-26-2-40-16-38-33 24 0 38 15 38 33Z"/>
      <path d="M65 112c27-1 41-13 38-32-20 1-37 14-38 32ZM74 83C61 64 62 44 74 35c16 18 15 34 0 48Z"/>
      <path className="drawing-fill" d="m37 164 8 59h45l8-59Z"/>
      <path d="M40 178h55"/>
    </g>
    <g className="box-sketch">
      <path className="drawing-fill" d="m104 134 79-29 80 28v78l-80 25-79-25Z"/>
      <path d="m104 134 79 29 80-30M183 163v73M142 120l80 29v27l-23 8v-28"/>
      <path className="box-lid" d="m104 134-17-27 77-27 19 25 17-24 79 26-16 26"/>
      <path d="m125 185 21 8m-10-13v25m-14-11 27 10"/>
    </g>
    <g className="dog-sketch">
      <path className="drawing-fill" d="M282 217c-19-4-26-21-22-42l5-29 13 8c10-4 25-1 31 10l9-14 8 38c2 20-12 31-30 31Z"/>
      <path d="m267 145-7-22 23 13m25 10 14-18 6 27M285 185l5 5 6-5m-6 5v7m-12 0c4 4 8 3 12 0 4 3 8 4 12 0M267 209l-16 9"/>
      <circle cx="278" cy="175" r="2.5" className="drawing-solid"/><circle cx="302" cy="175" r="2.5" className="drawing-solid"/>
      <path className="dog-tail" d="M319 205c24-5 26-22 15-22-9 0-10 9-3 12"/>
    </g>
    <g className="packing-lines"><path d="m125 54 4 14m-24-3 11 9m29-8-5 10"/></g>
  </svg>;
}

export function StoryOpening() {
  const [later, setLater] = useState(false);
  return <section className="opening-story section-wrap" id="story" data-scene="fragments">
    <div className="opening-story-heading"><p className="section-kicker">先回到那句“有空再整理”</p><h2>记的时候很轻松。<br/>要用的时候，又得忙一遍。</h2><p className="section-lead">约定留在聊天里，清单散在笔记里，报价存成了截图。<br/>都记下了，却还没凑成一件能往下做的事。</p></div>
    <div className="fragment-stage" data-later={later}>
      <fieldset className="day-switch" aria-label="看看记录在不同时间的用处"><button aria-pressed={!later} onClick={() => setLater(false)}>周四 · 随手记</button><ArrowRight size={15}/><button aria-pressed={later} onClick={() => setLater(true)}>周六 · 真要用</button></fieldset>
      <div className="fragment-composition">
        <div className="fragment-item fragment-one"><span><FileText size={16}/>一句备忘</span><p>{later ? '“旺财的窝，买多大的？”' : '“给旺财换个窝。”'}</p><small>{later ? '当时没写全，现在要回想。' : '脑子里清楚，就先这样写。'}</small></div>
        <div className="fragment-drawing"><MovingDayDrawing later={later}/><span>一次普通的搬家</span></div>
        <div className="fragment-item fragment-two"><span><ImageIcon size={16}/>一张截图</span><p>{later ? '“那家搬家公司的报价呢？”' : '“报价先截个图。”'}</p><small>{later ? '相册翻了一遍，还没找到。' : '等定好时间，再回头联系。'}</small></div>
        <div className="fragment-item fragment-three"><span><Mic size={16}/>一段语音</span><p>{later ? '“还有什么没准备？”' : '“拖把、纸箱，别忘了买。”'}</p><small>{later ? '听完一遍，再抄一遍清单。' : '手上正忙，说一句最方便。'}</small></div>
      </div>
      <p className="fragment-caption" aria-live="polite">{later ? '记录留下了。理解、整理、翻找，还是得再做一次。' : '点一下“周六”，看看这些记录后来遇到了什么。'}</p>
    </div>
    <p className="opening-resolution">所以，我把设计的起点放在了这里：<br/><strong>让随手记下的东西，真正接得住后来的需要。</strong></p>
  </section>;
}


const screens = [
  {src: '/ui/entry.png', title: '先把事情留下', detail: '写入优先，待办接续', height: 2532, note: '设计稿中的广场入口未沿用。'},
  {src: '/ui/clarify.png', title: '补上关键的一句', detail: '原句在眼前，点选补清楚', height: 2532, note: '候选帮助回忆，答案由用户确认。'},
  {src: '/ui/organize.png', title: '有用的部分，留下来', detail: '正文、待办、建议、参考各就各位', height: 4608, note: '长界面按原比例完整展示。'},
  {src: '/ui/themes.png', title: '让旧记录接着帮忙', detail: '同一件事的资料，重新相遇', height: 2556, note: '图中仍标即将上线；测试版已接通合并。'},
];

export function InterfaceGallery({zoom}: {zoom: Zoom}) {
  return <section className="interface-gallery" id="interfaces" data-scene="gallery"><div className="section-wrap">
    <div className="gallery-heading"><h2>这些选择，<br/>最后长成了这样的界面。</h2><p className="section-lead">一路看过来的设计理由，回到真实的界面里。<br/>每张图都完整保留，点开可以慢慢看。</p></div>
    <div className="interface-contact-sheet">{screens.map((screen, i) => <figure className={`gallery-frame gallery-frame-${i}`} key={screen.src}>
      <figcaption><span className="gallery-step">{i + 1}</span><h3>{screen.title}</h3><p>{screen.detail}</p></figcaption>
      <button onClick={() => zoom(screen.src, `${screen.title} · V2 完整设计稿`)} className="screen-button gallery-screen" aria-label={`放大${screen.title}的完整设计稿`}><Image src={screen.src} alt={`${screen.title}的完整 V2 设计稿`} width={1170} height={screen.height} loading="lazy"/><span className="gallery-zoom"><ZoomIn size={17}/></span></button>
      <p className="gallery-source-note">{screen.note}</p>
    </figure>)}</div>
    <p className="gallery-provenance">来自已核对的 Figma V2 最新设计源文件 · 3 倍导出 · 设计稿与测试版分别说明</p>
  </div></section>;
}

export function ClosingNote() {
  const [done, setDone] = useState(false);
  return <div className={`closing-note ${done ? 'closing-note-done' : ''}`} data-scene="closing">
    <span className="closing-note-date">回到开头那条记录</span><p>周末搬家，给旺财换窝，记得买个拖把。</p>
    <div className="closing-checklist">{['要做的事，清楚了', '有用的建议，留下了', '以前的资料，找回来了'].map(text => <span key={text}><Check size={14}/>{text}</span>)}</div>
    <button onClick={() => setDone(v => !v)} aria-pressed={done}>{done ? <><RotateCcw size={14}/>再看一次</> : <>给这条记录一个下文 <ArrowRight size={15}/></>}</button>
    {done && <output className="closing-receipt">可以安心去忙搬家的事了。<br/><small>故事到这里，生活继续往前。</small></output>}
  </div>;
}
