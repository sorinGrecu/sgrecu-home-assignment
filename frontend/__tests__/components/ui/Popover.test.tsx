import {render, screen} from "@testing-library/react";
import {Popover, PopoverContent, PopoverTrigger,} from "@/app/components/ui/popover";

describe("Popover", () => {
    it("renders the popover trigger only initially (TC-U601)", () => {
        render(<Popover>
            <PopoverTrigger>Open Popover</PopoverTrigger>
            <PopoverContent>Popover content</PopoverContent>
        </Popover>);

        const trigger = screen.getByText("Open Popover");
        expect(trigger).toBeInTheDocument();

        expect(screen.queryByText("Popover content")).not.toBeInTheDocument();
    });

    it("renders the popover content when popover is open (TC-U602)", () => {
        render(<Popover open>
            <PopoverTrigger>Open Popover</PopoverTrigger>
            <PopoverContent>Popover content</PopoverContent>
        </Popover>);

        const trigger = screen.getByText("Open Popover");
        expect(trigger).toBeInTheDocument();

        const content = screen.getByText("Popover content");
        expect(content).toBeInTheDocument();
    });

    it("renders popover content with custom className (TC-U603)", () => {
        render(<Popover open>
            <PopoverTrigger>Open Popover</PopoverTrigger>
            <PopoverContent className="custom-class">Popover content</PopoverContent>
        </Popover>);

        const contentElement = screen.getByText("Popover content");
        expect(contentElement).toBeInTheDocument();

        const popoverContentWrapper = contentElement.closest('[class*="custom-class"]');
        expect(popoverContentWrapper).not.toBeNull();
    });

    it("passes custom align and sideOffset props (TC-U604)", () => {
        render(<Popover open>
            <PopoverTrigger>Open Popover</PopoverTrigger>
            <PopoverContent align="start" sideOffset={10}>
                Popover content
            </PopoverContent>
        </Popover>);

        const content = screen.getByText("Popover content");
        expect(content).toBeInTheDocument();
    });
}); 