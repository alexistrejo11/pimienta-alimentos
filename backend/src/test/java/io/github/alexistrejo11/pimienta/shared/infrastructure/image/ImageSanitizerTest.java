package io.github.alexistrejo11.pimienta.shared.infrastructure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Random;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.junit.jupiter.api.Test;

class ImageSanitizerTest {

  private static final String SECRET = "GPSLatitude PIMIENTA-SECRET-GPS";

  private final ImageSanitizer sanitizer = new ImageSanitizer();

  @Test
  void encodesJpegPngAndWebpAndShrinksANoisyPng() throws IOException {
    byte[] jpeg = sanitizer.sanitize(payload("image/jpeg", "a.jpg", jpeg(80, 40)), ImageSanitizer.RECEIPTS);
    byte[] png = sanitizer.sanitize(payload("image/png", "a.png", png(80, 40)), ImageSanitizer.RECEIPTS);
    byte[] webp = sanitizer.sanitize(payload("image/webp", "a.webp", jpeg), ImageSanitizer.RECEIPTS);
    assertWebp(jpeg);
    assertWebp(png);
    assertWebp(webp);

    byte[] noisy = noisyPng(640, 480);
    byte[] compressed = sanitizer.sanitize(payload("image/png", "noise.png", noisy), ImageSanitizer.RECEIPTS);
    assertWebp(compressed);
    assertTrue(compressed.length < noisy.length);
  }

