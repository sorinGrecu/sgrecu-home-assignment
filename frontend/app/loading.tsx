"use client"

import { LoadingSpinner } from './components/ui/LoadingSpinner'

export default function Loading() {
  return (
    <div className="flex items-center justify-center h-screen bg-zinc-900">
      <LoadingSpinner 
        size="lg" 
        showText 
        text="Loading..."
      />
    </div>
  )
} 