import {nextAuthHandlers} from "@/app/auth";

export const GET = nextAuthHandlers.GET;
export const POST = nextAuthHandlers.POST;

export const runtime = "edge";
export const dynamic = "force-dynamic"; 