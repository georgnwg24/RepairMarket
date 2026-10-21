import {chatPreviewFormatted} from './date-helper';

describe('chatPreviewFormatted', () => {
  it('formats messages older than a month without an invalid locale error', () => {
    const older = new Date();
    older.setDate(older.getDate() - 40);
    expect(() => chatPreviewFormatted(older)).not.toThrow();
    expect(chatPreviewFormatted(older)).toMatch(/\d{2}\.\d{2}/);
  });
});
