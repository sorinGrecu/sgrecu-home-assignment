import React from "react";
import {render, screen} from "@testing-library/react";
import {MessageList} from "@/app/components/chat/MessageList";
import {ROLE} from "@/app/components/chat/chatModels";

jest.mock("../../../app/components/chat/ChatMessage", () => ({
    ChatMessage: ({message, isLoading, isLastMessage}: any) => (<div
            data-testid="chat-message"
            data-role={message.role}
            data-content={message.content}
            data-is-loading={isLoading}
            data-is-last={isLastMessage}
        >
            Message Component
        </div>),
}));

jest.mock('react', () => {
    const originalReact = jest.requireActual('react');
    return {
        ...originalReact, useEffect: jest.fn()
    };
});

describe("MessageList", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("displays placeholder text when messages array is empty (TC-U091)", () => {
        render(<MessageList messages={[]} isLoading={false}/>);

        const placeholderText = screen.getByText("Ask a question to start the conversation");
        expect(placeholderText).toBeInTheDocument();

        expect(screen.queryByTestId("chat-message")).not.toBeInTheDocument();
    });

    it("renders a list of ChatMessage items when messages exist (TC-U092)", () => {
        const messages = [{
            role: ROLE.USER, content: "Hello", conversationId: "", createdAt: new Date().toISOString(),
        }, {
            role: ROLE.ASSISTANT, content: "Hi there!", conversationId: "", createdAt: new Date().toISOString(),
        }, {
            role: ROLE.USER, content: "How are you?", conversationId: "", createdAt: new Date().toISOString(),
        },];

        render(<MessageList messages={messages} isLoading={true}/>);

        const messageElements = screen.getAllByTestId("chat-message");
        expect(messageElements).toHaveLength(3);

        expect(messageElements[2]).toHaveAttribute("data-is-last", "true");

        messageElements.forEach((message) => {
            expect(message).toHaveAttribute("data-is-loading", "true");
        });

        expect(messageElements[0]).toHaveAttribute("data-content", "Hello");
        expect(messageElements[0]).toHaveAttribute("data-role", ROLE.USER);

        expect(messageElements[1]).toHaveAttribute("data-content", "Hi there!");
        expect(messageElements[1]).toHaveAttribute("data-role", ROLE.ASSISTANT);
    });

    it("has the correct container classes for styling and scrolling", () => {
        const messages = [{
            role: ROLE.USER, content: "Test message", conversationId: "", createdAt: new Date().toISOString(),
        }];

        const {container} = render(<MessageList messages={messages} isLoading={false}/>);

        const outerContainer = container.firstChild as HTMLElement;
        expect(outerContainer).toHaveClass("flex-1", "px-2", "md:px-4", "py-4", "pb-24", "overflow-y-auto", "scrollbar-on-hover");

        const innerContainer = outerContainer.firstChild as HTMLElement;
        expect(innerContainer).toHaveClass("space-y-6", "pt-16");
    });
}); 