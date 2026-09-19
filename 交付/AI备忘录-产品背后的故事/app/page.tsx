'use client';
import { ArrowDown, ArrowRight, X, Menu, ZoomIn } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import Image from 'next/image';
import { Journey, Entry, Capture, Clarify, Organize, Memory, Beyond, Ending } from './story-sections';
import { details } from './story-data';
import { StoryHero, StoryOpening, InterfaceGallery, MotionButton, useStoryMotion } from './story-experience';
import { PictureChapter } from './story-media';

export default function Story(){
  const [detail,setDetail]=useState<string|null>(null);
  const [zoom,setZoom]=useState<{src:string;title:string}|null>(null);
  const [menu,setMenu]=useState(false);
  const [active,setActive]=useState('story');
  const [paused,setPaused]=useState(false);
  useStoryMotion(paused);
  useEffect(()=>{const observer=new IntersectionObserver(entries=>{for(const e of entries)if(e.isIntersecting)setActive(e.target.id)},{rootMargin:'-15% 0px -55% 0px'});document.querySelectorAll('section[id]').forEach(e=>observer.observe(e));return()=>observer.disconnect()},[]);
  const current=detail?details[detail]:null;
  const showZoom=(src:string,title:string)=>setZoom({src,title});
  return <><a className="skip" href="#story">跳到正文</a><header className="site-header"><a className="brand" href="#top"><Image src="/logo.webp" width="30" height="30" alt=""/>AI 备忘录<span className="brand-separator"/><span className="brand-sub">设计手记</span></a><nav aria-label="章节导航"><a href="#story">产品故事</a><a href="#choices">设计选择</a><a href="#beyond">屏幕之外</a></nav><div className="header-actions"><MotionButton paused={paused} toggle={()=>setPaused(v=>!v)}/><a className="header-link" href="#story">开始阅读<ArrowDown size={15}/></a></div><div className="reading-progress" aria-hidden="true"/></header>
  <main>
    <StoryHero zoom={showZoom}/>
    <StoryOpening/>
    <Journey active={active}/>
    <Entry open={setDetail} zoom={showZoom}/>
    <Capture open={setDetail}/>
    <PictureChapter art="scattered" href="#clarify" next="接下来，让 AI 听懂">一句话留下来了。<br/><strong>可它明白的，和你想的是同一件事吗？</strong></PictureChapter>
    <Clarify open={setDetail}/>
    <PictureChapter art="context" href="#organize" next="看看怎样把帮助留下" dark>补上“旺财是家里的小狗”，方向清楚了。<br/><strong>接下来，要让建议变成真正有用的下一步。</strong></PictureChapter>
    <Organize open={setDetail} zoom={showZoom}/>
    <PictureChapter art="connections" href="#memory" next="把以前的记录也接过来">眼前的事情有着落了。<br/><strong>可那家以前存过的搬家公司，又在哪里？</strong></PictureChapter>
    <Memory open={setDetail}/>
    <InterfaceGallery zoom={showZoom}/>
    <PictureChapter art="care" href="#beyond" next="走到屏幕背后">一条记录，已经能接着帮忙。<br/><strong>怎样让这份帮助，值得长期信赖？</strong></PictureChapter>
    <Beyond open={setDetail}/>
    <Ending open={setDetail}/>  </main><footer className="site-footer"><a className="brand" href="#top">AI 备忘录<span className="brand-sub">产品背后的故事</span></a><span>设计手记 · 材料核对于 2026.09.06</span><a href="#top">回到开头 ↑</a></footer>
  <button className="mobile-contents" onClick={()=>setMenu(true)} aria-label="打开阅读目录"><Menu size={17}/><span>目录</span></button>
  <Dialog open={menu} onOpenChange={setMenu}><DialogContent className="contents-dialog" showCloseButton={false}><div className="dialog-heading"><DialogTitle>从你感兴趣的地方读起</DialogTitle><button className="close-button" onClick={()=>setMenu(false)} aria-label="关闭阅读目录"><X size={22}/></button></div><DialogDescription>一条记录背后的产品选择</DialogDescription><nav aria-label="完整阅读目录">{[['story','故事的起点'],['choices','首页与记录入口'],['clarify','AI 为什么要补问'],['organize','为什么结果要分层'],['memory','主题如何连接记录'],['beyond','可靠性、商业化与验证']].map(([id,label])=><a key={id} href={`#${id}`} aria-current={active===id?'location':undefined} onClick={()=>setMenu(false)}>{label}<ArrowRight size={16}/></a>)}</nav></DialogContent></Dialog>
  <Dialog open={!!current} onOpenChange={v=>{if(!v)setDetail(null)}}><DialogContent className="detail-dialog" showCloseButton={false}>{current&&<><div className="dialog-heading"><span>设计选择 · 深入一层</span><button className="close-button" onClick={()=>setDetail(null)} aria-label="关闭设计详情"><X size={22}/></button></div><DialogTitle className="detail-title">{current.title}</DialogTitle><DialogDescription className="detail-intro">{current.intro}</DialogDescription><div className="detail-content">{current.sections.map(s=><section key={s.title}><h3>{s.title}</h3><p>{s.text}</p></section>)}{current.image&&<figure className="detail-image"><button onClick={()=>showZoom(current.image!,current.imageAlt!)} className="screen-button"><Image src={current.image} alt={current.imageAlt || current.title} width={1170} height={current.image.includes("organize")?4608:current.image.includes("themes")?2556:2532} loading="lazy"/><span className="zoom-hint"><ZoomIn size={15}/>查看全图</span></button><figcaption>{current.imageAlt} · 源文件 3 倍导出</figcaption></figure>}<aside className="evidence-note"><strong>依据与边界</strong><p>{current.evidence}</p>{detail==='sources'&&<a href="https://www.figma.com/design/krVwzIcFrg0LsRwqiQmy8e/AI备忘录?node-id=0-1" target="_blank" rel="noreferrer">查看 Figma 设计源文件 ↗</a>}</aside></div></>}</DialogContent></Dialog>
  <Dialog open={!!zoom} onOpenChange={v=>{if(!v)setZoom(null)}}><DialogContent className="image-dialog" showCloseButton={false}>{zoom&&<><div className="dialog-heading"><DialogTitle>{zoom.title}</DialogTitle><button className="close-button" onClick={()=>setZoom(null)} aria-label="关闭图片"><X size={22}/></button></div><DialogDescription>完整图片按原比例展示；点击原图可放大阅读。设计稿与当前测试版的差异见章节说明。</DialogDescription><div className="image-scroll"><a href={zoom.src} target="_blank" rel="noreferrer" aria-label="在新页面打开高清原图"><Image src={zoom.src} alt={zoom.title} width={1170} height={zoom.src.includes("organize")?4608:zoom.src.includes("themes")?2556:2532}/></a></div></>}</DialogContent></Dialog>
  </>
}
