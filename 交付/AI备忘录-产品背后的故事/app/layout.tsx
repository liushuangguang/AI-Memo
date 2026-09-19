import type { Metadata } from 'next';
import './globals.css';
import './story-revision.css';
import './story-media.css';
export const metadata: Metadata = {
  metadataBase: new URL('https://ainote-behind-the-design.easy-fern-5574.chatgpt.site'),
  title: 'AI 备忘录 · 产品背后的故事',
  description: '记下来的事，值得有下文。从随手记录、信息补全到主题合并，走进 AI 备忘录的产品选择与设计手记。',
  openGraph: { title: '记下来的事，值得有下文。', description: 'AI 备忘录：一个让随手记录继续发挥价值的产品，是怎样设计出来的。', locale:'zh_CN', type:'website', images:[{url:'/art/connections.png',width:1536,height:1024,alt:'蓝色线绳连接旧记录，AI 备忘录产品背后的故事'}] },
};
export default function Layout({ children }: {children: React.ReactNode}) {return <html lang="zh-CN"><body>{children}</body></html>}
