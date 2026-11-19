package io.github.alexistrejo11.pimienta.module.files.core.port.output;

import io.github.alexistrejo11.pimienta.module.files.core.application.NormalizedFile;

public interface FileContentNormalizer {

  NormalizedFile normalize(byte[] content, String declaredContentType, String originalFilename);
}
