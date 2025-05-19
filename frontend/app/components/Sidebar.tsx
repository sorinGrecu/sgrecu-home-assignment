"use client";

import React from "react";
import Link from "next/link";
import {usePathname, useRouter} from "next/navigation";
import {MessageSquare, PlusCircle} from "lucide-react";
import {cn} from "@/lib/utils";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "./ui/tooltip";
import {useSidebarEffects, useUI} from "@/lib/store";
import {useAuth} from "@/lib/hooks/useAuth";
import {useConversations} from "@/lib/hooks/useConversations";
import {LoadingSpinner} from "./ui/LoadingSpinner";

interface Conversation {
    id: string;
    title?: string;
    lastMessage?: {
        content?: string;
    };
}

/**
 * Sidebar navigation for authenticated users with conversation history
 */
export function Sidebar() {
    const pathname = usePathname();
    const router = useRouter();
    const {isSidebarOpen: isOpen, isSidebarCollapsed: isCollapsed, setIsSidebarOpen: setIsOpen} = useUI();
    const {isAuthenticated} = useAuth();
    const {data: conversations = [], isLoading, error} = useConversations();

    useSidebarEffects(isAuthenticated);

    if (!isAuthenticated) return null;

    const handleNewChat = () => {
        router.push('/chat');
        setIsOpen(false);
    };

    const getConversationPreview = (conversation: Conversation): string => {
        if (conversation.lastMessage?.content) {
            const preview = conversation.lastMessage.content;
            return preview.length > 30 ? preview.substring(0, 27) + '...' : preview;
        }
        return conversation.title || "New conversation";
    };

    const renderConversationItem = (conversation: Conversation) => {
        if (isCollapsed) {
            return (<Tooltip>
                    <TooltipTrigger asChild>
                        <Link
                            href={`/chat/${conversation.id}`}
                            className={cn("flex justify-center items-center rounded-md p-2 h-10 w-10 mx-auto transition-colors", pathname === `/chat/${conversation.id}` ? "bg-zinc-800/80 text-zinc-50" : "text-zinc-400 hover:bg-zinc-800/50 hover:text-zinc-50")}
                        >
                            <MessageSquare className="h-5 w-5"/>
                        </Link>
                    </TooltipTrigger>
                    <TooltipContent side="right" className="bg-zinc-900/90 text-zinc-50 border-zinc-700/60">
                        {getConversationPreview(conversation)}
                    </TooltipContent>
                </Tooltip>);
        }

        return (<Link
                href={`/chat/${conversation.id}`}
                className={cn("flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors", pathname === `/chat/${conversation.id}` ? "bg-zinc-800/80 text-zinc-50" : "text-zinc-400 hover:bg-zinc-800/50 hover:text-zinc-50")}
                onClick={() => setIsOpen(false)}
            >
                <MessageSquare className="h-5 w-5 flex-shrink-0"/>
                <span className="truncate">{getConversationPreview(conversation)}</span>
            </Link>);
    };

    const renderNewChatButton = () => {
        if (isCollapsed) {
            return (<Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            onClick={handleNewChat}
                            className="flex justify-center items-center h-10 w-10 mx-auto text-zinc-400 hover:text-zinc-50"
                        >
                            <PlusCircle className="h-5 w-5"/>
                        </button>
                    </TooltipTrigger>
                    <TooltipContent side="right" className="bg-zinc-900/90 text-zinc-50 border-zinc-700/60">
                        New Chat
                    </TooltipContent>
                </Tooltip>);
        }

        return (<button
                onClick={handleNewChat}
                className="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-zinc-400 hover:bg-zinc-800/50 hover:text-zinc-50"
            >
                <PlusCircle className="h-5 w-5"/>
                <span>New Chat</span>
            </button>);
    };

    const renderConversationsList = () => {
        if (isLoading) {
            return (<div className="flex justify-center py-4">
                    <LoadingSpinner size="sm"/>
                </div>);
        }

        if (error) {
            if (isCollapsed) {
                return null;
            }
            return (<div className="text-zinc-400 text-sm text-center py-4">
                    Failed to load conversations
                </div>);
        }

        if (conversations.length === 0) {
            if (!isCollapsed) {
                return (<div className="text-zinc-400 text-sm text-center py-4">
                        No conversations yet
                    </div>);
            }
            return null;
        }

        return (<ul className="space-y-2">
                {conversations.map((conversation) => (<li key={conversation.id}>
                        {renderConversationItem(conversation)}
                    </li>))}
            </ul>);
    };

    return (<>
            <aside
                className={cn("fixed top-16 left-0 h-[calc(100vh-4rem)] bg-zinc-900/60 backdrop-blur-sm z-10 transition-all duration-300 ease-in-out", isCollapsed ? "w-[70px]" : "w-64", {
                    "-translate-x-full": !isOpen,
                    "translate-x-0": isOpen,
                    "lg:translate-x-0": true,
                    "hidden lg:block": !isOpen
                })}
            >
                <TooltipProvider delayDuration={0}>
                    <nav className="flex flex-col h-full px-2 py-4">
                        <div className="mb-4">
                            {renderNewChatButton()}
                        </div>

                        <div className="flex-1 overflow-y-auto scrollbar-on-hover pr-1">
                            {renderConversationsList()}
                        </div>
                    </nav>
                </TooltipProvider>
            </aside>

            {isOpen && (<div
                    className="fixed inset-0 bg-black/40 backdrop-blur-sm z-5 lg:hidden"
                    onClick={() => setIsOpen(false)}
                />)}
        </>);
} 