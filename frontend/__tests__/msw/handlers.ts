import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { Conversation } from '@/types/conversation';
import { Message, ROLE } from '@/types/core';

const fakeConvos: Conversation[] = [
  { id: 'aaa', title: 'First', updatedAt: new Date().toISOString() },
  { id: 'bbb', title: 'Second', updatedAt: new Date().toISOString() },
];

const fakeMessages: Record<string, Message[]> = {
  'aaa': [
    { role: ROLE.USER, content: 'Hello', conversationId: 'aaa', createdAt: new Date().toISOString() },
    { role: ROLE.ASSISTANT, content: 'Hi there', conversationId: 'aaa', createdAt: new Date().toISOString() }
  ],
  'bbb': [
    { role: ROLE.USER, content: 'How are you?', conversationId: 'bbb', createdAt: new Date().toISOString() },
    { role: ROLE.ASSISTANT, content: 'I am fine, thanks', conversationId: 'bbb', createdAt: new Date().toISOString() }
  ]
};

console.log('🔧 Setting up MSW handlers');

export const handlers = [
  rest.get(/\/api\/conversations$/, (req, res, ctx) => {
    console.log('👍 MSW intercepted GET /api/conversations');
    return res(ctx.json(fakeConvos));
  }),

  rest.get(/\/api\/conversations\/([^\/]+)$/, (req, res, ctx) => {
    const url = req.url.toString();
    const id = url.split('/').pop() as string;
    
    console.log(`👍 MSW intercepted GET /api/conversations/${id}`);
    
    const convo = fakeConvos.find(c => c.id === id);
    if (!convo) {
      console.log(`❌ Conversation not found: ${id}`);
      return res(ctx.status(404));
    }
    
    return res(ctx.json(convo));
  }),

  rest.get(/\/api\/conversations\/([^\/]+)\/messages$/, (req, res, ctx) => {
    const url = req.url.toString();
    const parts = url.split('/');
    const id = parts[parts.length - 2];
    
    console.log(`👍 MSW intercepted GET /api/conversations/${id}/messages`);
    
    const messages = fakeMessages[id] || [];
    return res(ctx.json(messages));
  }),

  rest.post(/\/api\/conversations$/, async (req, res, ctx) => {
    console.log('👍 MSW intercepted POST /api/conversations');
    
    const data = await req.json();
    const newConvo: Conversation = {
      id: 'new-id',
      title: data.title || 'New chat',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    return res(ctx.json(newConvo));
  }),

  rest.post(/\/api\/chat\/stream$/, (req, res, ctx) => {
    console.log('👍 MSW intercepted POST /api/chat/stream');
    return res(ctx.status(200), ctx.text('stream-not-implemented-in-test'));
  })
];

export const server = setupServer(...handlers);

server.events.on('request:unhandled', (req) => {
  console.error('⚠️ MSW caught unhandled request:', req.method, req.url.href);
});

console.log('✅ MSW Server initialized with handlers'); 