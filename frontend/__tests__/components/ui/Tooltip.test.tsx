import {render, screen} from "@testing-library/react";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger,} from "@/app/components/ui/tooltip";

describe("Tooltip", () => {
    it("renders tooltip trigger without initially showing content (TC-U301)", async () => {
        render(<TooltipProvider>
            <Tooltip>
                <TooltipTrigger>Hover me</TooltipTrigger>
                <TooltipContent>Tooltip content</TooltipContent>
            </Tooltip>
        </TooltipProvider>);

        const trigger = screen.getByText("Hover me");
        expect(trigger).toBeInTheDocument();

        expect(screen.queryByText("Tooltip content")).not.toBeInTheDocument();
    });

    it("renders tooltip content when tooltip is open (TC-U302)", async () => {
        render(<TooltipProvider>
            <Tooltip open>
                <TooltipTrigger>Hover me</TooltipTrigger>
                <TooltipContent className="custom-tooltip">Tooltip content</TooltipContent>
            </Tooltip>
        </TooltipProvider>);

        const tooltipTrigger = screen.getByText("Hover me");
        expect(tooltipTrigger).toBeInTheDocument();

        const tooltipRole = screen.getByRole("tooltip");
        expect(tooltipRole).toBeInTheDocument();
    });

    it("renders tooltip content with content (TC-U303)", async () => {
        render(<TooltipProvider>
            <Tooltip open>
                <TooltipTrigger>Hover me</TooltipTrigger>
                <TooltipContent>Tooltip content</TooltipContent>
            </Tooltip>
        </TooltipProvider>);

        const tooltipTrigger = screen.getByText("Hover me");
        expect(tooltipTrigger).toBeInTheDocument();

        const tooltipRole = screen.getByRole("tooltip");
        expect(tooltipRole).toBeInTheDocument();
    });

    it("passes custom sideOffset prop (TC-U304)", async () => {
        render(<TooltipProvider>
            <Tooltip open>
                <TooltipTrigger>Hover me</TooltipTrigger>
                <TooltipContent sideOffset={10}>Tooltip content</TooltipContent>
            </Tooltip>
        </TooltipProvider>);

        const tooltipTrigger = screen.getByText("Hover me");
        expect(tooltipTrigger).toBeInTheDocument();

        const tooltipRole = screen.getByRole("tooltip");
        expect(tooltipRole).toBeInTheDocument();
    });
}); 