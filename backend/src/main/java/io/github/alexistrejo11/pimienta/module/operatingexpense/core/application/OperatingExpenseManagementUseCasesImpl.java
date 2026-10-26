package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.CreateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.UpdateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseListQuery;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSearchCriteria;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSummary;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.exception.OperatingExpenseNotFoundException;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.input.OperatingExpenseManagementUseCases;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptImages;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptStorage;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OperatingExpenseManagementUseCasesImpl implements OperatingExpenseManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(OperatingExpenseManagementUseCasesImpl.class);

  private final OperatingExpenseRepository expenseRepository;
  private final HeadquarterRepository headquarterRepository;
  private final OperatingExpenseAccessPolicy accessPolicy;
  private final OperatingExpenseReceiptImages imageProcessor;
  private final OperatingExpenseReceiptStorage receiptStorage;

  public OperatingExpenseManagementUseCasesImpl(
      OperatingExpenseRepository expenseRepository,
      HeadquarterRepository headquarterRepository,
      OperatingExpenseAccessPolicy accessPolicy,
      OperatingExpenseReceiptImages imageProcessor,
      OperatingExpenseReceiptStorage receiptStorage) {
    this.expenseRepository = expenseRepository;
    this.headquarterRepository = headquarterRepository;
    this.accessPolicy = accessPolicy;
    this.imageProcessor = imageProcessor;
    this.receiptStorage = receiptStorage;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OperatingExpense> search(
      JwtAuthenticationContext principal, OperatingExpenseListQuery query, Pageable pageable) {
    OperatingExpenseSearchCriteria criteria = accessPolicy.resolveList(principal, query);
    return expenseRepository.search(criteria, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public OperatingExpenseSummary summarize(JwtAuthenticationContext principal, OperatingExpenseListQuery query) {
    OperatingExpenseSearchCriteria criteria = accessPolicy.resolveList(principal, query);
    return new OperatingExpenseSummary(
        expenseRepository.sumAmount(criteria),
        OperatingExpenseSummaries.topCategory(expenseRepository.sumByCategory(criteria)),
        expenseRepository.sumByDay(criteria));
  }

  @Override
  @Transactional(readOnly = true)
  public OperatingExpense getById(JwtAuthenticationContext principal, long id) {
    OperatingExpense expense =
        expenseRepository.findById(id).orElseThrow(() -> new OperatingExpenseNotFoundException(id));
    accessPolicy.requireVisible(principal, expense);
    return expense;
  }

  @Override
  public OperatingExpense create(JwtAuthenticationContext principal, CreateOperatingExpenseCommand command) {
    long headquarterId = accessPolicy.resolveWriteHeadquarter(principal, command.headquarterId());
    LocalDate occurredOn = accessPolicy.resolveOccurredOn(principal, command.occurredOn());
    requireHeadquarter(headquarterId);
    List<MultipartFile> files = files(command.attachments());
    imageProcessor.validateAll(files, 0);
    List<String> uploaded = upload(files, headquarterId, occurredOn);
    try {
      OperatingExpense expense =
          OperatingExpense.builder()
              .withHeadquarterId(headquarterId)
              .withAmount(command.amount())
              .withCategory(command.category())
              .withConcept(command.concept())
              .withNotes(command.notes())
              .withOccurredOn(occurredOn)
              .withRecordedByUserId(principal.userId())
              .withAttachmentKeys(uploaded)
              .register();
      return expenseRepository.save(expense);
    } catch (RuntimeException ex) {
      discardUploads(uploaded);
      throw ex;
    }
  }

  @Override
  public OperatingExpense update(JwtAuthenticationContext principal, long id, UpdateOperatingExpenseCommand command) {
    accessPolicy.requireMaintainer(principal);
    OperatingExpense existing =
        expenseRepository.findById(id).orElseThrow(() -> new OperatingExpenseNotFoundException(id));
    long headquarterId = accessPolicy.resolveWriteHeadquarter(principal, command.headquarterId());
    LocalDate occurredOn = accessPolicy.resolveOccurredOn(principal, command.occurredOn());
    requireHeadquarter(headquarterId);
    List<String> previous = existing.getAttachmentKeys();
    List<String> retained = retainedKeys(previous, command.retainAttachmentKeys());
    List<MultipartFile> files = files(command.attachments());
    imageProcessor.validateAll(files, retained.size());
    List<String> uploaded = upload(files, headquarterId, occurredOn);
    OperatingExpense saved;
    try {
      List<String> keys = new ArrayList<>(retained);
      keys.addAll(uploaded);
      existing.setHeadquarterId(headquarterId);
      existing.setAmount(command.amount());
      existing.setCategory(command.category());
      existing.setConcept(command.concept());
      existing.setNotes(command.notes());
      existing.setOccurredOn(occurredOn);
      existing.setAttachmentKeys(keys);
      existing.touch();
      saved = expenseRepository.save(existing);
    } catch (RuntimeException ex) {
      discardUploads(uploaded);
      throw ex;
    }
    Set<String> kept = new HashSet<>(retained);
    for (String key : previous) {
      if (!kept.contains(key)) {
        receiptStorage.deleteQuietly(key);
      }
    }
    return saved;
  }

  @Override
  public void delete(JwtAuthenticationContext principal, long id) {
    accessPolicy.requireMaintainer(principal);
    OperatingExpense existing =
        expenseRepository.findById(id).orElseThrow(() -> new OperatingExpenseNotFoundException(id));
    existing.delete();
    expenseRepository.save(existing);
  }

  private void requireHeadquarter(long headquarterId) {
    if (headquarterRepository.findById(headquarterId).isEmpty()) {
      throw new HeadquarterNotFoundException(headquarterId);
    }
  }

  private List<String> upload(List<MultipartFile> files, long headquarterId, LocalDate occurredOn) {
    List<String> uploaded = new ArrayList<>();
    try {
      for (MultipartFile file : files) {
        byte[] jpeg = imageProcessor.process(file);
        uploaded.add(receiptStorage.storeJpeg(jpeg, headquarterId, occurredOn));
      }
      return uploaded;
    } catch (RuntimeException ex) {
      discardUploads(uploaded);
      throw ex;
    }
  }

  private void discardUploads(List<String> keys) {
    for (String key : keys) {
      log.info("discarding expense receipt after failed save key={}", key);
      receiptStorage.deleteQuietly(key);
    }
  }

  private static List<MultipartFile> files(List<MultipartFile> attachments) {
    return attachments == null ? List.of() : attachments;
  }

  private static List<String> retainedKeys(List<String> current, List<String> requested) {
    if (requested == null) {
      return new ArrayList<>(current);
    }
    List<String> kept = new ArrayList<>();
    for (String key : requested) {
      if (key == null || key.isBlank()) {
        throw new BusinessValidationException(
            "La clave del adjunto es inválida.",
            Map.of("reason", "INVALID_KEY"),
            "blank retainAttachmentKeys entry");
      }
      String trimmed = key.strip();
      if (!current.contains(trimmed)) {
        throw new BusinessValidationException(
            "Un adjunto no pertenece a este gasto operativo.",
            Map.of("reason", "UNKNOWN_KEY"),
            "retain key not on expense");
      }
      if (kept.contains(trimmed)) {
        throw new BusinessValidationException(
            "La clave del adjunto está repetida.",
            Map.of("reason", "DUPLICATE_KEY"),
            "duplicate retain key");
      }
      kept.add(trimmed);
    }
    return kept;
  }
}
