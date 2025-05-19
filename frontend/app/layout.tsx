import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Navbar } from "./components/Navbar";
import { Sidebar } from "./components/Sidebar";
import { Providers } from "./providers";
import ClientOnly from "./components/ClientOnly";
import { AuthAwareContent } from "./components/AuthAwareContent";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Home Assignment",
  description: "A modern LLM chat application built for JetBrains interview home assignment",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full bg-zinc-900">
      <body
        className={`${geistSans.variable} ${geistMono.variable} font-sans antialiased h-full overflow-hidden bg-zinc-900`}
      >
        <Providers>
          <ClientOnly>
            <div className="flex flex-col h-full">
              <Navbar />
              <Sidebar />
              <div className="flex-1 w-full pt-16 overflow-auto scrollbar-on-hover">
                <AuthAwareContent>
                  {children}
                </AuthAwareContent>
              </div>
            </div>
          </ClientOnly>
        </Providers>
      </body>
    </html>
  );
}
