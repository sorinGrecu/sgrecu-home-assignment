import {render, screen} from "@testing-library/react";
import {Spinner} from "@/app/components/ui/spinner";

describe("Spinner", () => {
    it("renders with default text (TC-U501)", () => {
        render(<Spinner/>);

        const loadingText = screen.getByText("Loading...");
        expect(loadingText).toBeInTheDocument();
        expect(loadingText).toHaveClass("text-gray-500");
    });

    it("renders with custom text (TC-U502)", () => {
        render(<Spinner text="Please wait"/>);

        const customText = screen.getByText("Please wait");
        expect(customText).toBeInTheDocument();
    });

    it("renders without text when text prop is empty string (TC-U503)", () => {
        render(<Spinner text=""/>);

        expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
    });

    it("applies additional className when provided (TC-U504)", () => {
        const {container} = render(<Spinner className="mt-4"/>);

        const spinnerContainer = container.firstChild;
        expect(spinnerContainer).toHaveClass("mt-4");
        expect(spinnerContainer).toHaveClass("flex", "items-center", "space-x-2");
    });

    it("renders animated spinner element (TC-U505)", () => {
        const {container} = render(<Spinner/>);

        const spinnerElement = container.querySelector(".animate-spin");
        expect(spinnerElement).toBeInTheDocument();
        expect(spinnerElement).toHaveClass("rounded-full", "border-2", "border-t-transparent", "border-gray-500");
    });
}); 