package io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface OperatingExpenseReceiptImages {

  void validateAll(List<MultipartFile> files, int retainedCount);

  byte[] process(MultipartFile file);
}
