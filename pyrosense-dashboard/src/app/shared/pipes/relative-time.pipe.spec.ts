import { RelativeTimePipe } from './relative-time.pipe';

describe('RelativeTimePipe', () => {
  const pipe = new RelativeTimePipe();

  it('should return empty string for null', () => {
    expect(pipe.transform(null)).toBe('');
  });

  it('should return "A l\'instant" for very recent', () => {
    const now = new Date().toISOString();
    expect(pipe.transform(now)).toBe("A l'instant");
  });

  it('should return minutes for recent past', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60000).toISOString();
    expect(pipe.transform(fiveMinAgo)).toBe('Il y a 5 min');
  });

  it('should return hours for same day', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 3600000).toISOString();
    expect(pipe.transform(twoHoursAgo)).toBe('Il y a 2h');
  });

  it('should return days for same week', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 86400000).toISOString();
    expect(pipe.transform(threeDaysAgo)).toBe('Il y a 3j');
  });
});
