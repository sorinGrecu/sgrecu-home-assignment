"use client"

import React from 'react'

export default function ChatLayout({children,}: {
    children: React.ReactNode
}): React.ReactElement {
    return (<div className="bg-zinc-900">
        <section>
            {children}
        </section>
    </div>)
} 