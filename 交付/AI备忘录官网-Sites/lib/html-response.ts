export function htmlResponse(html: string) {
  return new Response(html, {
    headers: {
      'content-type': 'text/html; charset=utf-8',
      'cache-control': 'public, max-age=300',
      'x-content-type-options': 'nosniff',
    },
  });
}

export function maintenancePage(kind: 'download' | 'share') {
  const isDownload = kind === 'download';
  const title = isDownload ? '下载服务维护中' : '分享服务维护中';
  const description = isDownload
    ? '新版安装包正在准备中，当前暂不提供 APK 下载。'
    : '动态分享服务正在恢复中，暂时无法查看分享内容。';

  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${title} - AI备忘录</title>
    <meta name="description" content="${description}" />
    <meta name="theme-color" content="#0b0b0d" />
    <meta property="og:title" content="${title} - AI备忘录" />
    <meta property="og:description" content="${description}" />
    <meta property="og:image" content="https://ainote.tabtotask.top/assets/ainote-hero.png" />
    <link rel="icon" href="/h5/www/static/favicon.ico" />
    <link rel="stylesheet" href="/site.css" />
  </head>
  <body>
    <a class="skip-link" href="#main">跳到主要内容</a>
    <header class="site-nav">
      <nav class="site-nav__inner" aria-label="主导航">
        <a class="brand" href="/home"><img src="/h5/www/static/logo.webp" alt="" width="36" height="36" /><span>AI备忘录</span></a>
        <div class="site-nav__links"><a href="/products">产品介绍</a><a href="/support">帮助中心</a><a class="site-nav__status" href="/${kind}" aria-current="page">服务维护中</a></div>
      </nav>
    </header>
    <main class="maintenance" id="main">
      <section class="maintenance__inner" aria-labelledby="maintenance-title">
        <div class="maintenance__mark"><img src="/h5/www/static/logo.webp" alt="" width="48" height="48" />AI备忘录</div>
        <h1 id="maintenance-title">${title}</h1>
        <p>${description} 官网首页、产品介绍和帮助中心仍可正常浏览。</p>
        <div class="maintenance__actions">
          <a class="button button--primary" href="/home">返回首页 <span class="arrow" aria-hidden="true">→</span></a>
          <a class="button" href="/support">帮助中心</a>
        </div>
      </section>
    </main>
  </body>
</html>`;
}
