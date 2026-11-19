package io.github.alexistrejo11.pimienta.module.files.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.alexistrejo11.pimienta.module.files.core.application.command.UploadFileCommand;
import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileAssetRepository;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileContentNormalizer;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class FileAssetManagementUseCasesImplTest {

  @Mock private FileAssetRepository repository;
  @Mock private FileStoragePort storage;
  @Mock private FileContentNormalizer normalizer;

  @Test
  void discardsTheObjectWhenSaveFails() {
    when(normalizer.normalize(any(), any(), any()))
        .thenReturn(new NormalizedFile("hola".getBytes(), "text/plain", "txt"));
    when(repository.save(any())).thenThrow(new RuntimeException("db down"));
    FileAssetManagementUseCasesImpl useCases = useCases();

    assertThrows(
        RuntimeException.class,
        () ->
            useCases.upload(
                new UploadFileCommand(
                    new MockMultipartFile("file", "secret-invoice.txt", "text/plain", "hola".getBytes()),
                    FileCategory.TEMPLATE,
                    null,
                    null,
                    null,
                    null,
                    4L)));

    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    verify(storage).store(any(), anyString(), key.capture());
    assertTrue(key.getValue().endsWith(".txt"));
    assertFalse(key.getValue().contains("secret-invoice"));
    verify(storage).deleteQuietly(key.getValue());
  }

  @Test
  void keepsTheObjectWhenSaveSucceeds() {
    when(normalizer.normalize(any(), any(), any()))
        .thenReturn(new NormalizedFile(new byte[] {1, 2}, "image/webp", "webp"));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    FileAssetManagementUseCasesImpl useCases = useCases();

    var saved =
        useCases.upload(
            new UploadFileCommand(
                new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[] {1}),
                FileCategory.COMPANY,
                null,
                null,
                null,
                null,
                4L));

    assertTrue(saved.getS3Key().endsWith(".webp"));
    assertEquals(saved.getContentType(), "image/webp");
    verify(storage, never()).deleteQuietly(any());
  }

  private FileAssetManagementUseCasesImpl useCases() {
    return new FileAssetManagementUseCasesImpl(
        repository, storage, normalizer, tx(), "pimienta/sources", DataSize.ofMegabytes(10));
  }

  private static PlatformTransactionManager tx() {
    return new AbstractPlatformTransactionManager() {
      @Override
      protected Object doGetTransaction() {
        return new Object();
      }

      @Override
      protected void doBegin(Object transaction, TransactionDefinition definition) {}

      @Override
      protected void doCommit(DefaultTransactionStatus status) {}

      @Override
      protected void doRollback(DefaultTransactionStatus status) {}
    };
  }
}
