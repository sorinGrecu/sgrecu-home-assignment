import { render, screen } from "@testing-library/react";
import ChatInterface from "../../../app/components/chat/ChatInterface";
import { useSession } from "next-auth/react";
import { useChat } from "@/lib/hooks/useChat";

jest.mock("next-auth/react", () => ({
  useSession: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  usePathname: jest.fn().mockReturnValue("/chat"),
}));

jest.mock("@/lib/hooks/useChat", () => ({
  useChat: jest.fn().mockReturnValue({
    messages: [],
    isLoading: false,
    sendMessage: jest.fn()
  }),
}));

jest.mock("../../../app/components/auth/AuthGuard", () => ({
  AuthGuard: ({ isLoading }: { isLoading: boolean }) => (
    <div data-testid="auth-guard">
      {isLoading ? "Loading auth..." : "Please sign in"}
    </div>
  ),
}));

jest.mock("../../../app/components/chat/MessageList", () => ({
  MessageList: ({ messages, isLoading }: { messages: any[], isLoading: boolean }) => (
    <div data-testid="message-list">
      {isLoading ? "Loading messages..." : `${messages.length} messages`}
    </div>
  ),
}));

jest.mock("../../../app/components/chat/MessageInput", () => ({
  MessageInput: ({ isLoading }: { isLoading: boolean, onSubmit: any }) => (
    <div data-testid="message-input">
      {isLoading ? "Loading input..." : "Type a message"}
    </div>
  ),
}));

describe("ChatInterface", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  
  it("renders AuthGuard when user is not authenticated (TC-C501)", () => {
    (useSession as jest.Mock).mockReturnValue({
      status: "unauthenticated",
    });
    
    render(<ChatInterface />);
    
    expect(screen.getByTestId("auth-guard")).toBeInTheDocument();
    expect(screen.queryByTestId("message-list")).not.toBeInTheDocument();
    expect(screen.queryByTestId("message-input")).not.toBeInTheDocument();
  });
  
  it("renders AuthGuard with loading state when authentication is loading (TC-C502)", () => {
    (useSession as jest.Mock).mockReturnValue({
      status: "loading",
    });
    
    render(<ChatInterface />);
    
    expect(screen.getByTestId("auth-guard")).toBeInTheDocument();
    expect(screen.getByTestId("auth-guard")).toHaveTextContent("Loading auth...");
  });
  
  it("renders chat interface when user is authenticated (TC-C503)", () => {
    (useSession as jest.Mock).mockReturnValue({
      status: "authenticated",
      data: { user: { name: "Test User" } },
    });
    
    (useChat as jest.Mock).mockReturnValue({
      messages: [],
      isLoading: false,
      sendMessage: jest.fn(),
    });
    
    render(<ChatInterface />);
    
    expect(screen.getByTestId("message-list")).toBeInTheDocument();
    expect(screen.getByTestId("message-input")).toBeInTheDocument();
    expect(screen.queryByTestId("auth-guard")).not.toBeInTheDocument();
  });
  
  it("passes initial messages to useChat hook (TC-C504)", () => {
    (useSession as jest.Mock).mockReturnValue({
      status: "authenticated",
      data: { user: { name: "Test User" } },
    });
    
    (useChat as jest.Mock).mockReturnValue({
      messages: [{ id: '1', content: 'Hello', role: 'user' }],
      isLoading: false,
      sendMessage: jest.fn(),
    });
    
    const initialMessages = [{ id: '1', content: 'Hello', role: 'user' }];
    render(<ChatInterface initialMessages={initialMessages} />);
    
    expect(useChat).toHaveBeenCalledWith(expect.objectContaining({
      initialMessages: initialMessages,
    }));
    
    expect(screen.getByTestId("message-list")).toHaveTextContent("1 messages");
  });
  
  it("shows loading state when chat is loading (TC-C505)", () => {
    (useSession as jest.Mock).mockReturnValue({
      status: "authenticated",
      data: { user: { name: "Test User" } },
    });
    
    (useChat as jest.Mock).mockReturnValue({
      messages: [],
      isLoading: true,
      sendMessage: jest.fn(),
    });
    
    render(<ChatInterface />);
    
    expect(screen.getByTestId("message-list")).toHaveTextContent("Loading messages...");
    expect(screen.getByTestId("message-input")).toHaveTextContent("Loading input...");
  });
}); 