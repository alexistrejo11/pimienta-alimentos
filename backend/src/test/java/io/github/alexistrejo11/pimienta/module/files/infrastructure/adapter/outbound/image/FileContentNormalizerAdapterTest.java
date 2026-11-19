package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.outbound.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexistrejo11.pimienta.module.files.core.application.NormalizedFile;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.infrastructure.image.ImageSanitizer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FileContentNormalizerAdapterTest {

  private final FileContentNormalizerAdapter normalizer =
      new FileContentNormalizerAdapter(new ImageSanitizer());

  @Test
  void rewritesJpegPngAndWebpAndLeavesOtherFilesUntouched() throws IOException {
    byte[] jpeg = jpeg();
    NormalizedFile fromJpeg = normalizer.normalize(jpeg, "image/jpeg", "ticket.jpg");
    assertEquals("webp", fromJpeg.extension());
    assertEquals("image/webp", fromJpeg.contentType());
    assertWebp(fromJpeg.content());

    NormalizedFile fromPng = normalizer.normalize(png(), "image/png", "logo.png");
    assertWebp(fromPng.content());

    NormalizedFile sniffed =
        normalizer.normalize(jpeg, "application/octet-stream", "secret-photo.bin");
    assertEquals("webp", sniffed.extension());
    assertWebp(sniffed.content());

    byte[] text = "hola".getBytes(StandardCharsets.UTF_8);
    NormalizedFile plain = normalizer.normalize(text, "text/plain", "notas.txt");
    assertEquals("txt", plain.extension());
    assertEquals("text/plain", plain.contentType());
    assertEquals("hola", new String(plain.content(), StandardCharsets.UTF_8));
  }

  @Test
  void rejectsAnExecutableDeclaredAsJpeg() {
    byte[] exe = new byte[] {0x4D, 0x5A, 0x00, 0x01};
    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class,
            () -> normalizer.normalize(exe, "image/jpeg", "photo.jpg"));
    assertEquals("UNSUPPORTED_TYPE", ex.context().get("reason"));
  }

  private static void assertWebp(byte[] bytes) {
    assertTrue(bytes.length >= 12);
    assertEquals((byte) 'R', bytes[0]);
    assertEquals((byte) 'W', bytes[8]);
    assertEquals((byte) 'E', bytes[9]);
    assertEquals((byte) 'B', bytes[10]);
    assertEquals((byte) 'P', bytes[11]);
    assertFalse(new String(bytes, StandardCharsets.ISO_8859_1).contains("ticket"));
  }

  private static byte[] jpeg() throws IOException {
    return draw("jpg");
  }

  private static byte[] png() throws IOException {
    return draw("png");
  }

  private static byte[] draw(String format) throws IOException {
    BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.ORANGE);
    graphics.fillRect(0, 0, 32, 24);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, format, out)) {
      throw new AssertionError(format);
    }
    return out.toByteArray();
  }
}
