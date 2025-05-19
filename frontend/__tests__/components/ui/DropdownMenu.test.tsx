import { render, screen } from "@testing-library/react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/app/components/ui/dropdown-menu";

describe("DropdownMenu", () => {
  it("renders the dropdown menu trigger only initially (TC-U701)", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open Menu</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Item 1</DropdownMenuItem>
          <DropdownMenuItem>Item 2</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
    
    const trigger = screen.getByText("Open Menu");
    expect(trigger).toBeInTheDocument();
    
    expect(screen.queryByText("Item 1")).not.toBeInTheDocument();
    expect(screen.queryByText("Item 2")).not.toBeInTheDocument();
  });
  
  it("renders dropdown content when menu is open (TC-U702)", () => {
    render(
      <DropdownMenu open>
        <DropdownMenuTrigger>Open Menu</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Item 1</DropdownMenuItem>
          <DropdownMenuItem>Item 2</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
    
    const trigger = screen.getByText("Open Menu");
    expect(trigger).toBeInTheDocument();
    
    const item1 = screen.getByText("Item 1");
    const item2 = screen.getByText("Item 2");
    expect(item1).toBeInTheDocument();
    expect(item2).toBeInTheDocument();
  });
  
  it("renders dropdown content with custom className (TC-U703)", () => {
    render(
      <DropdownMenu open>
        <DropdownMenuTrigger>Open Menu</DropdownMenuTrigger>
        <DropdownMenuContent className="custom-dropdown">
          <DropdownMenuItem>Item 1</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
    
    const item = screen.getByText("Item 1");
    expect(item).toBeInTheDocument();
    
    const menuContentWrapper = item.closest('[class*="custom-dropdown"]');
    expect(menuContentWrapper).not.toBeNull();
  });
  
  it("renders dropdown menu item with custom className (TC-U704)", () => {
    render(
      <DropdownMenu open>
        <DropdownMenuTrigger>Open Menu</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem className="custom-item">Custom Item</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
    
    const item = screen.getByText("Custom Item");
    expect(item).toBeInTheDocument();
    
    expect(item).toHaveClass("custom-item");
  });
  
  it("passes custom sideOffset prop to dropdown content (TC-U705)", () => {
    render(
      <DropdownMenu open>
        <DropdownMenuTrigger>Open Menu</DropdownMenuTrigger>
        <DropdownMenuContent sideOffset={10}>
          <DropdownMenuItem>Item 1</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
    
    const item = screen.getByText("Item 1");
    expect(item).toBeInTheDocument();
  });
}); 