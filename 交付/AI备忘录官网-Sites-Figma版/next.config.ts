import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  async redirects() {
    return [
      {
        source: '/app-release.apk',
        destination: '/download',
        permanent: false,
      },
      {
        source: '/h5/app-release.apk',
        destination: '/download',
        permanent: false,
      },
    ];
  },
};

export default nextConfig;
