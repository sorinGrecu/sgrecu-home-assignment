import {render, screen} from "@testing-library/react";
import {AnimatedBorder} from "@/app/components/ui/AnimatedBorder";

jest.unmock("@/app/components/ui/AnimatedBorder");

describe("AnimatedBorder", () => {
    it("renders children and has gradient-border-idle by default (TC-U031)", () => {
        const {container} = render(<AnimatedBorder>
            <div data-testid="child">Test Content</div>
        </AnimatedBorder>);

        const child = screen.getByTestId("child");
        expect(child).toBeInTheDocument();
        expect(child.textContent).toBe("Test Content");

        const wrapper = container.firstChild as HTMLElement;
        expect(wrapper).toHaveClass("gradient-border-idle");
        expect(wrapper).toHaveClass("relative");
        expect(wrapper).toHaveClass("transition-all");
        expect(wrapper).toHaveClass("duration-300");
    });

    it("applies gradient-border-attention when variant is attention (TC-U032)", () => {
        const {container} = render(<AnimatedBorder variant="attention">
            <div data-testid="child">Test Content</div>
        </AnimatedBorder>);

        const wrapper = container.firstChild as HTMLElement;
        expect(wrapper).toHaveClass("gradient-border-attention");
        expect(wrapper).not.toHaveClass("gradient-border-idle");
        expect(wrapper).not.toHaveClass("gradient-border-focus");
    });

    it("applies gradient-border-focus when isActive is true (TC-U033)", () => {
        const {container} = render(<AnimatedBorder isActive={true} variant="attention">
            <div data-testid="child">Test Content</div>
        </AnimatedBorder>);

        const wrapper = container.firstChild as HTMLElement;
        expect(wrapper).toHaveClass("gradient-border-focus");
        expect(wrapper).not.toHaveClass("gradient-border-attention");
        expect(wrapper).not.toHaveClass("gradient-border-idle");
    });

    it("combines custom className with default classes", () => {
        const {container} = render(<AnimatedBorder className="custom-class test-class">
            <div data-testid="child">Test Content</div>
        </AnimatedBorder>);
        const wrapper = container.firstChild as HTMLElement;
        expect(wrapper).toHaveClass("custom-class");
        expect(wrapper).toHaveClass("test-class");
        expect(wrapper).toHaveClass("gradient-border-idle");
        expect(wrapper).toHaveClass("relative");
    });
}); 