package io.github.alexistrejo11.pimienta.module.operatingexpense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.ParsedAccessToken;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.OperatingExpenseAccessPolicy;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.OperatingExpenseManagementUseCasesImpl;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.CreateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.UpdateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseCategoryTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseDayTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseListQuery;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSummary;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptStorage;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseRepository;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.image.OperatingExpenseReceiptImageProcessor;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class OperatingExpenseManagementUseCasesImplTest {

  @Mock private OperatingExpenseRepository expenseRepository;
  @Mock private HeadquarterRepository headquarterRepository;
  @Mock private HeadquarterAccessService headquarterAccess;
  @Mock private OperatingExpenseReceiptStorage receiptStorage;

  private OperatingExpenseManagementUseCasesImpl useCases;
  private JwtAuthenticationContext admin;

  @BeforeEach
  void setUp() {
    useCases =
        new OperatingExpenseManagementUseCasesImpl(
            expenseRepository,
            headquarterRepository,
            new OperatingExpenseAccessPolicy(headquarterAccess),
            new OperatingExpenseReceiptImageProcessor(),
            receiptStorage);
    admin =
        new JwtAuthenticationContext(
            new ParsedAccessToken(8L, "admin@mail.com", "Ada", "Admin", "FEMALE", List.of("ADMIN"), List.of(), "jti"));
    when(headquarterAccess.isAdmin(any())).thenReturn(true);
  }

  private void headquarterExists() {
    when(headquarterRepository.findById(3L)).thenReturn(Optional.of(org.mockito.Mockito.mock(Headquarter.class)));
  }

  @Test
  void createDeletesNewKeysWhenSaveFails() throws IOException {
    headquarterExists();
    when(receiptStorage.storeJpeg(any(), anyLong(), any())).thenReturn("operating-expenses/3/2026-10/new.jpg");
    when(expenseRepository.save(any())).thenThrow(new RuntimeException("db down"));

    assertThrows(
        RuntimeException.class,
        () ->
            useCases.create(
                admin,
                new CreateOperatingExpenseCommand(
                    3L,
                    new BigDecimal("10.00"),
                    OperatingExpenseCategory.SUPPLIERS_MERCHANDISE,
                    "Hielo",
                    null,
                    LocalDate.of(2026, 10, 14),
                    List.of(jpegFile()))));

    verify(receiptStorage).deleteQuietly("operating-expenses/3/2026-10/new.jpg");
  }

  @Test
  void updateDeletesOnlyDroppedKeysAfterSave() throws IOException {
    OperatingExpense existing =
        OperatingExpense.builder()
            .withId(9L)
            .withHeadquarterId(3L)
            .withAmount(new BigDecimal("10.00"))
            .withConcept("Hielo")
            .withOccurredOn(LocalDate.of(2026, 10, 1))
            .withRecordedByUserId(8L)
            .withAttachmentKeys(List.of("keep.jpg", "drop.jpg"))
            .withVersion(1L)
            .reconstruct();
    headquarterExists();
    when(expenseRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(receiptStorage.storeJpeg(any(), anyLong(), any())).thenReturn("operating-expenses/3/2026-10/added.jpg");

    OperatingExpense saved =
        useCases.update(
            admin,
            9L,
            new UpdateOperatingExpenseCommand(
                3L,
                new BigDecimal("12.00"),
                OperatingExpenseCategory.SUPPLIERS_MERCHANDISE,
                "Hielo",
                "nota",
                LocalDate.of(2026, 10, 1),
                List.of("keep.jpg"),
                List.of(jpegFile())));

    assertEquals(List.of("keep.jpg", "operating-expenses/3/2026-10/added.jpg"), saved.getAttachmentKeys());
    verify(receiptStorage).deleteQuietly("drop.jpg");
    verify(receiptStorage, never()).deleteQuietly("keep.jpg");
    verify(receiptStorage, never()).deleteQuietly("operating-expenses/3/2026-10/added.jpg");
  }

  @Test
  void softDeleteDoesNotTouchStorage() {
    OperatingExpense existing =
        OperatingExpense.builder()
            .withId(9L)
            .withHeadquarterId(3L)
            .withAmount(new BigDecimal("10.00"))
            .withConcept("Hielo")
            .withOccurredOn(LocalDate.of(2026, 10, 1))
            .withRecordedByUserId(8L)
            .withAttachmentKeys(List.of("keep.jpg"))
            .withVersion(1L)
            .reconstruct();
    when(expenseRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    useCases.delete(admin, 9L);

    assertTrue(existing.getDeletedAt() != null);
    verify(receiptStorage, never()).deleteQuietly(any());
  }

  @Test
  void summarizeReturnsTheRepositoryTotal() {
    when(expenseRepository.sumAmount(any())).thenReturn(new BigDecimal("25.50"));
    when(expenseRepository.sumByDay(any())).thenReturn(List.of());
    when(expenseRepository.sumByCategory(any())).thenReturn(List.of());

    OperatingExpenseSummary summary =
        useCases.summarize(
            admin, new OperatingExpenseListQuery(null, 3L, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), null));

    assertEquals(new BigDecimal("25.50"), summary.totalAmount());
    assertEquals(null, summary.topCategory());
    assertTrue(summary.byDay().isEmpty());
  }

  @Test
  void summarizeKeepsRepositoryDayOrderAndBreaksCategoryTies() {
    LocalDate monday = LocalDate.of(2026, 10, 12);
    LocalDate tuesday = LocalDate.of(2026, 10, 13);
    when(expenseRepository.sumAmount(any())).thenReturn(new BigDecimal("40.00"));
    when(expenseRepository.sumByDay(any()))
        .thenReturn(
            List.of(
                new OperatingExpenseDayTotal(monday, new BigDecimal("30.00"), 2),
                new OperatingExpenseDayTotal(tuesday, new BigDecimal("10.00"), 1)));
    when(expenseRepository.sumByCategory(any()))
        .thenReturn(
            List.of(
                new OperatingExpenseCategoryTotal(
                    OperatingExpenseCategory.RENT, new BigDecimal("20.00"), 1),
                new OperatingExpenseCategoryTotal(
                    OperatingExpenseCategory.SUPPLIERS_MERCHANDISE, new BigDecimal("20.00"), 2)));

    OperatingExpenseSummary summary =
        useCases.summarize(admin, new OperatingExpenseListQuery("hielo", 3L, monday, tuesday, null));

    assertEquals(monday, summary.byDay().getFirst().occurredOn());
    assertEquals(OperatingExpenseCategory.SUPPLIERS_MERCHANDISE, summary.topCategory().category());
    assertEquals(2L, summary.topCategory().count());
  }

  private static MockMultipartFile jpegFile() throws IOException {
    BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, 8, 8);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    return new MockMultipartFile("attachments", "note.jpg", "image/jpeg", out.toByteArray());
  }
}
