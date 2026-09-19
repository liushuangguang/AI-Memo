import Image from 'next/image';
import { ArrowLeft, Clock3 } from 'lucide-react';

export default function StatusPage({ kind }: { kind: 'download' | 'share' }) {
  const isDownload = kind === 'download';
  return (
    <main className="service-shell">
      <header className="service-header">
        <a href="/" aria-label="返回 AI备忘录案例首页">
          <Image src="/logo.webp" alt="" width={34} height={34} />
          <strong>AI备忘录</strong>
        </a>
        <span>服务状态</span>
      </header>
      <section className="service-card" aria-labelledby="service-title">
        <Clock3 size={30} />
        <p>{isDownload ? '下载服务' : '分享服务'}</p>
        <h1 id="service-title">{isDownload ? '下载服务维护中' : '分享服务维护中'}</h1>
        <p className="service-description">
          {isDownload
            ? '新版安装包正在准备中，当前暂不提供 APK 下载。历史安装包不会从这个网站公开分发。'
            : '动态分享服务暂未恢复。当前案例网站不接入旧分享数据，也不会展示失效内容。'}
        </p>
        <a className="service-back" href="/"><ArrowLeft size={17} /> 返回产品设计案例</a>
      </section>
    </main>
  );
}
