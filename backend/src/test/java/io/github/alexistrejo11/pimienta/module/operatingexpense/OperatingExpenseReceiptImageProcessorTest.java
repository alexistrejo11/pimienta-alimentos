package io.github.alexistrejo11.pimienta.module.operatingexpense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.image.OperatingExpenseReceiptImageProcessor;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class OperatingExpenseReceiptImageProcessorTest {

  private final OperatingExpenseReceiptImageProcessor processor = new OperatingExpenseReceiptImageProcessor();

  @BeforeAll
  static void registerReaders() {
    ImageIO.scanForPlugins();
  }

  @Test
  void rejectsEmptyMismatchedOversizedAndTooManyFiles() throws IOException {
    BusinessValidationException empty =
        assertThrows(
            BusinessValidationException.class,
            () ->
                processor.validateAll(
                    List.of(new MockMultipartFile("attachments", "a.jpg", "image/jpeg", new byte[0])),
                    0));
    assertEquals("EMPTY_FILE", empty.context().get("reason"));

    byte[] png = png(16, 16, false);
    BusinessValidationException mismatch =
        assertThrows(
            BusinessValidationException.class,
            () -> processor.process(file("image/jpeg", "note.jpg", png)));
    assertEquals("UNSUPPORTED_TYPE", mismatch.context().get("reason"));

    BusinessValidationException huge =
        assertThrows(
            BusinessValidationException.class,
            () ->
                processor.validateAll(
                    List.of(file("image/jpeg", "big.jpg", new byte[(int) OperatingExpenseReceiptImageProcessor.MAX_FILE_BYTES + 1])),
                    0));
    assertEquals("FILE_TOO_LARGE", huge.context().get("reason"));

    int chunk = 3 * 1024 * 1024 + 1024;
    BusinessValidationException total =
        assertThrows(
            BusinessValidationException.class,
            () ->
                processor.validateAll(
                    List.of(
                        file("image/jpeg", "a.jpg", new byte[chunk]),
                        file("image/jpeg", "b.jpg", new byte[chunk])),
                    0));
    assertEquals("TOTAL_TOO_LARGE", total.context().get("reason"));

    List<MultipartFile> six = new ArrayList<>();
    byte[] jpeg = jpeg(8, 8);
    for (int i = 0; i < 6; i++) {
      six.add(file("image/jpeg", "r" + i + ".jpg", jpeg));
    }
    BusinessValidationException count =
        assertThrows(BusinessValidationException.class, () -> processor.validateAll(six, 0));
    assertEquals("TOO_MANY_ATTACHMENTS", count.context().get("reason"));
  }

  @Test
  void scalesDownOnlyWhenLongestSideExceeds1920() throws IOException {
    BufferedImage large = read(processor.process(file("image/jpeg", "large.jpg", jpeg(2000, 800))));
    assertEquals(1920, large.getWidth());
    assertEquals(768, large.getHeight());

    BufferedImage exact = read(processor.process(file("image/jpeg", "exact.jpg", jpeg(1920, 800))));
    assertEquals(1920, exact.getWidth());
    assertEquals(800, exact.getHeight());

    BufferedImage small = read(processor.process(file("image/png", "small.png", png(100, 40, false))));
    assertEquals(100, small.getWidth());
    assertEquals(40, small.getHeight());
    assertTrue((small.getRGB(0, 0) & 0xFFFFFF) != 0);
  }

  @Test
  void flattensTransparentPngOntoWhiteAndAcceptsWebp() throws IOException {
    byte[] jpeg = processor.process(file("image/png", "clear.png", png(24, 24, true)));
    assertTrue((jpeg[0] & 0xFF) == 0xFF && (jpeg[1] & 0xFF) == 0xD8);
    BufferedImage flat = read(jpeg);
    int rgb = flat.getRGB(0, 0);
    assertTrue((rgb >> 16 & 0xFF) > 250);
    assertTrue((rgb >> 8 & 0xFF) > 250);
    assertTrue((rgb & 0xFF) > 250);

    byte[] webp = webp();
    byte[] converted = processor.process(file("image/webp", "note.webp", webp));
    assertTrue((converted[0] & 0xFF) == 0xFF && (converted[1] & 0xFF) == 0xD8);
    BufferedImage decoded = read(converted);
    assertTrue(decoded.getWidth() > 0 && decoded.getHeight() > 0);
  }

  private static BufferedImage read(byte[] jpeg) throws IOException {
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
    if (image == null) {
      throw new AssertionError("output is not a readable image");
    }
    return image;
  }

  private static MultipartFile file(String type, String name, byte[] bytes) {
    return new MockMultipartFile("attachments", name, type, bytes);
  }

  private static byte[] jpeg(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "jpg", out)) {
      throw new AssertionError("jpeg writer missing");
    }
    return out.toByteArray();
  }

  private static byte[] png(int width, int height, boolean transparent) throws IOException {
    BufferedImage image =
        new BufferedImage(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    if (transparent) {
      graphics.setColor(new Color(0, 0, 0, 0));
    } else {
      graphics.setColor(Color.BLUE);
    }
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "png", out)) {
      throw new AssertionError("png writer missing");
    }
    return out.toByteArray();
  }

  private static byte[] webp() throws IOException {
    BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.GREEN);
    graphics.fillRect(0, 0, 8, 8);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (ImageIO.write(image, "webp", out) && out.size() > 12) {
      return out.toByteArray();
    }
    return Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
  }
}
