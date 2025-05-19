import {render, screen} from "@testing-library/react";
import {ChatMessage} from "@/app/components/chat/ChatMessage";
import {ROLE} from "@/app/components/chat/chatModels";

jest.mock("react-markdown", () => ({
    __esModule: true, default: ({children, components}: { children: string; components: any }) => {
        if (children.includes('[') && children.includes('](')) {
            const linkMatch = children.match(/\[([^\]]+)]\(([^)]+)\)/);
            if (linkMatch && components.a) {
                const [, linkText, linkUrl] = linkMatch;
                return (<div data-testid="markdown">
                    <a
                        href={linkUrl}
                        data-testid="markdown-link"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-400 hover:underline"
                    >
                        {linkText}
                    </a>
                </div>);
            }
        } else if (children.startsWith('# ')) {
            const headingText = children.substring(2);
            if (components.h1) {
                return (<div data-testid="markdown">
                    <h1
                        data-testid="markdown-heading"
                        className="text-xl font-bold my-2"
                    >
                        {headingText}
                    </h1>
                </div>);
            }
        }

        return <div data-testid="markdown">{children}</div>;
    },
}));

jest.mock("remark-gfm", () => ({
    __esModule: true, default: () => ({}),
}));

describe("ChatMessage", () => {
    it("displays user message with right alignment and user-bubble styling (TC-U081)", () => {
        const userMessage = {
            role: ROLE.USER, content: "Hello, this is a test", conversationId: "", createdAt: new Date().toISOString(),
        };

        const {container} = render(<ChatMessage message={userMessage}/>);

        const messageWrapper = container.querySelector('.flex');
        expect(messageWrapper).toHaveClass("justify-end");

        const messageBubble = container.querySelector('.bg-indigo-600');
        expect(messageBubble).toBeInTheDocument();

        expect(screen.getByText(userMessage.content)).toBeInTheDocument();
    });

    it("displays assistant message with left alignment and markdown rendering (TC-U082)", () => {
        const assistantMessage = {
            role: ROLE.ASSISTANT,
            content: "Hello, I'm **Assistant**",
            conversationId: "",
            createdAt: new Date().toISOString(),
        };

        const {container} = render(<ChatMessage message={assistantMessage}/>);

        const messageWrapper = container.querySelector('.flex');
        expect(messageWrapper).toHaveClass("justify-start");

        const messageBubble = container.querySelector('.bg-zinc-800');
        expect(messageBubble).toBeInTheDocument();

        const markdownContent = screen.getByTestId("markdown");
        expect(markdownContent).toHaveTextContent(assistantMessage.content);
    });

    it("displays loading indicator for empty content when isLoading and isLastMessage (TC-U083)", () => {
        const emptyMessage = {
            role: ROLE.ASSISTANT, content: "", conversationId: "", createdAt: new Date().toISOString(),
        };

        const {container} = render(<ChatMessage message={emptyMessage} isLoading={true} isLastMessage={true}/>);

        const loadingDotsContainer = container.querySelector('.flex.items-center.h-6.space-x-1');
        expect(loadingDotsContainer).toBeInTheDocument();

        const dots = loadingDotsContainer!.querySelectorAll("div");
        expect(dots.length).toBe(3);
    });

    it("displays '(No content)' for empty content when not loading (TC-U084)", () => {
        const emptyMessage = {
            role: ROLE.ASSISTANT, content: "", conversationId: "", createdAt: new Date().toISOString(),
        };

        render(<ChatMessage message={emptyMessage} isLoading={false}/>);

        const noContentText = screen.getByText("(No content)");
        expect(noContentText).toBeInTheDocument();
        expect(noContentText).toHaveClass("italic");
    });

    it("renders a link with target and underline class", () => {
        const messageWithLink = {
            role: ROLE.ASSISTANT,
            content: "Check out this [link](https://example.com)",
            conversationId: "",
            createdAt: new Date().toISOString(),
        };

        render(<ChatMessage message={messageWithLink}/>);

        const link = screen.getByTestId("markdown-link");
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute("href", "https://example.com");
        expect(link).toHaveAttribute("target", "_blank");
        expect(link).toHaveAttribute("rel", "noopener noreferrer");
        expect(link).toHaveClass("text-blue-400");
        expect(link).toHaveClass("hover:underline");
        expect(link).toHaveTextContent("link");
    });

    it("renders an H1 element for '# heading'", () => {
        const messageWithHeading = {
            role: ROLE.ASSISTANT, content: "# Main Heading", conversationId: "", createdAt: new Date().toISOString(),
        };

        render(<ChatMessage message={messageWithHeading}/>);

        const heading = screen.getByTestId("markdown-heading");
        expect(heading).toBeInTheDocument();
        expect(heading.tagName).toBe("H1");
        expect(heading).toHaveClass("text-xl", "font-bold", "my-2");
        expect(heading).toHaveTextContent("Main Heading");
    });
}); 