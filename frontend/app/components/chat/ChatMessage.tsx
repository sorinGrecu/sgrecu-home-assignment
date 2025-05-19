import {cn} from '@/lib/utils';
import {Message, ROLE} from './chatModels';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {AnchorHTMLAttributes, DetailedHTMLProps, HTMLAttributes} from 'react';

interface ChatMessageProps {
    message: Message;
    isLoading?: boolean;
    isLastMessage?: boolean;
}

type MarkdownAnchorProps = DetailedHTMLProps<AnchorHTMLAttributes<HTMLAnchorElement>, HTMLAnchorElement>;
type MarkdownListProps = DetailedHTMLProps<HTMLAttributes<HTMLUListElement>, HTMLUListElement>;
type MarkdownOrderedListProps = DetailedHTMLProps<HTMLAttributes<HTMLOListElement>, HTMLOListElement>;
type MarkdownListItemProps = DetailedHTMLProps<HTMLAttributes<HTMLLIElement>, HTMLLIElement>;
type MarkdownHeadingProps = DetailedHTMLProps<HTMLAttributes<HTMLHeadingElement>, HTMLHeadingElement>;
type MarkdownPreProps = DetailedHTMLProps<HTMLAttributes<HTMLPreElement>, HTMLPreElement>;
type MarkdownCodeProps = DetailedHTMLProps<HTMLAttributes<HTMLElement>, HTMLElement>;
type MarkdownBlockquoteProps = DetailedHTMLProps<HTMLAttributes<HTMLQuoteElement>, HTMLQuoteElement>;
type MarkdownHrProps = DetailedHTMLProps<HTMLAttributes<HTMLHRElement>, HTMLHRElement>;

const markdownComponents = {
    a: (props: MarkdownAnchorProps) => <a className="text-blue-400 hover:underline" target="_blank"
                                          rel="noopener noreferrer" {...props} />,
    ul: (props: MarkdownListProps) => <ul className="list-disc pl-5 my-2" {...props} />,
    ol: (props: MarkdownOrderedListProps) => <ol className="list-decimal pl-5 my-2" {...props} />,
    li: (props: MarkdownListItemProps) => <li className="my-0.5" {...props} />,
    h1: (props: MarkdownHeadingProps) => <h1 className="text-xl font-bold my-2" {...props} />,
    h2: (props: MarkdownHeadingProps) => <h2 className="text-lg font-bold my-2" {...props} />,
    h3: (props: MarkdownHeadingProps) => <h3 className="text-md font-bold my-1.5" {...props} />,
    h4: (props: MarkdownHeadingProps) => <h4 className="font-bold my-1.5" {...props} />,
    pre: (props: MarkdownPreProps) => <pre
        className="bg-zinc-700 p-2 rounded-md text-sm overflow-x-auto my-2" {...props} />,
    code: (props: MarkdownCodeProps) => <code className="bg-zinc-700 px-1 py-0.5 rounded text-sm" {...props} />,
    blockquote: (props: MarkdownBlockquoteProps) => <blockquote
        className="border-l-4 border-zinc-500 pl-2 my-2 italic" {...props} />,
    hr: (props: MarkdownHrProps) => <hr className="my-4 border-zinc-600" {...props} />
};

/**
 * Renders a chat message with support for markdown formatting and loading states
 */
export function ChatMessage({message, isLoading = false, isLastMessage = false}: ChatMessageProps) {
    const hasContent = message.content && message.content.length > 0;

    return (<div className={cn("flex", message.role === ROLE.USER ? "justify-end" : "justify-start")}>
            <div
                className={cn("px-3 py-2 rounded-lg max-w-[85%]", message.role === ROLE.USER ? "bg-indigo-600 text-white" : "bg-zinc-800 text-zinc-100 border border-zinc-700")}>
                {hasContent ? (<div className="markdown-content prose prose-invert max-w-none">
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            components={markdownComponents}
                        >
                            {message.content}
                        </ReactMarkdown>
                    </div>) : isLoading && isLastMessage ? (<div className="flex items-center h-6 space-x-1">
                        <div className="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-pulse"></div>
                        <div className="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-pulse delay-150"></div>
                        <div className="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-pulse delay-300"></div>
                    </div>) : (<div className="text-gray-400 italic">(No content)</div>)}
            </div>
        </div>);
} 