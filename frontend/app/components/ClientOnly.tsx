"use client";

import { useState, useEffect } from "react";

interface ClientOnlyProps {
  children: React.ReactNode;
}

/**
 * Component that only renders its children on the client side
 */
export default function ClientOnly({ children }: ClientOnlyProps) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return null;
  }

  return <>{children}</>;
} 