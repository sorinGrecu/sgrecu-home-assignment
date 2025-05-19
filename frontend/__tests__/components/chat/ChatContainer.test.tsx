import { render, screen } from "@testing-library/react";
import { ChatContainer } from "@/app/components/chat/ChatContainer";

describe("ChatContainer", () => {
  it("renders its children (TC-C401)", () => {
    render(
      <ChatContainer>
        <div data-testid="child-content">Test content</div>
      </ChatContainer>
    );
    
    const childContent = screen.getByTestId("child-content");
    expect(childContent).toBeInTheDocument();
    expect(childContent).toHaveTextContent("Test content");
  });
  
  it("applies flex-col and min-h-full classes to container (TC-C402)", () => {
    const { container } = render(
      <ChatContainer>
        <div>Child content</div>
      </ChatContainer>
    );
    
    const containerDiv = container.firstChild;
    expect(containerDiv).toHaveClass("flex", "flex-col", "min-h-full");
  });
  
  it("works with multiple children (TC-C403)", () => {
    render(
      <ChatContainer>
        <div data-testid="first-child">First</div>
        <div data-testid="second-child">Second</div>
      </ChatContainer>
    );
    
    expect(screen.getByTestId("first-child")).toBeInTheDocument();
    expect(screen.getByTestId("second-child")).toBeInTheDocument();
  });
}); 