  @Test
  void stripsExifGpsMarkerAndAppliesOrientation() throws IOException {
    byte[] marked = jpegWithExif(20, 10, 6);
    assertTrue(new String(marked, StandardCharsets.ISO_8859_1).contains(SECRET));
    byte[] webp = sanitizer.sanitize(payload("image/jpeg", "ticket.jpg", marked), ImageSanitizer.RECEIPTS);
    assertWebp(webp);
    assertFalse(new String(webp, StandardCharsets.ISO_8859_1).contains(SECRET));
    assertFalse(new String(webp, StandardCharsets.ISO_8859_1).contains("GPSLatitude"));
    BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(webp));
    assertEquals(10, decoded.getWidth());
    assertEquals(20, decoded.getHeight());
  }

  @Test
  void rejectsSpoofedBytesAndAShortPngSignature() {
    byte[] exe = new byte[] {0x4D, 0x5A, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
    BusinessValidationException spoofed =
        assertThrows(
            BusinessValidationException.class,
            () -> sanitizer.sanitize(payload("image/jpeg", "photo.jpg", exe), ImageSanitizer.RECEIPTS));
    assertEquals("UNSUPPORTED_TYPE", spoofed.context().get("reason"));

    byte[] text = "not-an-image".getBytes(StandardCharsets.UTF_8);
    BusinessValidationException plain =
        assertThrows(
            BusinessValidationException.class,
            () -> sanitizer.sanitize(payload("image/jpeg", "note.jpg", text), ImageSanitizer.RECEIPTS));
    assertEquals("UNSUPPORTED_TYPE", plain.context().get("reason"));

    byte[] shortPng = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
    BusinessValidationException incomplete =
        assertThrows(
            BusinessValidationException.class,
            () -> sanitizer.sanitize(payload("image/png", "x.png", shortPng), ImageSanitizer.RECEIPTS));
    assertEquals("UNSUPPORTED_TYPE", incomplete.context().get("reason"));
  }

  @Test
  void rejectsHugeHeaderWithoutDecodingTheRaster() {
    BusinessValidationException bomb =
        assertThrows(
            BusinessValidationException.class,
            () ->
                sanitizer.sanitize(
                    payload("image/png", "bomb.png", pngHeader(40_000, 40_000)), ImageSanitizer.RECEIPTS));
    assertEquals("DIMENSIONS_TOO_LARGE", bomb.context().get("reason"));
  }

  @Test
  void peopleProfileShrinksLargePhotosAndDoesNotUpscaleSmallOnes() throws IOException {
    BufferedImage large =
        ImageIO.read(
            new ByteArrayInputStream(
                sanitizer.sanitize(payload("image/jpeg", "big.jpg", jpeg(2000, 1000)), ImageSanitizer.PEOPLE)));
    assertEquals(800, large.getWidth());
    assertEquals(400, large.getHeight());

    BufferedImage small =
        ImageIO.read(
            new ByteArrayInputStream(
                sanitizer.sanitize(payload("image/png", "small.png", png(100, 40)), ImageSanitizer.PEOPLE)));
    assertEquals(100, small.getWidth());
    assertEquals(40, small.getHeight());
  }

  @Test
  void webpWriterComesFromWebpImageioAndReadersStayOnTwelveMonkeys() {
    assertTrue(ImageSanitizer.webpWriterSpiClassName().startsWith("com.luciad.imageio.webp."));
    Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/webp");
    assertTrue(readers.hasNext());
    while (readers.hasNext()) {
      ImageReader reader = readers.next();
      try {
        assertTrue(
            reader.getOriginatingProvider().getClass().getName().startsWith("com.twelvemonkeys.imageio.plugins.webp."));
      } finally {
        reader.dispose();
      }
    }
  }

  private static ImageUploadPayload payload(String type, String name, byte[] bytes) {
    return new ImageUploadPayload(bytes, type, name, bytes.length);
  }

  private static void assertWebp(byte[] bytes) {
    assertTrue(bytes.length >= 12);
    assertEquals((byte) 'R', bytes[0]);
    assertEquals((byte) 'I', bytes[1]);
    assertEquals((byte) 'F', bytes[2]);
    assertEquals((byte) 'F', bytes[3]);
    assertEquals((byte) 'W', bytes[8]);
    assertEquals((byte) 'E', bytes[9]);
    assertEquals((byte) 'B', bytes[10]);
    assertEquals((byte) 'P', bytes[11]);
  }

  private static byte[] jpeg(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, width / 2, height);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "jpg", out)) {
      throw new AssertionError("jpeg writer missing");
    }
    return out.toByteArray();
  }

  private static byte[] jpegWithExif(int width, int height, int orientation) throws IOException {
    byte[] raw = jpeg(width, height);
    byte[] app1 = exifOrientation(orientation);
    byte[] comment = comment(SECRET);
    byte[] out = new byte[2 + app1.length + comment.length + raw.length - 2];
    out[0] = (byte) 0xFF;
    out[1] = (byte) 0xD8;
    System.arraycopy(app1, 0, out, 2, app1.length);
    System.arraycopy(comment, 0, out, 2 + app1.length, comment.length);
    System.arraycopy(raw, 2, out, 2 + app1.length + comment.length, raw.length - 2);
    return out;
  }

  private static byte[] exifOrientation(int orientation) {
    byte[] tiff = new byte[26];
    tiff[0] = 'I';
    tiff[1] = 'I';
    tiff[2] = 42;
    tiff[4] = 8;
    tiff[8] = 1;
    tiff[10] = 0x12;
    tiff[11] = 0x01;
    tiff[12] = 3;
    tiff[14] = 1;
    tiff[18] = (byte) orientation;
    byte[] header = new byte[] {'E', 'x', 'i', 'f', 0, 0};
    int payload = header.length + tiff.length;
    byte[] app1 = new byte[4 + payload];
    app1[0] = (byte) 0xFF;
    app1[1] = (byte) 0xE1;
    int length = payload + 2;
    app1[2] = (byte) ((length >> 8) & 0xFF);
    app1[3] = (byte) (length & 0xFF);
    System.arraycopy(header, 0, app1, 4, header.length);
    System.arraycopy(tiff, 0, app1, 4 + header.length, tiff.length);
    return app1;
  }

  private static byte[] comment(String text) {
    byte[] body = text.getBytes(StandardCharsets.US_ASCII);
    byte[] segment = new byte[4 + body.length];
    segment[0] = (byte) 0xFF;
    segment[1] = (byte) 0xFE;
    int length = body.length + 2;
    segment[2] = (byte) ((length >> 8) & 0xFF);
    segment[3] = (byte) (length & 0xFF);
    System.arraycopy(body, 0, segment, 4, body.length);
    return segment;
  }

  private static byte[] png(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.GREEN);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "png", out)) {
      throw new AssertionError("png writer missing");
    }
    return out.toByteArray();
  }

  private static byte[] noisyPng(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Random random = new Random(7);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, random.nextInt(0xFFFFFF));
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "png", out)) {
      throw new AssertionError("png writer missing");
    }
    return out.toByteArray();
  }

  private static byte[] pngHeader(int width, int height) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    byte[] data = new byte[13];
    data[0] = (byte) (width >>> 24);
    data[1] = (byte) (width >>> 16);
    data[2] = (byte) (width >>> 8);
    data[3] = (byte) width;
    data[4] = (byte) (height >>> 24);
    data[5] = (byte) (height >>> 16);
    data[6] = (byte) (height >>> 8);
    data[7] = (byte) height;
    data[8] = 8;
    data[9] = 2;
    writeChunk(out, "IHDR", data);
    writeChunk(out, "IEND", new byte[0]);
    return out.toByteArray();
  }

  private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) {
    out.write((data.length >>> 24) & 0xFF);
    out.write((data.length >>> 16) & 0xFF);
    out.write((data.length >>> 8) & 0xFF);
    out.write(data.length & 0xFF);
    byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
    out.writeBytes(typeBytes);
    out.writeBytes(data);
    CRC32 crc = new CRC32();
    crc.update(typeBytes);
    crc.update(data);
    long value = crc.getValue();
    out.write((int) ((value >>> 24) & 0xFF));
    out.write((int) ((value >>> 16) & 0xFF));
    out.write((int) ((value >>> 8) & 0xFF));
    out.write((int) (value & 0xFF));
  }
}
