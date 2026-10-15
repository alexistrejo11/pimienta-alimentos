export const RECEIPT_MAX_EDGE = 1920;
export const RECEIPT_JPEG_QUALITY = 0.85;

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);

/** Longest side capped at `maxEdge`. Smaller images stay at their original size. */
export function scaledReceiptSize(
  width: number,
  height: number,
  maxEdge = RECEIPT_MAX_EDGE,
): { width: number; height: number } {
  const longest = Math.max(width, height);
  if (longest <= maxEdge || longest <= 0) {
    return { width, height };
  }
  const scale = maxEdge / longest;
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
}

function jpegName(name: string): string {
  const base = name.replace(/\.[^.]+$/, '') || 'comprobante';
  return `${base}.jpg`;
}

/**
 * Shrinks a receipt in the browser before upload.
 * Transparent pixels are painted on white and the result is JPEG at 0.85.
 */
export async function compressReceiptImage(file: File): Promise<File> {
  const type = (file.type || '').toLowerCase();
  if (!ALLOWED_TYPES.has(type)) {
    throw new Error('Solo se aceptan imágenes JPEG, PNG o WebP.');
  }
  if (typeof document === 'undefined') {
    throw new Error('La compresión de imágenes solo está disponible en el navegador.');
  }

  const bitmap = await createImageBitmap(file);
  try {
    const size = scaledReceiptSize(bitmap.width, bitmap.height);
    const canvas = document.createElement('canvas');
    canvas.width = size.width;
    canvas.height = size.height;
    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('No se pudo preparar la imagen.');
    }
    context.fillStyle = '#FFFFFF';
    context.fillRect(0, 0, size.width, size.height);
    context.drawImage(bitmap, 0, 0, size.width, size.height);
    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (result) => (result ? resolve(result) : reject(new Error('No se pudo comprimir la imagen.'))),
        'image/jpeg',
        RECEIPT_JPEG_QUALITY,
      );
    });
    return new File([blob], jpegName(file.name), { type: 'image/jpeg' });
  } finally {
    bitmap.close();
  }
}
