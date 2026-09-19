import type { Metadata } from 'next';
import Image from 'next/image';
import { ArrowLeft, FileText, Link2 } from 'lucide-react';

export const metadata: Metadata = { title: '服务说明 · AI备忘录' };

export default function SupportPage() {
  return (
    <main className="service-shell">
      <header className="service-header">
        <a href="/" aria-label="返回 AI备忘录案例首页">
          <Image src="/logo.webp" alt="" width={34} height={34} />
          <strong>AI备忘录</strong>
        </a>
        <span>服务说明</span>
      </header>
      <section className="support-card" aria-labelledby="support-title">
        <div className="support-intro">
          <p>当前恢复范围</p>
          <h1 id="support-title">案例网站已经恢复，产品服务仍在整理。</h1>
          <p>这个站点用于展示 AI备忘录的产品设计与 AI UX 方法，不接入旧数据库、动态分享或安装包分发。</p>
        </div>
        <div className="support-links">
          <a href="/download"><FileText size={20} /><span><strong>下载状态</strong><small>当前不提供 APK</small></span></a>
          <a href="/share"><Link2 size={20} /><span><strong>分享状态</strong><small>动态服务维护中</small></span></a>
        </div>
        <a className="service-back" href="/"><ArrowLeft size={17} /> 返回产品设计案例</a>
      </section>
    </main>
  );
}
