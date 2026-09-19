'use client';
/* oxlint-disable jsx-a11y/prefer-tag-over-role -- Inline SVG needs image semantics; an img element cannot contain state-driven paths. */

import { useEffect, useRef, useState, type ReactNode } from 'react';
import { ArrowRight, RotateCcw } from 'lucide-react';

function Mechanism({name, label, children, caption}: {name:string;label:string;children:ReactNode;caption:string}) {
  const root = useRef<HTMLElement>(null);
  useEffect(() => {
    const element = root.current;
    if (!element) return;
    const observer = new IntersectionObserver(([entry]) => {
      element.dataset.inview = String(entry.isIntersecting);
    }, {threshold:.15});
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  return <figure className={`mechanism mechanism-${name}`} ref={root}>
    <div className="mechanism-label"><span>{label}</span><span>机制动画</span></div>
    {children}<figcaption aria-live="polite">{caption}</figcaption>
  </figure>;
}

function NoteGlyph({x,y,selected=false}: {x:number;y:number;selected?:boolean}) {
  return <g transform={`translate(${x} ${y})`} className={selected?'note-glyph is-lit':'note-glyph'}><path className="svg-paper" d="M0 0h32l12 12v42H0Z"/><path d="M32 0v12h12M10 24h22M10 32h22M10 40h14"/></g>;
}

export function CaptureMechanism() {
  const [step,setStep] = useState(0);
  return <Mechanism name="capture" label="从截图到记录" caption={['先选图，等待本人确认。','确认后，才开始识别和处理。','生成一份记录；原图的处理过程有迹可循。'][step]}>
    <svg viewBox="0 0 360 156" role="img" aria-label={`截图流程第 ${step+1} 步`} data-stage={step}>
      <path className="svg-track" d="M84 72h67M211 72h75"/>
      <path className={`svg-flow ${step>0?'flow-on':''}`} pathLength="1" d="M84 72h67"/>
      <path className={`svg-flow ${step>1?'flow-on':''}`} pathLength="1" d="M211 72h75"/>
      <g className="picture-glyph"><rect className="svg-paper" x="24" y="32" width="60" height="80" rx="5"/><path d="m32 91 15-20 13 13 9-11 7 18ZM35 46h36"/><circle cx="68" cy="60" r="5"/>
        <path className={step===1?'scan-beam':''} d="M20 59h68" visibility={step===1?'visible':'hidden'}/>
      </g>
      <g className={`confirm-glyph ${step>0?'is-lit':''}`}><circle className="svg-paper" cx="181" cy="72" r="30"/>{step===0?<><path d="M169 72h24M181 60v24"/><circle className="wait-ring" cx="181" cy="72" r="37"/></>:<path className="svg-check" pathLength="1" d="m167 73 10 9 20-22"/>}</g>
      <NoteGlyph x={286} y={45} selected={step===2}/>
      <text x="54" y="145" textAnchor="middle">选中截图</text><text x="181" y="145" textAnchor="middle">本人确认</text><text x="308" y="145" textAnchor="middle">生成记录</text>
    </svg>
    <div className="mechanism-controls"><span>预设演示 · 不上传图片</span><button onClick={()=>setStep(s=>(s+1)%3)}>{step===2?<><RotateCcw size={13}/>再看一次</>:<>{step===0?'确认示例图':'模拟生成'}<ArrowRight size={13}/></>}</button></div>
  </Mechanism>;
}

export function ClarifyMechanism({selected,saved}: {selected:string;saved:boolean}) {
  const index=selected==='小狗'?0:selected==='仓鼠'?1:selected==='manual'?2:-1;
  const preserved=selected==='preserve';
  return <Mechanism name="clarify" label="先选方向，再往下走" caption={preserved?'保留原词，这次不补充私人事实。':saved?'经过你的确认，这个补充才可以作为依据。':index>=0?'方向已选好，还需要你确认。':'同一个名字，可能指向完全不同的生活。'}>
    <svg viewBox="0 0 360 188" role="img" aria-label={preserved?'保留原词，不补全':`旺财的身份${saved?'已确认':'等待确认'}`} key={`${selected}-${saved}`}>
      <rect className="svg-paper" x="9" y="65" width="75" height="56" rx="8"/><text x="46" y="99" textAnchor="middle" className="svg-title">旺财</text>
      {[36,94,152].map((y,i)=><g key={y}><path className="svg-track" d={`M84 93C132 93 120 ${y} 157 ${y}`}/><path pathLength="1" className={`svg-flow ${index===i?'flow-on':''}`} d={`M84 93C132 93 120 ${y} 157 ${y}`}/><g className={index===i?'is-lit':''}><rect className="svg-paper" x="157" y={y-22} width="143" height="44" rx="7"/><text x="228" y={y+6} textAnchor="middle">{['家里的小狗','家里的仓鼠','我来补充'][i]}</text></g></g>)}
      {index>=0&&<g className="context-confirm" transform={`translate(329 ${[36,94,152][index]})`}><circle className="svg-paper" r="17"/>{saved?<path className="svg-check" pathLength="1" d="m-8 0 6 6 11-13"/>:<circle className="wait-ring" r="5"/>}</g>}
      {preserved&&<path className="preserve-loop svg-flow flow-on" pathLength="1" d="M46 121c0 50 74 53 74 16"/>}
    </svg>
  </Mechanism>;
}

export function OrganizeMechanism({count,saved}: {count:number;saved:boolean}) {
  return <Mechanism name="organize" label="一句话，分成四种用途" caption={saved?`演示中已确认 ${count} 条建议，原始记录继续保留。`:'正文承接事实，待办承接行动，建议等你采纳，参考保留来源。'}>
    <svg viewBox="0 0 360 234" role="img" aria-label={`原始记录、待办、建议、参考分层，当前选择 ${count} 条建议`} key={`${count}-${saved}`}>
      <path className="svg-track" d="M64 114h35M99 38v156M99 38h36M99 90h36M99 142h36M99 194h36"/>
      <path className="svg-flow flow-on distribution-path" pathLength="1" d="M64 114h35M99 38v156M99 38h36M99 90h36M99 142h36M99 194h36"/>
      <NoteGlyph x={20} y={86} selected/>
      <text x="43" y="171" textAnchor="middle">随手记</text>
      {['原始记录','待办事项','AI 建议','参考出处'].map((label,i)=><g key={label} className={`sort-layer sort-layer-${i} ${i===2&&saved?'is-lit':''}`}><rect className="svg-paper" x="135" y={18+i*52} width="216" height="40" rx="6"/><text x="149" y={44+i*52}>{label}</text><text className="svg-detail" x="337" y={44+i*52} textAnchor="end">{['留原意','逐项做',`${saved?'已留':'已选'} ${count} 条`,'可核对'][i]}</text></g>)}
    </svg>
  </Mechanism>;
}

export function MemoryMechanism({chosen,merged,source}: {chosen:number[];merged:boolean;source:number|null}) {
  return <Mechanism name="memory" label="连接起来，也留得住出处" caption={source?`正在回看第 ${source} 条来源。`:merged?'生成了新记录，三条原记录仍各自在原处。':'勾选下方来源，看看哪些记录会进入这次主题。'}>
    <svg viewBox="0 0 360 190" role="img" aria-label={`${chosen.length} 条来源已选，${merged?'已合并，原记录保留':'尚未合并'}`} key={`${chosen.join('-')}-${merged}-${source}`}>
      {['这次的备忘','上次的清单','存过的报价'].map((label,i)=>{const y=32+i*62,active=chosen.includes(i+1);return <g key={label}><path className="svg-track" d={`M130 ${y}C159 ${y} 146 94 178 94`}/>{active&&<path className={`svg-flow flow-on ${source===i+1?'return-source':''}`} pathLength="1" d={`M130 ${y}C159 ${y} 146 94 178 94`}/>}<g className={active?'is-lit':''}><rect className="svg-paper" x="6" y={y-22} width="124" height="44" rx="6"/><text x="68" y={y+6} textAnchor="middle">{label}</text></g></g>})}
      <circle className="svg-paper" cx="196" cy="94" r="18"/><path className={merged?'svg-check':'merge-cross'} pathLength="1" d={merged?'m187 94 6 7 12-14':'M187 94h18M196 85v18'}/>
      <path className="svg-track" d="M214 94h27"/>{merged&&<path className="svg-flow flow-on" pathLength="1" d="M214 94h27"/>}
      <g className={merged?'is-lit':''}><rect className="svg-paper" x="241" y="49" width="113" height="90" rx="8"/><text x="297" y="85" className="svg-title" textAnchor="middle">搬家准备</text><text x="297" y="115" textAnchor="middle" className="svg-detail">{merged?`${chosen.length} 条来源`:'等待确认'}</text></g>
    </svg>
  </Mechanism>;
}

export function ReliabilityMechanism() {
  const [mode,setMode] = useState<'late'|'retry'>('late');
  const [recovered,setRecovered] = useState(false);
  return <Mechanism name="reliability" label="失误发生之后，路还在" caption={mode==='late'?'新结果与编辑中的草稿分开，迟到也不会覆盖刚写的字。':recovered?'示意中的处理已恢复，同一份记录保留。':'这次处理失败了，原记录还在，待处理内容可以重试。'}>
    <fieldset className="reliability-switch" aria-label="选择一个失败场景"><button aria-pressed={mode==='late'} onClick={()=>{setMode('late');setRecovered(false)}}>结果晚到</button><button aria-pressed={mode==='retry'} onClick={()=>{setMode('retry');setRecovered(false)}}>处理失败</button></fieldset>
    <svg viewBox="0 0 360 196" role="img" aria-label={mode==='late'?'晚到的结果不覆盖编辑中的草稿':recovered?'示例处理已恢复':'失败的处理可以重试'} key={`${mode}-${recovered}`}>
      <g className="protected-note"><rect className="svg-paper" x="9" y="91" width="142" height="75" rx="7"/><text x="80" y="122" textAnchor="middle">{mode==='late'?'正在写的草稿':'原记录'}</text><text className="svg-detail" x="80" y="147" textAnchor="middle">始终保留</text><path className="svg-check" pathLength="1" d="m24 68 8 8 16-18"/></g>
      <g className="incoming-result"><rect className="svg-paper" x="214" y="19" width="136" height="62" rx="7"/><text x="282" y="45" textAnchor="middle">{mode==='late'?'晚到的结果':recovered?'恢复后的结果':'处理未成功'}</text><text x="282" y="69" textAnchor="middle" className="svg-detail">{mode==='late'?'单独放好':recovered?'可以继续':'等待重试'}</text></g>
      {mode==='late'?<><path className="svg-track" d="M214 50h-36q-16 0-16 16v42"/><path className="boundary-stop" d="M174 108h-24M174 115h-24"/><path className="svg-flow flow-on" pathLength="1" d="M282 82v59h-63"/><text x="269" y="177" textAnchor="middle" className="svg-detail">确认后再使用</text></>:<><path className="svg-flow flow-on retry-loop" pathLength="1" d="M233 101c-58 12-46 73 17 63 47-7 57-54 18-58"/><path d="m276 96-11 10 12 9"/>{recovered&&<path className="svg-check" pathLength="1" d="m273 128 9 10 22-24"/>}</>}
    </svg>
    {mode==='retry'&&<div className="mechanism-controls"><span>预设恢复演示</span><button onClick={()=>setRecovered(v=>!v)}><RotateCcw size={13}/>{recovered?'重置场景':'模拟重试'}</button></div>}
  </Mechanism>;
}
