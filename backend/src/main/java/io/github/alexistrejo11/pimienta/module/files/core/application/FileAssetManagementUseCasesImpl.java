package io.github.alexistrejo11.pimienta.module.files.core.application;

import io.github.alexistrejo11.pimienta.module.files.core.application.command.UploadFileCommand;
import io.github.alexistrejo11.pimienta.module.files.core.domain.FileAsset;
import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import io.github.alexistrejo11.pimienta.module.files.core.domain.exception.FileAssetNotFoundException;
import io.github.alexistrejo11.pimienta.module.files.core.port.input.FileAssetManagementUseCases;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileAssetRepository;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileContentNormalizer;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileStoragePort;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;

@Service
public class FileAssetManagementUseCasesImpl implements FileAssetManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(FileAssetManagementUseCasesImpl.class);

  private final FileAssetRepository fileAssetRepository;
  private final FileStoragePort fileStoragePort;
  private final FileContentNormalizer contentNormalizer;
  private final TransactionTemplate transactionTemplate;
  private final String sourcesRoot;
  private final long maxBytes;

  public FileAssetManagementUseCasesImpl(
      FileAssetRepository fileAssetRepository,
      FileStoragePort fileStoragePort,
      FileContentNormalizer contentNormalizer,
      PlatformTransactionManager transactionManager,
      @Value("${aws.s3.sources-root:pimienta/sources}") String sourcesRoot,
      @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
    this.fileAssetRepository = fileAssetRepository;
    this.fileStoragePort = fileStoragePort;
    this.contentNormalizer = contentNormalizer;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.sourcesRoot = sourcesRoot;
    this.maxBytes = maxFileSize.toBytes();
  }

  @Override
  public FileAsset upload(UploadFileCommand command) {
    log.info(
        "file upload start category={} module={} entityId={} uploader={}",
        command.category(),
        command.module(),
        command.entityId(),
        command.uploadedByUserId());

    validateUploadCommand(command);
    byte[] raw = readBytes(command);
    if (raw.length > maxBytes) {
      throw tooLarge(raw.length);
    }

    String originalFilename =
        command.file().getOriginalFilename() != null ? command.file().getOriginalFilename() : "file";
    NormalizedFile normalized =
        contentNormalizer.normalize(raw, command.file().getContentType(), originalFilename);
    String s3Key =
        FileAssetKeyBuilder.buildKey(
            sourcesRoot,
            command.category(),
            command.module(),
            command.entityId(),
            normalized.extension());

    fileStoragePort.store(normalized.content(), normalized.contentType(), s3Key);
    log.debug("file stored s3Key={}", s3Key);
    try {
      FileAsset saved =
          transactionTemplate.execute(
              status -> {
                FileAsset asset =
                    FileAsset.builder()
                        .category(command.category())
                        .module(command.module())
                        .entityType(command.entityType())
                        .entityId(command.entityId())
                        .s3Key(s3Key)
                        .originalName(originalFilename)
                        .contentType(normalized.contentType())
                        .fileSizeBytes((long) normalized.content().length)
                        .description(command.description())
                        .uploadedByUserId(command.uploadedByUserId())
                        .register();
                FileAsset stored = fileAssetRepository.save(asset);
                log.info("file asset saved id={} s3Key={}", stored.getId(), stored.getS3Key());
                return stored;
              });
      return saved;
    } catch (RuntimeException ex) {
      log.info("discarding file after failed save key={}", s3Key);
      fileStoragePort.deleteQuietly(s3Key);
      throw ex;
    }
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    log.info("file delete start id={}", id);
    FileAsset asset =
        fileAssetRepository.findActiveById(id).orElseThrow(() -> new FileAssetNotFoundException(id));
    fileStoragePort.delete(asset.getS3Key());
    fileAssetRepository.softDelete(id);
    log.info("file delete complete id={}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public String generateDownloadUrl(UUID id) {
    FileAsset asset =
        fileAssetRepository.findActiveById(id).orElseThrow(() -> new FileAssetNotFoundException(id));
    return fileStoragePort.presignedDownloadUrl(asset.getS3Key());
  }

  private void validateUploadCommand(UploadFileCommand command) {
    if (command.file() == null || command.file().isEmpty()) {
      throw new BusinessValidationException(
          "File must not be empty.", Map.of("field", "file"), "upload: empty file");
    }
    if (command.category() == null) {
      throw new BusinessValidationException(
          "Category is required.", Map.of("field", "category"), "upload: missing category");
    }
    if (command.category() == FileCategory.RESOURCE
        && (command.module() == null || command.module().isBlank())) {
      throw new BusinessValidationException(
          "Module is required for RESOURCE category.",
          Map.of("field", "module"),
          "upload: missing module for RESOURCE");
    }
    if (command.file().getSize() > maxBytes) {
      throw tooLarge(command.file().getSize());
    }
  }

  private BusinessValidationException tooLarge(long actual) {
    return new BusinessValidationException(
        "File size exceeds maximum of " + (maxBytes / (1024 * 1024)) + " MB.",
        Map.of("maxBytes", maxBytes, "actualBytes", actual),
        "upload: file too large size=" + actual);
  }

  private static byte[] readBytes(UploadFileCommand command) {
    try {
      return command.file().getBytes();
    } catch (IOException ex) {
      throw new BusinessValidationException(
          "No se pudo leer el archivo.", Map.of("reason", "UNREADABLE"), "upload read failed");
    }
  }
}
