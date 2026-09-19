import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'AI备忘录 · AI 产品设计案例｜刘双广',
  description: '一篇关于 AI 如何参与记录、理解、整理、关联与行动的深度产品设计案例，聚焦智能行为边界、用户控制与结果可靠性。',
  icons: { icon: '/favicon.ico' },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
