import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { GoogleSignInButton } from "@/app/components/auth/GoogleSignInButton";
import { signIn } from "next-auth/react";

jest.mock("next-auth/react", () => ({
  signIn: jest.fn(),
}));

describe("GoogleSignInButton", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders Google SVG + default text (TC-U061)", () => {
    render(<GoogleSignInButton />);

    const svg = document.querySelector("svg");
    expect(svg).toBeInTheDocument();
    
    const buttonText = screen.getByText("Sign in with Google");
    expect(buttonText).toBeInTheDocument();
  });

  it("clicking triggers signIn with google and callbackUrl (TC-U062)", async () => {
    const user = userEvent.setup();
    const customCallbackUrl = "/dashboard";

    render(<GoogleSignInButton callbackUrl={customCallbackUrl} />);

    const button = screen.getByRole("button", { name: /sign in with google/i });
    await user.click(button);

    expect(signIn).toHaveBeenCalledWith("google", { callbackUrl: customCallbackUrl });
  });

  it("when isLoading=true, button is disabled, spinner appears, and shows loadingText (TC-U063)", () => {
    const loadingText = "Please wait...";
    render(<GoogleSignInButton isLoading={true} loadingText={loadingText} />);

    const button = screen.getByRole("button", { name: loadingText });
    expect(button).toBeDisabled();

    const spinner = screen.getByRole("status");
    expect(spinner).toBeInTheDocument();

    expect(screen.getByText(loadingText)).toBeInTheDocument();
    
    expect(spinner).toBeVisible();
  });
  
  it("applies small size classes when size='sm'", () => {
    render(<GoogleSignInButton size="sm" />);
    
    const button = screen.getByRole("button");
    expect(button).toHaveClass("py-1.5", "px-3", "text-xs", "h-8");
    
    const svg = document.querySelector("svg");
    expect(svg).toHaveClass("w-4", "h-4");
  });
  
  it("applies large size classes when size='lg'", () => {
    render(<GoogleSignInButton size="lg" />);
    
    const button = screen.getByRole("button");
    expect(button).toHaveClass("py-3", "px-6", "text-base", "h-12");
    
    const svg = document.querySelector("svg");
    expect(svg).toHaveClass("w-6", "h-6");
  });
  
  it("hides Google logo when showLogo=false", () => {
    render(<GoogleSignInButton showLogo={false} />);
    
    const svg = document.querySelector("svg");
    expect(svg).not.toBeInTheDocument();
    
    expect(screen.getByText("Sign in with Google")).toBeInTheDocument();
  });
  
  it("displays custom text when text prop is provided", () => {
    const customText = "Login with Google";
    render(<GoogleSignInButton text={customText} />);
    
    expect(screen.getByText(customText)).toBeInTheDocument();
    expect(screen.queryByText("Sign in with Google")).not.toBeInTheDocument();
  });
}); 