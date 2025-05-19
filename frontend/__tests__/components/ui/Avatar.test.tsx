import {render, screen} from "@testing-library/react";
import {Avatar} from "@/app/components/ui/avatar";

jest.mock("next/image", () => ({
    __esModule: true, default: (props: any) => {
        // eslint-disable-next-line jsx-a11y/alt-text, @next/next/no-img-element
        return <img data-testid="avatar-image" {...props} />;
    },
}));

describe("Avatar", () => {
    it("renders an image with rounded-full class (TC-U201)", () => {
        render(<Avatar src="/test-avatar.jpg" alt="Test user"/>);

        const avatar = screen.getByAltText("Test user");
        expect(avatar).toBeInTheDocument();
        expect(avatar).toHaveClass("rounded-full");
    });

    it("sets correct width and height by default (TC-U202)", () => {
        render(<Avatar src="/test-avatar.jpg" alt="Test user"/>);

        const avatar = screen.getByAltText("Test user");
        expect(avatar).toHaveAttribute("width", "32");
        expect(avatar).toHaveAttribute("height", "32");
    });

    it("applies additional className when provided (TC-U203)", () => {
        render(<Avatar src="/test-avatar.jpg" alt="Test user" className="border-2"/>);

        const avatar = screen.getByAltText("Test user");
        expect(avatar).toHaveClass("rounded-full");
        expect(avatar).toHaveClass("border-2");
    });

    it("passes additional props to the Image component (TC-U204)", () => {
        render(<Avatar
            src="/test-avatar.jpg"
            alt="Test user"
            width={64}
            height={64}
            priority
        />);

        const avatar = screen.getByTestId("avatar-image");
        expect(avatar).toHaveAttribute("width", "64");
        expect(avatar).toHaveAttribute("height", "64");
    });
}); 