jest.mock('@/app/components/chat/chatApiClient', () => ({
  chatApiClient: {
    fetchStreamingResponse: jest.fn()
  }
}));

import { createConversation } from '@/lib/services/chatService';

describe("createConversation", () => {
  beforeAll(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2023-01-01'));
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  it("creates a conversation object with the provided id and title", () => {
    const id = "123e4567-e89b-12d3-a456-426614174000";
    const title = "Test Conversation";
    
    const result = createConversation(id, title);
    
    expect(result).toEqual({
      id,
      title,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
  });
  
  it("uses ISO date format for createdAt and updatedAt", () => {
    const id = "123e4567-e89b-12d3-a456-426614174000";
    const title = "Test Conversation";
    
    const result = createConversation(id, title);
    
    const isoRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z$/;
    expect(result.createdAt).toMatch(isoRegex);
    expect(result.updatedAt).toMatch(isoRegex);
    
    const createdDate = new Date(result.createdAt!);
    const updatedDate = new Date(result.updatedAt!);
    expect(createdDate).toBeInstanceOf(Date);
    expect(updatedDate).toBeInstanceOf(Date);
    
    expect(createdDate.toISOString()).toBe(new Date('2023-01-01').toISOString());
    expect(updatedDate.toISOString()).toBe(new Date('2023-01-01').toISOString());
  });
}); 