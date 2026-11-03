import {ArtistBiographyDto, ImageFaceDto} from '../domain/library.dto';

export interface ArtistBiographyImageCrop {
  height: number;
  top: number;
  fadeStart: number;
}

export function cropArtistBiographyImage(
  imageWidth: number, imageHeight: number, displayWidth: number, targetHeight: number,
  faces: ImageFaceDto[] | undefined
): ArtistBiographyImageCrop | undefined {
  if (imageWidth <= 0 || imageHeight <= 0 || displayWidth <= 0 || !faces?.length || faces.some(face =>
    ![face.xMin, face.yMin, face.xMax, face.yMax].every(value => Number.isFinite(value) && value >= 0 && value <= 1) ||
    face.xMin >= face.xMax || face.yMin >= face.yMax)) {
    return undefined;
  }
  const fullHeight = displayWidth * imageHeight / imageWidth;
  const faceHeight = Math.max(...faces.map(face => (face.yMax - face.yMin) * fullHeight));
  const safeTop = Math.max(0, Math.min(...faces.map(face => face.yMin)) * fullHeight - faceHeight * .55);
  const safeBottom = Math.min(fullHeight, Math.max(...faces.map(face => face.yMax)) * fullHeight + faceHeight * .70);
  const fadeSpace = Math.min(66, displayWidth * .075, fullHeight - safeBottom);
  const height = Math.min(fullHeight, Math.max(targetHeight, safeBottom - safeTop + fadeSpace));
  const minTop = Math.max(0, safeBottom + fadeSpace - height);
  const maxTop = Math.min(safeTop, fullHeight - height);
  const preferredTop = (safeTop + safeBottom) / 2 - (height - fadeSpace) / 2;
  const top = Math.max(0, Math.min(maxTop, Math.max(minTop, preferredTop)));
  return {height, top, fadeStart: (height - fadeSpace) / height * 100};
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
