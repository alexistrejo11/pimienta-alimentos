package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence.entity;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "operating_expenses")
public class OperatingExpenseJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private OperatingExpenseCategory category;

  @Column(nullable = false, length = 200)
  private String concept;

  @Column(nullable = true, length = 500)
  private String notes;

  @Column(name = "occurred_on", nullable = false)
  private LocalDate occurredOn;

  @Column(name = "recorded_by_user_id", nullable = false)
  private Long recordedByUserId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attachment_keys", nullable = false)
  private List<String> attachmentKeys = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public OperatingExpenseCategory getCategory() {
    return category;
  }

  public void setCategory(OperatingExpenseCategory category) {
    this.category = category;
  }

  public String getConcept() {
    return concept;
  }

  public void setConcept(String concept) {
    this.concept = concept;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public LocalDate getOccurredOn() {
    return occurredOn;
  }

  public void setOccurredOn(LocalDate occurredOn) {
    this.occurredOn = occurredOn;
  }

  public Long getRecordedByUserId() {
    return recordedByUserId;
  }

  public void setRecordedByUserId(Long recordedByUserId) {
    this.recordedByUserId = recordedByUserId;
  }

  public List<String> getAttachmentKeys() {
    return attachmentKeys;
  }

  public void setAttachmentKeys(List<String> attachmentKeys) {
    this.attachmentKeys = attachmentKeys != null ? attachmentKeys : new ArrayList<>();
  }
}
