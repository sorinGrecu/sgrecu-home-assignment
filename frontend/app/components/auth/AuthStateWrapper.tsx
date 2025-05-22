"use client"

import {useSession} from "next-auth/react"
import {usePathname, useRouter} from "next/navigation"
import {useEffect, useState} from "react"
import {LoadingSpinner} from "../ui/LoadingSpinner"

interface AuthStateWrapperProps {
    children: React.ReactNode
}

interface SessionWithBackendToken {
    backendToken?: string
    user?: {
        name?: string | null
        email?: string | null
        image?: string | null
    }
    expires: string
}

/**
 * Component that prevents the flash of unauthenticated content
 * when navigating after login redirects
 */
export function AuthStateWrapper({children}: AuthStateWrapperProps) {
    const {data: session, status} = useSession()
    const router = useRouter()
    const pathname = usePathname()
    const [isTransitioning, setIsTransitioning] = useState(false)

    const sessionWithToken = session as SessionWithBackendToken | null
    
    const isPublicRoute = pathname === "/" || pathname.startsWith("/api/auth")
    const isLoadingAuth = status === "loading"
    const isAuthenticated = status === "authenticated"
    const hasBackendToken = !!sessionWithToken?.backendToken
    const isFullyAuthenticated = isAuthenticated && hasBackendToken
    const hasInvalidSession = isAuthenticated && !hasBackendToken

    useEffect(() => {
        if (hasInvalidSession) {
            setIsTransitioning(true)
            router.push("/?error=session_expired")
            return
        }

        if (isFullyAuthenticated && isPublicRoute) {
            setIsTransitioning(true)
            router.push("/chat")
        } else if (!isFullyAuthenticated && !isPublicRoute && !isLoadingAuth) {
            setIsTransitioning(true)
            router.push("/")
        } else {
            setIsTransitioning(false)
        }
    }, [isFullyAuthenticated, isPublicRoute, router, isLoadingAuth, pathname, hasInvalidSession])

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