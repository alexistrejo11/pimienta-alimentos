package io.github.alexistrejo11.pimienta.shared.infrastructure.image;

import com.luciad.imageio.webp.WebPWriteParam;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.exception.ImageCodecException;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

/**
 * Validates magic bytes and header dimensions, then re-encodes a clean RGB raster to lossy WebP.
 * Metadata is not copied: the writer receives pixels only.
 *
 * <p>WebP bytes from the client are read only by the pure-Java TwelveMonkeys reader. The native
 * {@code libwebp} reader is deregistered. Encoding still uses the native {@code webp-imageio}
 * writer; if that writer is missing, the failure is an infrastructure error.
 */
@Component
public class ImageSanitizer {

  public static final int MAX_DIMENSION = 10_000;
  public static final long MAX_PIXELS = 25_000_000L;
  public static final Profile RECEIPTS = new Profile(1920, 0.78f, 5L * 1024 * 1024);
  public static final Profile PEOPLE = new Profile(800, 0.78f, 2L * 1024 * 1024);
  public static final Profile FILES = new Profile(1920, 0.78f, 10L * 1024 * 1024);

  private static final String WEBP_NATIVE_PACKAGE = "com.luciad.imageio.webp.";
  private static final String WEBP_JAVA_READER_PACKAGE = "com.twelvemonkeys.imageio.plugins.webp.";

  static {
    ImageIO.scanForPlugins();
    deregisterNativeWebpReaders();
    deregisterForeignWebpWriters();
  }

  public static boolean supportedRaster(byte[] bytes) {
    return bytes != null && detect(bytes) != null;
  }

  public void validate(ImageUploadPayload payload, Profile profile) {
    byte[] bytes = requireBytes(payload, profile);
    Kind kind = requireKind(bytes, payload.declaredContentType());
    dimensions(bytes, kind);
  }

  public byte[] sanitize(ImageUploadPayload payload, Profile profile) {
    byte[] bytes = requireBytes(payload, profile);
    Kind kind = requireKind(bytes, payload.declaredContentType());
    dimensions(bytes, kind);
    try {
      BufferedImage decoded = decode(bytes, kind);
      BufferedImage fitted = downscale(decoded, profile.maxEdgePx());
      BufferedImage rgb = flatten(fitted);
      return encodeWebp(rgb, profile.webpQuality());
    } catch (BusinessValidationException | ImageCodecException ex) {
      throw ex;
    } catch (IOException | RuntimeException ex) {
      throw unreadable();
    }
  }

  private static byte[] requireBytes(ImageUploadPayload payload, Profile profile) {
    if (payload == null || payload.isEmpty()) {
      throw invalid("La imagen está vacía.", "EMPTY_FILE", "empty image");
    }
    byte[] bytes = payload.content();
    long declared = Math.max(payload.size(), bytes.length);
    if (declared > profile.maxFileBytes() || bytes.length > profile.maxFileBytes()) {
      throw invalid(
          "La imagen debe pesar como máximo " + (profile.maxFileBytes() / (1024 * 1024)) + " MB.",
          "FILE_TOO_LARGE",
          "image bytes " + declared);
    }
    return bytes;
  }

  private static Kind requireKind(byte[] bytes, String declaredContentType) {
    Kind kind = detect(bytes);
    if (kind == null || !declaredMatches(declaredContentType, kind)) {
      throw invalid(
          "Solo se aceptan imágenes JPEG, PNG o WebP.",
          "UNSUPPORTED_TYPE",
          "contentType=" + mediaType(declaredContentType));
    }
    return kind;
  }

