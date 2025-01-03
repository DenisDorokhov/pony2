export function nullSafeNormalizedEquals(value1: string | undefined, value2: string | undefined): boolean {
  if (value1 == null && value2 == null) {
    return true;
  }
  if (value1 == null || value2 == null) {
    return false;
  }
  const normalizedValue1 = value1.trim().toLowerCase();
  const normalizedValue2 = value2.trim().toLowerCase();
  return normalizedValue1 === normalizedValue2;
}
