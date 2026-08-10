// Client-side compression for camera captures. Field photos routinely arrive
// as 8-12MB JPEGs at 4000px+; none of that resolution is needed to document
// site progress, and shipping it over a spotty site connection is the actual
// bottleneck this module exists to remove. Everything here runs off a single
// canvas draw, so it stays cheap enough for the main thread on mid-range
// Android hardware without needing a dedicated worker.

export interface CompressImageOptions {
  /** Longest edge in pixels; the other edge scales to preserve aspect ratio. */
  maxDimension?: number;
  /** 0-1 encoder quality. */
  quality?: number;
  /** Force a specific output format instead of auto-detecting WebP support. */
  mimeType?: 'image/webp' | 'image/jpeg';
}

export interface CompressedImage {
  blob: Blob;
  contentType: string;
  width: number;
  height: number;
  byteSize: number;
  originalByteSize: number;
}

const DEFAULT_MAX_DIMENSION = 1920;
const DEFAULT_QUALITY = 0.75;

type Canvas2DContext = CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D;
type AnyCanvas = OffscreenCanvas | HTMLCanvasElement;
type DecodedImage = ImageBitmap | HTMLImageElement;

// Cached across calls - the answer can't change mid-session and re-probing
// per photo would mean an extra canvas encode on every single capture.
let webpEncodeSupported: boolean | null = null;

async function supportsWebPEncoding(): Promise<boolean> {
  if (webpEncodeSupported !== null) return webpEncodeSupported;
  try {
    const probe = document.createElement('canvas');
    probe.width = 1;
    probe.height = 1;
    const blob = await new Promise<Blob | null>((resolve) => probe.toBlob(resolve, 'image/webp'));
    webpEncodeSupported = blob !== null && blob.type === 'image/webp';
  } catch {
    webpEncodeSupported = false;
  }
  return webpEncodeSupported;
}

function decodeViaImageElement(source: Blob): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(source);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Failed to decode image for compression'));
    };
    img.src = url;
  });
}

// createImageBitmap is preferred: it's typically hardware-decoded and
// `imageOrientation: 'from-image'` bakes in EXIF rotation up front, so we
// never end up compressing (and later displaying) a sideways site photo.
// Safari < 15 and some WebViews lack it, hence the <img> fallback.
async function decodeImage(source: Blob): Promise<DecodedImage> {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(source, { imageOrientation: 'from-image' });
    } catch {
      // Some browsers advertise createImageBitmap but choke on certain
      // camera JPEGs (odd ICC profiles) - fall through to <img> decoding.
    }
  }
  return decodeViaImageElement(source);
}

function computeTargetSize(
  width: number,
  height: number,
  maxDimension: number,
): { width: number; height: number } {
  if (width <= maxDimension && height <= maxDimension) {
    return { width, height };
  }
  const scale = maxDimension / Math.max(width, height);
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
}

function drawToCanvas(source: DecodedImage, width: number, height: number): AnyCanvas {
  const canvas: AnyCanvas =
    typeof OffscreenCanvas !== 'undefined'
      ? new OffscreenCanvas(width, height)
      : Object.assign(document.createElement('canvas'), { width, height });

  const ctx = canvas.getContext('2d') as Canvas2DContext | null;
  if (!ctx) throw new Error('2D canvas context unavailable');
  ctx.drawImage(source, 0, 0, width, height);
  return canvas;
}

function encodeCanvas(canvas: AnyCanvas, contentType: string, quality: number): Promise<Blob> {
  if (canvas instanceof HTMLCanvasElement) {
    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => (blob ? resolve(blob) : reject(new Error('Canvas encoding failed'))),
        contentType,
        quality,
      );
    });
  }
  return canvas.convertToBlob({ type: contentType, quality });
}

/**
 * Resizes and re-encodes a camera JPEG/PNG into a WebP (or JPEG, on browsers
 * that can't encode WebP) blob suitable for upload over a slow connection.
 */
export async function compressImage(
  file: File | Blob,
  options: CompressImageOptions = {},
): Promise<CompressedImage> {
  const maxDimension = options.maxDimension ?? DEFAULT_MAX_DIMENSION;
  const quality = options.quality ?? DEFAULT_QUALITY;

  const decoded = await decodeImage(file);
  const { width, height } = computeTargetSize(decoded.width, decoded.height, maxDimension);
  const canvas = drawToCanvas(decoded, width, height);

  if (decoded instanceof ImageBitmap) decoded.close();

  const useWebP = options.mimeType !== 'image/jpeg' && (await supportsWebPEncoding());
  const contentType = useWebP ? 'image/webp' : 'image/jpeg';
  const blob = await encodeCanvas(canvas, contentType, quality);

  return {
    blob,
    contentType,
    width,
    height,
    byteSize: blob.size,
    originalByteSize: file.size,
  };
}
