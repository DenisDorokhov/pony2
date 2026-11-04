import {LinkifyIt} from 'linkify-it';
import {ArtistBiographyDto, ImageFaceDto} from '../domain/library.dto';

const BIOGRAPHY_LINKIFY = new LinkifyIt({fuzzyLink: true}).add('ftp:', null).add('//', null);

export interface ArtistBiographyTextPart {
  text: string;
  href?: string;
}

export function formatArtistBiographyText(text: string | undefined): ArtistBiographyTextPart[][] {
  text = text?.replace(/\\n/gu, '');
  if (!text || !/\p{L}/u.test(text)) {
    return [];
  }
  return text.split(/[\r\n\u2028\u2029]+/u).map(paragraph => paragraph.trim()).filter(Boolean).map(paragraph => {
    const parts: ArtistBiographyTextPart[] = [];
    let offset = 0;
    // Keep original offsets while treating bullets and invisible separators as link boundaries.
    const textForLinks = paragraph.replace(/[\u2022\u2063]/gu, ' ');
    for (const match of BIOGRAPHY_LINKIFY.match(textForLinks) ?? []) {
      if (match.index > offset) {
        parts.push({text: paragraph.slice(offset, match.index)});
      }
      parts.push({text: paragraph.slice(match.index, match.lastIndex),
        href: match.schema ? match.url : `https://${match.raw}`});
      offset = match.lastIndex;
    }
    if (offset < paragraph.length) {
      parts.push({text: paragraph.slice(offset)});
    }
    return parts;
  });
}

export interface ArtistBiographyImageCrop {
  height: number;
  top: number;
  width: number;
  fadeStart: number;
}

export function cropArtistBiographyImage(
  imageWidth: number, imageHeight: number, displayWidth: number, targetHeight: number, maxHeight: number,
  faces: ImageFaceDto[] | undefined
): ArtistBiographyImageCrop | undefined {
  if (imageWidth <= 0 || imageHeight <= 0 || displayWidth <= 0 || targetHeight <= 0 || maxHeight <= 0) {
    return undefined;
  }
  const originalHeight = displayWidth * imageHeight / imageWidth;
  const fadeSpace = Math.min(32, displayWidth * .06, maxHeight * .15);
  if (!faces?.length || faces.some(face =>
    ![face.xMin, face.yMin, face.xMax, face.yMax].every(value => Number.isFinite(value) && value >= 0 && value <= 1) ||
    face.xMin >= face.xMax || face.yMin >= face.yMax)) {
    const height = Math.min(maxHeight, originalHeight);
    return {height, top: (originalHeight - height) / 2, width: displayWidth,
      fadeStart: Math.max(0, (height - fadeSpace) / height * 100)};
  }
  const originalFaceTop = Math.min(...faces.map(face => face.yMin)) * originalHeight;
  const originalFaceBottom = Math.max(...faces.map(face => face.yMax)) * originalHeight;
  const scale = Math.min(1, (maxHeight - fadeSpace) / (originalFaceBottom - originalFaceTop));
  const width = displayWidth * scale;
  const fullHeight = originalHeight * scale;
  const faceTop = originalFaceTop * scale;
  const faceBottom = originalFaceBottom * scale;
  const faceHeight = Math.max(...faces.map(face => (face.yMax - face.yMin) * fullHeight));
  const topMargin = Math.min(40, faceHeight * .25, faceTop);
  const bottomMargin = Math.min(32, faceHeight * .20, fullHeight - faceBottom);
  const marginSpace = Math.max(0, maxHeight - (faceBottom - faceTop) - fadeSpace);
  const marginScale = topMargin + bottomMargin > 0 ? Math.min(1, marginSpace / (topMargin + bottomMargin)) : 0;
  const safeTop = faceTop - topMargin * marginScale;
  const safeBottom = faceBottom + bottomMargin * marginScale;
  const height = Math.min(maxHeight, Math.max(Math.min(targetHeight, fullHeight), safeBottom - safeTop + fadeSpace));
  const minTop = Math.max(0, safeBottom + fadeSpace - height);
  const maxTop = Math.min(safeTop, Math.max(0, fullHeight + fadeSpace - height));
  const preferredTop = (safeTop + safeBottom) / 2 - (height - fadeSpace) / 2;
  const top = Math.max(0, Math.min(maxTop, Math.max(minTop, preferredTop)));
  return {height, top, width, fadeStart: (height - fadeSpace) / height * 100};
}

export const ARTIST_BIOGRAPHY_LINKS: Record<ArtistBiographyDto.LinkType, {label: string; icon: string}> = {
  INSTAGRAM: {label: 'Instagram', icon: 'bi-instagram'},
  FACEBOOK: {label: 'Facebook', icon: 'bi-facebook'},
  X: {label: 'X', icon: 'bi-twitter-x'},
  TIKTOK: {label: 'TikTok', icon: 'bi-tiktok'},
  WIKIPEDIA: {label: 'Wikipedia', icon: 'bi-wikipedia'},
  WHATSAPP: {label: 'WhatsApp', icon: 'bi-whatsapp'},
  WEBSITE: {label: 'Website', icon: 'bi-globe'},
};

export function artistBiographyImageColor(image: HTMLImageElement): number[] | undefined {
  try {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 24;
    const context = canvas.getContext('2d');
    if (!context) {
      return undefined;
    }
    context.drawImage(image, 0, 0, 24, 24);
    const pixels = context.getImageData(0, 0, 24, 24).data;
    const buckets = new Map<string, {count: number; channels: number[]}>();
    for (let index = 0; index < pixels.length; index += 4) {
      const rgb = [pixels[index], pixels[index + 1], pixels[index + 2]];
      const brightness = (rgb[0] + rgb[1] + rgb[2]) / 3;
      if (brightness < 28 || brightness > 235) {
        continue;
      }
      const key = rgb.map(channel => Math.floor(channel / 32)).join(',');
      const bucket = buckets.get(key) ?? {count: 0, channels: [0, 0, 0]};
      bucket.count++;
      rgb.forEach((channel, index) => bucket.channels[index] += channel);
      buckets.set(key, bucket);
    }
    const bucket = [...buckets.values()].sort((a, b) => b.count - a.count)[0];
    return bucket?.channels.map(channel => channel / bucket.count);
  } catch {
    return undefined;
  }
}
