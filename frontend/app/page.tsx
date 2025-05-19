"use client"

import {useSearchParams} from 'next/navigation'
import {AlertCircle} from 'lucide-react'
import {GoogleSignInButton} from './components/auth/GoogleSignInButton'

const ERROR_MESSAGES = {
    'session_expired': 'Your session has expired. Please sign in again.',
    'unauthorized': 'You don&apos;t have permission to access that resource.',
    'default': 'An authentication error occurred. Please sign in again.'
};

export default function HomePage() {
    const searchParams = useSearchParams()
    const error = searchParams?.get('error')

    return (<div className="flex items-center justify-center h-full">
            <div className="w-full max-w-3xl px-6 md:px-8 py-12">
                {error && (<div
                        className="mb-6 p-4 bg-red-900/30 border border-red-500/50 rounded-lg text-red-200 flex items-start mx-auto">
                        <AlertCircle className="h-5 w-5 mr-2 mt-0.5 flex-shrink-0"/>
                        <span>{ERROR_MESSAGES[error as keyof typeof ERROR_MESSAGES] || ERROR_MESSAGES.default}</span>
                    </div>)}

                <div className="text-center">
                    <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold mb-6 bg-gradient-to-r from-blue-400 to-violet-500 text-transparent bg-clip-text">
                        Sorin Grecu&apos;s Chat App
                    </h1>
                    <p className="text-xl text-gray-300 mx-auto mb-8">
                        A powerful AI chat experience built for my home assignment
                    </p>

                    <div className="space-y-6 mb-8">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="bg-zinc-800/50 p-4 rounded-lg border border-zinc-700/50">
                                <h3 className="font-medium text-lg mb-2 text-blue-300">Seamless Authentication</h3>
                                <p className="text-zinc-300">Securely log in with your Google account for a personalized
                                    chat experience.</p>
                            </div>
                            <div className="bg-zinc-800/50 p-4 rounded-lg border border-zinc-700/50">
                                <h3 className="font-medium text-lg mb-2 text-violet-300">Chat Management</h3>
                                <p className="text-zinc-300">View your conversation history with a clean and
                                    straightforward list interface.</p>
                            </div>
                            <div className="bg-zinc-800/50 p-4 rounded-lg border border-zinc-700/50">
                                <h3 className="font-medium text-lg mb-2 text-indigo-300">Create & Continue</h3>
                                <p className="text-zinc-300">Start new discussions or pick up where you left off with
                                    existing conversations.</p>
                            </div>
                            <div className="bg-zinc-800/50 p-4 rounded-lg border border-zinc-700/50">
                                <h3 className="font-medium text-lg mb-2 text-purple-300">AI-Powered Responses</h3>
                                <p className="text-zinc-300">Get intelligent replies powered by advanced language models
                                    in real-time.</p>
                            </div>
                        </div>
                    </div>

                    <div className="flex justify-center">
                        <GoogleSignInButton size="lg"/>
                    </div>
                </div>
            </div>
        </div>)
}
