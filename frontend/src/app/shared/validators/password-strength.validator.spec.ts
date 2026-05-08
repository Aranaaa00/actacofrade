import { passwordStrength } from './password-strength.validator';
import { FormControl } from '@angular/forms';

describe('passwordStrength validator', () => {
  function check(value: string) {
    return passwordStrength(new FormControl(value));
  }

  it('returns null for a strong password', () => {
    expect(check('Abcdef1!')).toBeNull();
  });

  it('flags too short passwords', () => {
    expect(check('Ab1!')!['tooShort']).toBe(true);
  });

  it('flags too long passwords', () => {
    const tooLong = 'A'.repeat(60) + 'a'.repeat(50) + '1!';
    expect(check(tooLong)!['tooLong']).toBe(true);
  });

  it('flags missing uppercase', () => {
    expect(check('abcdef1!')!['noUppercase']).toBe(true);
  });

  it('flags missing lowercase', () => {
    expect(check('ABCDEF1!')!['noLowercase']).toBe(true);
  });

  it('flags missing digit', () => {
    expect(check('Abcdefg!')!['noDigit']).toBe(true);
  });

  it('flags missing special character', () => {
    expect(check('Abcdefg1')!['noSpecial']).toBe(true);
  });

  it('flags invalid characters', () => {
    expect(check('Abcdef1! ')!['invalidChars']).toBe(true);
  });

  it('returns multiple errors for empty value', () => {
    const result = check('');
    expect(result).not.toBeNull();
    expect(result!['tooShort']).toBe(true);
  });
});
