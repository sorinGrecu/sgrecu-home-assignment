"use client"

import ChatInterface from '../components/chat/ChatInterface'

export default function ChatPage() {
    return (<div className="h-[calc(100vh-4rem)] max-w-4xl mx-auto px-4 md:px-6">
        <ChatInterface
            sessionId={undefined}
        />
    </div>)
} 