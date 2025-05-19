import {render, screen} from "@testing-library/react";
import {AuthButton} from "@/app/components/auth/AuthButton";
import {useSession} from "next-auth/react";

jest.mock("next-auth/react", () => ({
    useSession: jest.fn(),
}));

jest.mock("../../../app/components/auth/GoogleSignInButton", () => ({
    GoogleSignInButton: ({loadingText, isLoading, layout}: any) => (
        <div data-testid="google-signin-button" data-loading={isLoading} data-text={loadingText} data-layout={layout}>
            Google Sign In Button
        </div>),
}));

jest.mock("../../../app/components/auth/UserMenuDropdown", () => ({
    UserMenuDropdown: () => <div data-testid="user-menu-dropdown">User Menu Dropdown</div>,
}));

describe("AuthButton", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("renders GoogleSignInButton with loading state when status is loading (TC-U071)", () => {
        (useSession as jest.Mock).mockReturnValue({
            status: "loading",
        });

        render(<AuthButton/>);

        const button = screen.getByTestId("google-signin-button");
        expect(button).toBeInTheDocument();
        expect(button).toHaveAttribute("data-loading", "true");
        expect(button).toHaveAttribute("data-text", "Processing...");
        expect(button).toHaveAttribute("data-layout", "compact");
    });

    it("renders GoogleSignInButton when status is unauthenticated (TC-U072)", () => {
        (useSession as jest.Mock).mockReturnValue({
            status: "unauthenticated",
        });

        render(<AuthButton/>);

        const wrapper = screen.getByLabelText("Authentication controls");
        expect(wrapper).toBeInTheDocument();

        const button = screen.getByTestId("google-signin-button");
        expect(button).toBeInTheDocument();
        expect(button).not.toHaveAttribute("data-loading", "true");
        expect(button).toHaveAttribute("data-layout", "compact");
    });

    it("renders UserMenuDropdown when status is authenticated (TC-U073)", () => {
        (useSession as jest.Mock).mockReturnValue({
            status: "authenticated", data: {user: {name: "Test User"}},
        });

        render(<AuthButton/>);

        const wrapper = screen.getByLabelText("Authentication controls");
        expect(wrapper).toBeInTheDocument();

        const dropdown = screen.getByTestId("user-menu-dropdown");
        expect(dropdown).toBeInTheDocument();

        expect(screen.queryByTestId("google-signin-button")).not.toBeInTheDocument();
    });

    it("updates content when switching from unauthenticated to authenticated", () => {
        (useSession as jest.Mock).mockReturnValue({
            status: "unauthenticated",
        });

        const {rerender} = render(<AuthButton/>);
        const wrapper = screen.getByLabelText("Authentication controls");
        expect(wrapper).toBeInTheDocument();
        expect(screen.getByTestId("google-signin-button")).toBeInTheDocument();

        (useSession as jest.Mock).mockReturnValue({
            status: "authenticated", data: {user: {name: "Test User"}},
        });

        rerender(<AuthButton/>);

        expect(wrapper).toBeInTheDocument();
        expect(screen.queryByTestId("google-signin-button")).not.toBeInTheDocument();
        expect(screen.getByTestId("user-menu-dropdown")).toBeInTheDocument();
    });
}); 