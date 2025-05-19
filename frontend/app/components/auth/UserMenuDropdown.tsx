"use client";

import React from "react";
import {signOut, useSession} from "next-auth/react";
import {toast} from "sonner";
import {LoadingSpinner} from '../ui/LoadingSpinner';
import logger from "@/lib/utils/logger";
import {Popover, PopoverContent, PopoverTrigger} from "../ui/popover";
import {ChevronDown} from "lucide-react";
import Image from "next/image";

/**
 * User menu dropdown component that displays the authenticated user's information
 * and provides sign-out functionality.
 *
 * @returns A popover component that serves as a user menu
 */
export function UserMenuDropdown(): React.JSX.Element {
    const {data: session} = useSession();
    const [isOpen, setIsOpen] = React.useState(false);
    const [isLoading, setIsLoading] = React.useState(false);

    const {firstName, lastName} = React.useMemo(() => {
        const userName = session?.user?.name || "";
        const parts = userName.split(" ");
        return {
            firstName: parts[0] || "", lastName: parts.length > 1 ? parts[parts.length - 1] : ""
        };
    }, [session?.user?.name]);

    const handleLogout = async () => {
        setIsLoading(true);
        try {
            await signOut({callbackUrl: "/"});
        } catch (error) {
            logger.error("Logout error:", error);
            toast.error("Failed to sign out. Please try again.");
        } finally {
            setIsLoading(false);
            setIsOpen(false);
        }
    };

    return (<Popover open={isOpen} onOpenChange={setIsOpen}>
            <PopoverTrigger asChild>
                <button
                    className="flex items-center gap-2 rounded-md py-2 px-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                    aria-label="User menu"
                >
                    {session?.user?.image && (<Image
                            src={session.user.image}
                            alt={`${firstName}'s profile picture`}
                            className="rounded-full"
                            width={32}
                            height={32}
                        />)}
                    <div className="flex items-center gap-1">
                        <span className="text-sm font-medium text-white">{firstName} {lastName}</span>
                        <ChevronDown
                            className={`h-4 w-4 transition-transform duration-200 text-white ${isOpen ? 'rotate-180' : ''}`}/>
                    </div>
                </button>
            </PopoverTrigger>

            <PopoverContent
                className="w-48 p-0 rounded-md bg-white dark:bg-gray-900 shadow-lg"
                align="end"
                sideOffset={8}
            >
                <button
                    onClick={handleLogout}
                    disabled={isLoading}
                    className="w-full flex items-center justify-start gap-2 px-4 py-2 min-h-[40px] text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors disabled:opacity-50"
                >
                    {isLoading ? (<>
                            <LoadingSpinner size="sm"/>
                            <span>Processing...</span>
                        </>) : (<span>Sign Out</span>)}
                </button>
            </PopoverContent>
        </Popover>);
} 