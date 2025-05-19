"use client"

import {useParams} from 'next/navigation'
import ChatInterface from '../../components/chat/ChatInterface'
import {LoadingSpinner} from '../../components/ui/LoadingSpinner'
import {useConversation, useConversationMessages} from '@/lib/hooks/useConversations'

export default function ChatSessionPage() {
    const params = useParams()
    const sessionId = params.id as string

    const {data: conversation, isLoading: isConversationLoading} = useConversation(sessionId)
    const {data: messages, isLoading: isMessagesLoading} = useConversationMessages(sessionId)

    if (isConversationLoading || isMessagesLoading) {
        return (<div className="flex flex-col items-center justify-center h-[calc(100vh-4rem)]">
            <LoadingSpinner size="lg" showText text="Retrieving your conversation..."/>
        </div>)
    }

    return (<div className="min-h-[calc(100vh-4rem)] max-w-4xl mx-auto px-4 md:px-6">
        <ChatInterface
            sessionId={sessionId}
            initialMessages={messages}
            conversationTitle={conversation?.title}
        />
    </div>)
} 