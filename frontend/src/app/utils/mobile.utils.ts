export function isMobileBrowser(): boolean {
  return (window as any).ponyIsMobile || false;
}
