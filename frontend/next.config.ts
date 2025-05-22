import type {NextConfig} from "next";

const nextConfig: NextConfig = {
    transpilePackages: ["lucide-react"], 
    webpack: (config) => {
        config.resolve.alias = {
            ...config.resolve.alias, 'lucide-react/dist/esm/icons': 'lucide-react/dist/esm/icons'
        };
        return config;
    },
    images: {
        domains: ['lh3.googleusercontent.com'],
        remotePatterns: [
            {
                protocol: 'https',
                hostname: 'lh3.googleusercontent.com',
                pathname: '/**',
            }
        ],
        unoptimized: true
    }
};

export default nextConfig;
