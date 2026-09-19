import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'AI备忘录｜更快速、更智能的记录方式',
  description: 'AI备忘录支持高效记录、AI语音讨论、内容辅助和一键整理。',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
