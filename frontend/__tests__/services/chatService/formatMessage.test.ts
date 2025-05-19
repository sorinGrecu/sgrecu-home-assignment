jest.mock('@/app/components/chat/chatApiClient', () => ({
  chatApiClient: {
    fetchStreamingResponse: jest.fn()
  }
}));

import { formatMessage } from '@/lib/services/chatService';
import { ROLE } from "@/types/core";

describe("formatMessage", () => {
  beforeAll(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2023-01-01'));
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  it("populates all fields correctly with undefined sessionId (TC-U021)", () => {
    const role = ROLE.USER;
    const content = "Hello world";
    
    const result = formatMessage(role, content);
    
    expect(result).toEqual({
      role: role,
      content: content,
      conversationId: "",
      createdAt: new Date().toISOString()
    });
  });

  it("populates all fields correctly with provided sessionId (TC-U022)", () => {
    const role = ROLE.ASSISTANT;
    const content = "Hello user";
    const sessionId = "123e4567-e89b-12d3-a456-426614174000";
    
    const result = formatMessage(role, content, sessionId);
    
    expect(result).toEqual({
      role: role,
      content: content,
      conversationId: sessionId,
      createdAt: new Date().toISOString()
    });
  });
}); 