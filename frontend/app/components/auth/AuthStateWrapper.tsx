"use client"

import {useSession} from "next-auth/react"
import {usePathname, useRouter} from "next/navigation"
import {useEffect, useState} from "react"
import {LoadingSpinner} from "../ui/LoadingSpinner"

interface AuthStateWrapperProps {
    children: React.ReactNode
}

/**
 * Component that prevents the flash of unauthenticated content
 * when navigating after login redirects
 */
export function AuthStateWrapper({children}: AuthStateWrapperProps) {
    const {status} = useSession()
    const router = useRouter()
    const pathname = usePathname()
    const [isTransitioning, setIsTransitioning] = useState(false)

    const isPublicRoute = pathname === "/" || pathname.startsWith("/api/auth")
    const isLoadingAuth = status === "loading"
    const isAuthenticated = status === "authenticated"

    useEffect(() => {
        if (isAuthenticated && isPublicRoute) {
            setIsTransitioning(true)
            router.push("/chat")
        } else if (!isAuthenticated && !isPublicRoute && !isLoadingAuth) {
            setIsTransitioning(true)
            router.push("/")
        } else {
            setIsTransitioning(false)
        }
    }, [isAuthenticated, isPublicRoute, router, isLoadingAuth, pathname])

    if (isTransitioning || isLoadingAuth) {
        return (<div className="flex items-center justify-center h-screen bg-zinc-900">
                <LoadingSpinner
                    size="lg"
                    showText
                    text="Processing authentication..."
                />
            </div>)
    }

    return <>{children}</>
} 