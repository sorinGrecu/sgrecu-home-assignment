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
        remotePatterns: [
            {
                protocol: 'https',
                hostname: '*.googleusercontent.com',
                pathname: '/**',
            }
        ]
    }
};

export default nextConfig;
