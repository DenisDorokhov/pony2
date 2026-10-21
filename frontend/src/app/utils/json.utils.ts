import {applyEdits, createScanner, format, JSONScanner, ParseError, parseTree, SyntaxKind} from 'jsonc-parser';

export interface JsonPart {
  text: string;
  className: string;
  href?: string;
}

export function formatJsonParts(value: string): JsonPart[] {
  const errors: ParseError[] = [];
  parseTree(value, errors, {disallowComments: true, allowTrailingComma: false});
  if (errors.length > 0) {
    return linkify(value, '');
  }
  const formatted: string = applyEdits(value, format(value, undefined, {
    tabSize: 2,
    insertSpaces: true,
    eol: '\n'
  }));
  const scanner: JSONScanner = createScanner(formatted, true);
  const parts: JsonPart[] = [];
  let previousEnd = 0;
  let kind: SyntaxKind = scanner.scan();
  while (kind !== SyntaxKind.EOF) {
    const offset: number = scanner.getTokenOffset();
    const end: number = offset + scanner.getTokenLength();
    const text: string = formatted.slice(offset, end);
    const decoded: string = scanner.getTokenValue();
    const nextKind: SyntaxKind = scanner.scan();
    if (offset > previousEnd) {
      parts.push({text: formatted.slice(previousEnd, offset), className: ''});
    }
    if (kind === SyntaxKind.StringLiteral) {
      if (nextKind === SyntaxKind.ColonToken) {
        parts.push({text, className: 'hljs-attr'});
      } else {
        parts.push(...linkify('"' + decoded + '"', 'hljs-string'));
      }
    } else {
      let className = '';
      if (kind === SyntaxKind.NumericLiteral) {
        className = 'hljs-number';
      } else if (kind === SyntaxKind.TrueKeyword || kind === SyntaxKind.FalseKeyword || kind === SyntaxKind.NullKeyword) {
        className = 'hljs-literal';
      }
      parts.push({text, className});
    }
    previousEnd = end;
    kind = nextKind;
  }
  if (previousEnd < formatted.length) {
    parts.push({text: formatted.slice(previousEnd), className: ''});
  }
  return parts;
}

function linkify(text: string, className: string): JsonPart[] {
  const parts: JsonPart[] = [];
  const pattern = /\bhttps?:\/\/[^\s<>"'\\]+/giu;
  let offset = 0;
  for (const match of text.matchAll(pattern)) {
    const href: string = trimUrl(match[0]);
    try {
      const url: URL = new URL(href);
      if (url.protocol !== 'http:' && url.protocol !== 'https:') {
        continue;
      }
    } catch {
      continue;
    }
    if (match.index > offset) {
      parts.push({text: text.slice(offset, match.index), className});
    }
    parts.push({text: href, className, href});
    offset = match.index + href.length;
  }
  if (offset < text.length) {
    parts.push({text: text.slice(offset), className});
  }
  return parts;
}

function trimUrl(value: string): string {
  let url: string = value;
  while (/[.,;:!?]$/u.test(url) || ['()', '[]', '{}'].some(pair =>
    url.endsWith(pair[1]) && url.split(pair[1]).length > url.split(pair[0]).length
  )) {
    url = url.slice(0, -1);
  }
  return url;
}