  private static void dimensions(byte[] bytes, Kind kind) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      if (input == null) {
        throw unreadable();
      }
      ImageReader reader = openReader(kind);
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        long pixels = (long) width * (long) height;
        if (width < 1
            || height < 1
            || width > MAX_DIMENSION
            || height > MAX_DIMENSION
            || pixels > MAX_PIXELS) {
          throw invalid(
              "La imagen supera las dimensiones permitidas.",
              "DIMENSIONS_TOO_LARGE",
              "dimensions " + width + "x" + height);
        }
      } finally {
        reader.dispose();
      }
    } catch (BusinessValidationException | ImageCodecException ex) {
      throw ex;
    } catch (IOException | RuntimeException ex) {
      throw unreadable();
    }
  }

  private static BufferedImage decode(byte[] bytes, Kind kind) throws IOException {
    if (kind == Kind.WEBP) {
      return readJavaWebp(bytes);
    }
    BufferedImage decoded =
        Thumbnails.of(new ByteArrayInputStream(bytes))
            .useExifOrientation(true)
            .scale(1)
            .asBufferedImage();
    if (decoded == null) {
      throw unreadable();
    }
    return decoded;
  }

  private static BufferedImage readJavaWebp(byte[] bytes) throws IOException {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      if (input == null) {
        throw unreadable();
      }
      ImageReader reader = openReader(Kind.WEBP);
      try {
        reader.setInput(input, true, true);
        BufferedImage image = reader.read(0);
        if (image == null) {
          throw unreadable();
        }
        return image;
      } finally {
        reader.dispose();
      }
    }
  }

  private static BufferedImage downscale(BufferedImage decoded, int maxEdge) throws IOException {
    int longest = Math.max(decoded.getWidth(), decoded.getHeight());
    if (longest <= maxEdge) {
      return decoded;
    }
    return Thumbnails.of(decoded).size(maxEdge, maxEdge).keepAspectRatio(true).asBufferedImage();
  }

  private static BufferedImage flatten(BufferedImage fitted) {
    BufferedImage rgb =
        new BufferedImage(fitted.getWidth(), fitted.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = rgb.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
      graphics.drawImage(fitted, 0, 0, null);
    } finally {
      graphics.dispose();
    }
    return rgb;
  }

  private static byte[] encodeWebp(BufferedImage image, float quality) throws IOException {
    ImageWriter writer = nativeWebpWriter();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
      WebPWriteParam param = new WebPWriteParam(writer.getLocale() == null ? Locale.ROOT : writer.getLocale());
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
      param.setCompressionQuality(quality);
      writer.setOutput(output);
      writer.write(null, new IIOImage(image, null, null), param);
      output.flush();
    } finally {
      writer.dispose();
    }
    return bytes.toByteArray();
  }

  public static String webpWriterSpiClassName() {
    ImageWriter writer = webpWriter();
    try {
      ImageWriterSpi spi = writer.getOriginatingProvider();
      return spi == null ? "" : spi.getClass().getName();
    } finally {
      writer.dispose();
    }
  }

  static ImageWriter webpWriter() {
    return nativeWebpWriter();
  }

  private static ImageWriter nativeWebpWriter() {
    try {
      Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
      while (writers.hasNext()) {
        ImageWriter writer = writers.next();
        ImageWriterSpi spi = writer.getOriginatingProvider();
        if (spi != null && spi.getClass().getName().startsWith(WEBP_NATIVE_PACKAGE)) {
          return writer;
        }
        writer.dispose();
      }
      throw new IllegalStateException("webp-imageio writer is not registered");
    } catch (IllegalStateException ex) {
      throw new ImageCodecException("webp-imageio writer is not registered", ex);
    }
  }

  private static ImageReader openReader(Kind kind) {
    deregisterNativeWebpReaders();
    Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName(kind));
    if (kind != Kind.WEBP) {
      if (!readers.hasNext()) {
        throw unreadable();
      }
      return readers.next();
    }
    while (readers.hasNext()) {
      ImageReader reader = readers.next();
      ImageReaderSpi spi = reader.getOriginatingProvider();
      if (spi != null && spi.getClass().getName().startsWith(WEBP_JAVA_READER_PACKAGE)) {
        return reader;
      }
      reader.dispose();
    }
    throw new ImageCodecException("twelvemonkeys webp reader is not registered", null);
  }

  private static void deregisterNativeWebpReaders() {
    IIORegistry registry = IIORegistry.getDefaultInstance();
    // The registry loads SPI services on the first lookup. Drain that lookup so the native
    // reader is present before we remove it; doing this only from the static initializer
    // races the lazy registry and leaves libwebp registered.
    Iterator<ImageReader> primed = ImageIO.getImageReadersByMIMEType("image/webp");
    while (primed.hasNext()) {
      primed.next().dispose();
    }
    Iterator<ImageReaderSpi> providers = registry.getServiceProviders(ImageReaderSpi.class, false);
    List<ImageReaderSpi> nativeReaders = new ArrayList<>();
    while (providers.hasNext()) {
      ImageReaderSpi spi = providers.next();
      if (spi.getClass().getName().startsWith(WEBP_NATIVE_PACKAGE)) {
        nativeReaders.add(spi);
      }
    }
    for (ImageReaderSpi spi : nativeReaders) {
      registry.deregisterServiceProvider(spi);
    }
  }

  private static void deregisterForeignWebpWriters() {
    IIORegistry registry = IIORegistry.getDefaultInstance();
    Iterator<ImageWriterSpi> providers = registry.getServiceProviders(ImageWriterSpi.class, false);
    List<ImageWriterSpi> foreign = new ArrayList<>();
    while (providers.hasNext()) {
      ImageWriterSpi spi = providers.next();
      if (isWebpWriter(spi) && !spi.getClass().getName().startsWith(WEBP_NATIVE_PACKAGE)) {
        foreign.add(spi);
      }
    }
    for (ImageWriterSpi spi : foreign) {
      registry.deregisterServiceProvider(spi);
    }
  }

  private static boolean isWebpWriter(ImageWriterSpi spi) {
    String[] mimes = spi.getMIMETypes();
    if (mimes == null) {
      return false;
    }
    for (String mime : mimes) {
      if ("image/webp".equalsIgnoreCase(mime)) {
        return true;
      }
    }
    return false;
  }

  private static Kind detect(byte[] bytes) {
    if (isJpeg(bytes)) {
      return Kind.JPEG;
    }
    if (isPng(bytes)) {
      return Kind.PNG;
    }
    if (isWebp(bytes)) {
      return Kind.WEBP;
    }
    return null;
  }

  private static boolean isJpeg(byte[] bytes) {
    return bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xFF
        && (bytes[1] & 0xFF) == 0xD8
        && (bytes[2] & 0xFF) == 0xFF;
  }

  static boolean isPng(byte[] bytes) {
    return bytes.length >= 8
        && (bytes[0] & 0xFF) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4E
        && bytes[3] == 0x47
        && bytes[4] == 0x0D
        && bytes[5] == 0x0A
        && bytes[6] == 0x1A
        && bytes[7] == 0x0A;
  }

  private static boolean isWebp(byte[] bytes) {
    if (bytes.length < 12
        || bytes[0] != 'R'
        || bytes[1] != 'I'
        || bytes[2] != 'F'
        || bytes[3] != 'F'
        || bytes[8] != 'W'
        || bytes[9] != 'E'
        || bytes[10] != 'B'
        || bytes[11] != 'P') {
      return false;
    }
    long declared =
        (bytes[4] & 0xFFL)
            | ((bytes[5] & 0xFFL) << 8)
            | ((bytes[6] & 0xFFL) << 16)
            | ((bytes[7] & 0xFFL) << 24);
    return declared == bytes.length - 8L;
  }

  private static boolean declaredMatches(String contentType, Kind kind) {
    String type = mediaType(contentType);
    if (type.isEmpty()) {
      return true;
    }
    return switch (kind) {
      case JPEG -> type.equals("image/jpeg") || type.equals("image/jpg");
      case PNG -> type.equals("image/png");
      case WEBP -> type.equals("image/webp");
    };
  }

  private static String mediaType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return "";
    }
    int separator = contentType.indexOf(';');
    String type = separator >= 0 ? contentType.substring(0, separator) : contentType;
    return type.trim().toLowerCase(Locale.ROOT);
  }

  private static String formatName(Kind kind) {
    return switch (kind) {
      case JPEG -> "JPEG";
      case PNG -> "png";
      case WEBP -> "webp";
    };
  }

  private static BusinessValidationException invalid(String message, String reason, String log) {
    return new BusinessValidationException(message, Map.of("reason", reason), log);
  }

  private static BusinessValidationException unreadable() {
    return invalid("No se pudo leer la imagen.", "UNREADABLE", "image decode failed");
  }

  private enum Kind {
    JPEG,
    PNG,
    WEBP
  }

  public record Profile(int maxEdgePx, float webpQuality, long maxFileBytes) {
    public Profile {
      if (maxEdgePx < 1 || webpQuality < 0f || webpQuality > 1f || maxFileBytes < 1) {
        throw new IllegalArgumentException("invalid image profile");
      }
    }
  }
}
