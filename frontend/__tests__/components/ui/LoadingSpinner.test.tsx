import {render, screen} from "@testing-library/react";
import {LoadingSpinner} from "@/app/components/ui/LoadingSpinner";

describe("LoadingSpinner", () => {
    it("renders accessible wrapper with role='status' (TC-U041)", () => {
        render(<LoadingSpinner/>);

        const spinner = screen.getByRole("status");
        expect(spinner).toBeInTheDocument();
    });

    it("has sr-only text by default (TC-U042)", () => {
        render(<LoadingSpinner/>);

        const srText = screen.getByText("Loading...");
        expect(srText).toBeInTheDocument();
        expect(srText).toHaveClass("sr-only");
    });

    it("shows visible text when showText is true (TC-U043)", () => {
        const customText = "Please wait...";
        render(<LoadingSpinner showText={true} text={customText}/>);

        const srText = screen.getAllByText(customText)[0];
        expect(srText).toHaveClass("sr-only");

        const visibleTextElements = screen.getAllByText(customText);
        expect(visibleTextElements.length).toBe(2);

        const visibleText = visibleTextElements[1];
        expect(visibleText.tagName).toBe("P");
        expect(visibleText).not.toHaveClass("sr-only");
        expect(visibleText).toBeVisible();
    });

    it("renders small spinner classes when size='sm'", () => {
        const {container} = render(<LoadingSpinner size="sm"/>);

        const outerSpinner = container.querySelector('[aria-hidden="true"]');
        expect(outerSpinner).toHaveClass("w-8", "h-8", "border-2");

        const innerCircle = container.querySelector('.absolute .rounded-full');
        expect(innerCircle).toHaveClass("w-4", "h-4");
    });

    it("renders medium spinner classes by default", () => {
        const {container} = render(<LoadingSpinner/>);

        const outerSpinner = container.querySelector('[aria-hidden="true"]');
        expect(outerSpinner).toHaveClass("w-16", "h-16", "border-3");

        const innerCircle = container.querySelector('.absolute .rounded-full');
        expect(innerCircle).toHaveClass("w-8", "h-8");
    });

    it("renders large spinner classes when size='lg'", () => {
        const {container} = render(<LoadingSpinner size="lg"/>);

        const outerSpinner = container.querySelector('[aria-hidden="true"]');
        expect(outerSpinner).toHaveClass("w-24", "h-24", "border-4");

        const innerCircle = container.querySelector('.absolute .rounded-full');
        expect(innerCircle).toHaveClass("w-12", "h-12");
    });
}); 