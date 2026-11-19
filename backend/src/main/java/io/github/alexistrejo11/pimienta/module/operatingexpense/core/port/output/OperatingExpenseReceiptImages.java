package io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output;

import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.util.List;

public interface OperatingExpenseReceiptImages {

  void validateAll(List<ImageUploadPayload> files, int retainedCount);

  byte[] process(ImageUploadPayload file);
}
