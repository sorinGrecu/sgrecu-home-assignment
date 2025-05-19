import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MessageInput } from "@/app/components/chat/MessageInput";

jest.mock("../../../app/components/ui/AnimatedBorder", () => ({
  AnimatedBorder: ({ children, isActive, variant, className }: any) => (
    <div 
      data-testid="animated-border" 
      data-active={isActive.toString()} 
      data-variant={variant}
      className={className}
    >
      {children}
    </div>
  ),
}));

const originalScrollHeightDescriptor = Object.getOwnPropertyDescriptor(
  HTMLElement.prototype, 
  'scrollHeight'
);

describe("MessageInput", () => {
  beforeAll(() => {
    Object.defineProperty(HTMLElement.prototype, 'scrollHeight', {
      configurable: true,
      get: function() {
        if (this.value && this.value.length > 50) {
          return 80; 
        }
        return 30; 
      }
    });
  });
  
  afterAll(() => {
    if (originalScrollHeightDescriptor) {
      Object.defineProperty(HTMLElement.prototype, 'scrollHeight', originalScrollHeightDescriptor);
    } else {
      Object.defineProperty(HTMLElement.prototype, 'scrollHeight', {
        configurable: true,
        value: 0,
        writable: true
      });
    }
  });

  it("renders a textarea with placeholder 'Ask a question...' (TC-U051)", () => {
    render(<MessageInput onSubmit={jest.fn()} isLoading={false} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    expect(textarea).toBeInTheDocument();
    expect(textarea.tagName).toBe("TEXTAREA");
  });

  it("calls onSubmit with trimmed text when pressing Enter (TC-U052)", async () => {
    const handleSubmit = jest.fn();
    const user = userEvent.setup();
    
    render(<MessageInput onSubmit={handleSubmit} isLoading={false} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    await user.type(textarea, "Hello world ");
    await user.keyboard("{Enter}");
    
    expect(handleSubmit).toHaveBeenCalledWith("Hello world");
  });
  
  it("does not call onSubmit when input is empty or only contains spaces", async () => {
    const handleSubmit = jest.fn();
    const user = userEvent.setup();
    
    render(<MessageInput onSubmit={handleSubmit} isLoading={false} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    
    fireEvent.change(textarea, { target: { value: "" } });
    await user.keyboard("{Enter}");
    expect(handleSubmit).not.toHaveBeenCalled();
    
    fireEvent.change(textarea, { target: { value: "   " } });
    await user.keyboard("{Enter}");
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it("inserts newline when pressing Shift+Enter (TC-U053)", async () => {
    const handleSubmit = jest.fn();
    const user = userEvent.setup();
    
    render(<MessageInput onSubmit={handleSubmit} isLoading={false} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    await user.type(textarea, "First line");
    await user.keyboard("{Shift>}{Enter}{/Shift}");
    await user.type(textarea, "Second line");
    
    expect(textarea).toHaveValue("First line\nSecond line");
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it("disables textarea when isLoading is true (TC-U054)", () => {
    render(<MessageInput onSubmit={jest.fn()} isLoading={true} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    expect(textarea).toBeDisabled();
  });
  
  it("toggles AnimatedBorder isActive on focus/blur", async () => {
    render(<MessageInput onSubmit={jest.fn()} isLoading={false} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    const border = screen.getByTestId("animated-border");
    
    fireEvent.blur(textarea);
    expect(border).toHaveAttribute("data-active", "false");
    
    fireEvent.focus(textarea);
    expect(border).toHaveAttribute("data-active", "true");
    
    fireEvent.blur(textarea);
    expect(border).toHaveAttribute("data-active", "false");
  });
  
  it("changes to rounded-xl when text is multiline", async () => {
    const user = userEvent.setup();
    render(<MessageInput onSubmit={jest.fn()} isLoading={false} />);
    
    const textarea = screen.getByPlaceholderText("Ask a question...");
    const border = screen.getByTestId("animated-border");
    
    expect(border).toHaveClass("rounded-full");
    expect(border).not.toHaveClass("rounded-xl");
    
    const longText = "This is a very long text that should cause the textarea to expand beyond 40px in height and trigger the change to rounded-xl instead of rounded-full.";
    await user.type(textarea, longText);
    
    expect(border).not.toHaveClass("rounded-full");
    expect(border).toHaveClass("rounded-xl");
  });
}); 