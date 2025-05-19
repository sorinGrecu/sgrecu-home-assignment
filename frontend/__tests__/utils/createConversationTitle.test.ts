jest.mock('@/app/components/chat/chatApiClient', () => ({
  chatApiClient: {
    fetchStreamingResponse: jest.fn()
  }
}));

import { createConversationTitle } from '@/lib/services/chatService';

describe("createConversationTitle", () => {
  it("returns 'New chat' when message is undefined (TC-U001)", () => {
    expect(createConversationTitle(undefined)).toBe("New chat");
  });

  it("returns 'New chat' when message is empty string (TC-U001-B)", () => {
    expect(createConversationTitle("")).toBe("New chat");
  });

  it("returns the original message when less than 30 characters (TC-U002)", () => {
    const shortMessage = "short msg";
    expect(createConversationTitle(shortMessage)).toBe(shortMessage);
  });

  it("returns the full message when exactly 30 characters (TC-U003)", () => {
    const exactLengthMessage = "a".repeat(30);
    expect(createConversationTitle(exactLengthMessage)).toBe(exactLengthMessage);
  });

  it("returns truncated message with ellipsis when more than 30 characters (TC-U004)", () => {
    const longMessage = "a".repeat(31);
    const expected = "a".repeat(30) + "...";
    expect(createConversationTitle(longMessage)).toBe(expected);
  });
  
  it("trims whitespace before truncation", () => {
    expect(createConversationTitle("   Hello world")).toBe("Hello world");
    expect(createConversationTitle("Hello world   ")).toBe("Hello world");
    expect(createConversationTitle("   Hello world   ")).toBe("Hello world");
    const longMessageWithWhitespace = "   " + "a".repeat(30) + "   ";
    const expected = "a".repeat(30) + "...";
    expect(createConversationTitle(longMessageWithWhitespace)).toBe(expected);
  });
}); 