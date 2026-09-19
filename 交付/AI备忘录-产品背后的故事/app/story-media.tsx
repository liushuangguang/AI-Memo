import Image from 'next/image';
import { ArrowDown } from 'lucide-react';
import type { ReactNode } from 'react';

export const storyArt = {
  scattered: { title:'那些先存下来、等以后再说的小事', alt:'纸雕风格的搬家纸箱、散落便笺与相片，表现记录分散在不同地方', width:1536, height:1024 },
  context: { title:'你熟悉的生活，需要你来补上这一块', alt:'纸雕小狗和仓鼠旁，一只手为便笺补上缺失的纸片，表现私人上下文需要本人确认', width:1536, height:1024 },
  connections: { title:'过去留下的线索，在今天重新相遇', alt:'蓝色线绳连接旧便笺和打开的文件夹，表现有来源的主题连接', width:1536, height:1024 },
  care: { title:'新的帮助来了，原来的记录也好好留着', alt:'纸质文件托盘中保留原始记录，新的纸片单独放置，表现草稿保护与失败恢复', width:1536, height:1024 },
};
export type StoryArtKey = keyof typeof storyArt;

export function PictureChapter({art,children,next,href,dark=false}: {art:StoryArtKey;children:ReactNode;next:string;href:string;dark?:boolean}) {
  const source=storyArt[art];
  return <div className={`picture-chapter picture-${art} ${dark?'picture-dark':''}`} data-scene={`art-${art}`}>
    <div className="picture-chapter-inner">
      <figure className="story-artwork">
        <Image src={`/art/${art}.png`} alt={source.alt} width={source.width} height={source.height} loading="lazy"/>
        <figcaption><span>{source.title}</span><span>AI 叙事配图</span></figcaption>
      </figure>
      <div className="picture-passage"><svg className="passage-ink" viewBox="0 0 100 70" aria-hidden="true"><path pathLength="1" d="M7 15c20-19 74-8 68 21-4 21-33 17-27 2 7-17 36-9 33 22m-10-8 10 10 11-8"/></svg><p>{children}</p><a href={href}>{next}<ArrowDown size={15}/></a></div>
    </div>
  </div>;
}
