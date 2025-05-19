"use client";

import * as React from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { cn } from "@/lib/utils";

/**
 * Root component for the Sheet dialog.
 * Sheet is a dialog component that slides in from the edge of the screen.
 */
const Sheet = DialogPrimitive.Root;

/**
 * Button that triggers the sheet dialog to open when clicked.
 */
const SheetTrigger = DialogPrimitive.Trigger;

/**
 * Button that closes an open sheet dialog when clicked.
 */
const SheetClose = DialogPrimitive.Close;

/**
 * Portal component that renders sheet content into the document.body.
 */
const SheetPortal = DialogPrimitive.Portal;

const SheetOverlay = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Overlay
    className={cn(
      "fixed inset-0 z-50 bg-black/20 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",
      className
    )}
    {...props}
    ref={ref}
  />
));
SheetOverlay.displayName = DialogPrimitive.Overlay.displayName;

interface SheetContentProps
  extends React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content> {
  side?: "left" | "right" | "top" | "bottom";
}

/**
 * The main content container for the sheet dialog.
 * Slides in from the specified side with animation.
 */
const SheetContent = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Content>,
  SheetContentProps
>(({ className, children, side = "left", ...props }, ref) => (
  <SheetPortal>
    <SheetOverlay />
    <DialogPrimitive.Content
      ref={ref}
      className={cn(
        "fixed z-50 flex flex-col gap-4 bg-zinc-800 text-zinc-50 shadow-lg transition ease-in-out data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:duration-300 data-[state=open]:duration-300",
        {
          "h-full right-0 top-0 border-l data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right":
            side === "right",
          "h-full left-0 top-0 border-r data-[state=closed]:slide-out-to-left data-[state=open]:slide-in-from-left":
            side === "left",
          "bottom-0 left-0 right-0 border-t data-[state=closed]:slide-out-to-bottom data-[state=open]:slide-in-from-bottom":
            side === "bottom",
          "left-0 right-0 top-0 border-b data-[state=closed]:slide-out-to-top data-[state=open]:slide-in-from-top":
            side === "top",
        },
        className
      )}
      {...props}
    >
      {children}
    </DialogPrimitive.Content>
  </SheetPortal>
));
SheetContent.displayName = "SheetContent";

/**
 * Container for the header section of the sheet.
 */
const SheetHeader = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn("flex flex-col space-y-2 px-6 pt-6", className)}
    {...props}
  />
);
SheetHeader.displayName = "SheetHeader";

/**
 * Container for the footer section of the sheet.
 */
const SheetFooter = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn("flex flex-col-reverse gap-2 px-6 pb-6", className)}
    {...props}
  />
);
SheetFooter.displayName = "SheetFooter";

/**
 * Component for rendering the title of a sheet.
 */
const SheetTitle = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title
    ref={ref}
    className={cn("text-lg font-semibold text-zinc-50", className)}
    {...props}
  />
));
SheetTitle.displayName = DialogPrimitive.Title.displayName;

/**
 * Component for rendering a description within the sheet.
 */
const SheetDescription = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description
    ref={ref}
    className={cn("text-sm text-zinc-400", className)}
    {...props}
  />
));
SheetDescription.displayName = DialogPrimitive.Description.displayName;

export {
  Sheet,
  SheetPortal,
  SheetOverlay,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
}; 