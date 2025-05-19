import {render, screen} from "@testing-library/react";
import {Button} from "@/app/components/ui/button";

describe("Button", () => {
    it("renders with default variant and size (TC-U101)", () => {
        render(<Button>Click me</Button>);

        const button = screen.getByRole("button", {name: "Click me"});
        expect(button).toBeInTheDocument();
        expect(button).toHaveClass("bg-zinc-50", "text-zinc-900", "h-9", "px-4", "py-2");
    });

    it("renders with destructive variant (TC-U102)", () => {
        render(<Button variant="destructive">Delete</Button>);

        const button = screen.getByRole("button", {name: "Delete"});
        expect(button).toHaveClass("bg-red-500", "text-zinc-50");
    });

    it("renders with outline variant (TC-U103)", () => {
        render(<Button variant="outline">Outline</Button>);

        const button = screen.getByRole("button", {name: "Outline"});
        expect(button).toHaveClass("border", "border-zinc-200", "bg-transparent");
    });

    it("renders with secondary variant (TC-U104)", () => {
        render(<Button variant="secondary">Secondary</Button>);

        const button = screen.getByRole("button", {name: "Secondary"});
        expect(button).toHaveClass("bg-zinc-100", "text-zinc-900");
    });

    it("renders with ghost variant (TC-U105)", () => {
        render(<Button variant="ghost">Ghost</Button>);

        const button = screen.getByRole("button", {name: "Ghost"});
        expect(button).toHaveClass("hover:bg-zinc-100", "hover:text-zinc-900");
    });

    it("renders with link variant (TC-U106)", () => {
        render(<Button variant="link">Link</Button>);

        const button = screen.getByRole("button", {name: "Link"});
        expect(button).toHaveClass("text-zinc-900", "hover:underline");
    });

    it("renders with small size (TC-U107)", () => {
        render(<Button size="sm">Small</Button>);

        const button = screen.getByRole("button", {name: "Small"});
        expect(button).toHaveClass("h-8", "rounded-md", "px-3", "text-xs");
    });

    it("renders with large size (TC-U108)", () => {
        render(<Button size="lg">Large</Button>);

        const button = screen.getByRole("button", {name: "Large"});
        expect(button).toHaveClass("h-10", "rounded-md", "px-8");
    });

    it("renders with icon size (TC-U109)", () => {
        render(<Button size="icon">+</Button>);

        const button = screen.getByRole("button", {name: "+"});
        expect(button).toHaveClass("h-9", "w-9");
    });

    it("passes additional className to the element (TC-U110)", () => {
        render(<Button className="custom-class">Custom</Button>);

        const button = screen.getByRole("button", {name: "Custom"});
        expect(button).toHaveClass("custom-class");
    });

    it("renders as a different element when asChild is true (TC-U111)", () => {
        render(<Button asChild>
            <a href="#">Link Button</a>
        </Button>);

        const link = screen.getByRole("link", {name: "Link Button"});
        expect(link).toBeInTheDocument();
        expect(link.tagName).toBe("A");
        expect(link).toHaveAttribute("href", "#");
    });

    it("applies disabled styling when disabled (TC-U112)", () => {
        render(<Button disabled>Disabled</Button>);

        const button = screen.getByRole("button", {name: "Disabled"});
        expect(button).toBeDisabled();
    });
}); 