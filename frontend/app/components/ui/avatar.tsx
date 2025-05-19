import * as React from "react";
import { cn } from "@/lib/utils";
import Image from "next/image";

export interface AvatarProps extends Omit<React.ComponentProps<typeof Image>, 'src' | 'alt'> {
  src: string;
  alt: string;
}

export function Avatar({ src, alt, className, ...props }: AvatarProps) {
  return (
    <Image
      src={src}
      alt={alt}
      width={32}
      height={32}
      className={cn("rounded-full", className)}
      {...props}
    />
  );
} 