import type { Metadata } from 'next';
import StatusPage from '../status-page';

export const metadata: Metadata = { title: '下载服务维护中 · AI备忘录' };

export default function DownloadPage() {
  return <StatusPage kind="download" />;
}
