import { RiskLevelPipe, RiskColorPipe } from './risk-level.pipe';

describe('RiskLevelPipe', () => {
  const pipe = new RiskLevelPipe();

  it('should return LOW for score < 40', () => {
    expect(pipe.transform(20)).toBe('LOW');
    expect(pipe.transform(0)).toBe('LOW');
  });

  it('should return MODERATE for score 40-59', () => {
    expect(pipe.transform(40)).toBe('MODERATE');
    expect(pipe.transform(59)).toBe('MODERATE');
  });

  it('should return HIGH for score 60-79', () => {
    expect(pipe.transform(60)).toBe('HIGH');
    expect(pipe.transform(79)).toBe('HIGH');
  });

  it('should return CRITICAL for score >= 80', () => {
    expect(pipe.transform(80)).toBe('CRITICAL');
    expect(pipe.transform(100)).toBe('CRITICAL');
  });
});

describe('RiskColorPipe', () => {
  const pipe = new RiskColorPipe();

  it('should return green for low risk', () => {
    expect(pipe.transform(20)).toBe('#388e3c');
  });

  it('should return red for critical risk', () => {
    expect(pipe.transform(85)).toBe('#d32f2f');
  });
});
