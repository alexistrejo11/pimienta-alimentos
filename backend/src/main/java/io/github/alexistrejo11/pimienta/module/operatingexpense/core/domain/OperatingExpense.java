package io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One operating expense recorded against a single headquarter. Payroll is out of scope. */
public class OperatingExpense extends BaseDomain<Long> {

  public static final int MAX_ATTACHMENTS = 5;

  private long headquarterId;
  private BigDecimal amount;
  private OperatingExpenseCategory category;
  private String concept;
  private String notes;
  private LocalDate occurredOn;
  private long recordedByUserId;
  private List<String> attachmentKeys = new ArrayList<>();

  private OperatingExpense() {
    this.id = 0L;
    this.amount = BigDecimal.ZERO;
    this.concept = "";
    this.notes = "";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public long getHeadquarterId() {
    return headquarterId;
  }

  public BigDecimal getAmount() {
    return amount != null ? amount : BigDecimal.ZERO;
  }

  public OperatingExpenseCategory getCategory() {
    return category;
  }

  public String getConcept() {
    return concept != null ? concept : "";
  }

  public String getNotes() {
    return notes != null ? notes : "";
  }

  public LocalDate getOccurredOn() {
    return occurredOn;
  }

  public long getRecordedByUserId() {
    return recordedByUserId;
  }

  public List<String> getAttachmentKeys() {
    return attachmentKeys != null ? List.copyOf(attachmentKeys) : List.of();
  }

  public void setHeadquarterId(long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount != null ? amount : BigDecimal.ZERO;
  }

  public void setCategory(OperatingExpenseCategory category) {
    this.category = category;
  }

  public void setConcept(String concept) {
    this.concept = concept != null ? concept.strip() : "";
  }

  public void setNotes(String notes) {
    this.notes = notes != null ? notes.strip() : "";
  }

  public void setOccurredOn(LocalDate occurredOn) {
    this.occurredOn = occurredOn;
  }

  public void setRecordedByUserId(long recordedByUserId) {
    this.recordedByUserId = recordedByUserId;
  }

  public void setAttachmentKeys(List<String> attachmentKeys) {
    this.attachmentKeys = copyKeys(attachmentKeys);
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  private static List<String> copyKeys(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return new ArrayList<>();
    }
    List<String> copy = new ArrayList<>();
    for (String key : raw) {
      if (key != null && !key.isBlank()) {
        copy.add(key.strip());
      }
    }
    return copy;
  }

  public static final class SafeBuilder {
    private Long id;
    private Long headquarterId;
    private BigDecimal amount;
    private OperatingExpenseCategory category;
    private String concept;
    private String notes;
    private LocalDate occurredOn;
    private Long recordedByUserId;
    private List<String> attachmentKeys;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withAmount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public SafeBuilder withCategory(OperatingExpenseCategory category) {
      this.category = category;
      return this;
    }

    public SafeBuilder withConcept(String concept) {
      this.concept = concept;
      return this;
    }

    public SafeBuilder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public SafeBuilder withOccurredOn(LocalDate occurredOn) {
      this.occurredOn = occurredOn;
      return this;
    }

    public SafeBuilder withRecordedByUserId(Long recordedByUserId) {
      this.recordedByUserId = recordedByUserId;
      return this;
    }

    public SafeBuilder withAttachmentKeys(List<String> attachmentKeys) {
      this.attachmentKeys = attachmentKeys;
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public OperatingExpense register() {
      OperatingExpense expense = new OperatingExpense();
      expense.headquarterId = headquarterId != null ? headquarterId : 0L;
      expense.amount = amount != null ? amount : BigDecimal.ZERO;
      expense.category = category;
      expense.concept = concept != null ? concept.strip() : "";
      expense.notes = notes != null ? notes.strip() : "";
      expense.occurredOn = occurredOn;
      expense.recordedByUserId = recordedByUserId != null ? recordedByUserId : 0L;
      expense.attachmentKeys = copyKeys(attachmentKeys);
      expense.createdAt = LocalDateTime.now();
      expense.updatedAt = expense.createdAt;
      return expense;
    }

    public OperatingExpense reconstruct() {
      OperatingExpense expense = new OperatingExpense();
      expense.id = id != null ? id : 0L;
      expense.headquarterId = headquarterId != null ? headquarterId : 0L;
      expense.amount = amount;
      expense.category = category;
      expense.concept = concept;
      expense.notes = notes;
      expense.occurredOn = occurredOn;
      expense.recordedByUserId = recordedByUserId != null ? recordedByUserId : 0L;
      expense.attachmentKeys = copyKeys(attachmentKeys);
      expense.createdAt = createdAt;
      expense.updatedAt = updatedAt;
      expense.deletedAt = deletedAt;
      expense.version = version;
      return expense;
    }
  }
}
