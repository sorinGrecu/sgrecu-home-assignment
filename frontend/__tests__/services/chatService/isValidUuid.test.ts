jest.mock('uuid', () => ({
  validate: (uuid: string) => {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }
}));

jest.mock('@/app/components/chat/chatApiClient', () => ({
  chatApiClient: {
    fetchStreamingResponse: jest.fn()
  }
}));

import { isValidUuid } from '@/lib/services/chatService';

describe("isValidUuid", () => {
  it("accepts a valid v4 UUID (TC-U011)", () => {
    const validUuid = "123e4567-e89b-12d3-a456-426614174000";
    expect(isValidUuid(validUuid)).toBe(true);
  });
  
  it("rejects invalid inputs (TC-U012)", () => {
    expect(isValidUuid("not-a-uuid")).toBe(false);
    expect(isValidUuid("")).toBe(false);
    expect(isValidUuid(undefined)).toBe(false);
    expect(isValidUuid("123e4567-e89b-12d3-a456")).toBe(false);
    expect(isValidUuid("123e4567-e89b-12d3-a456-42661417400g")).toBe(false);
  });
}); 