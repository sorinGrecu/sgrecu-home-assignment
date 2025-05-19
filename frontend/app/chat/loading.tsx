"use client"

import {LoadingSpinner} from '../components/ui/LoadingSpinner'

export default function ChatLoading() {
    return (<div className="flex items-center justify-center h-[calc(100vh-4rem)] bg-zinc-900">
            <LoadingSpinner
                size="lg"
                showText
                text="Loading chat interface..."
            />
        </div>)
} 