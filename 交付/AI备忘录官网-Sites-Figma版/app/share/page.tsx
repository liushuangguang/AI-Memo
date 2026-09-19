import type { Metadata } from 'next';
import StatusPage from '../status-page';

export const metadata: Metadata = { title: '分享服务维护中 · AI备忘录' };

export default function SharePage() {
  return <StatusPage kind="share" />;
}
