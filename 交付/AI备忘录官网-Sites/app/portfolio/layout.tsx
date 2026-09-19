import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'AI备忘录 UX 设计案例｜刘双广',
  description:
    '从用户为什么需要、AI 功能取舍、私人语境、写回权限到异常恢复，完整复盘 AI 备忘录的 UX 设计与实现审计。',
};

export default function PortfolioLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return children;
}
