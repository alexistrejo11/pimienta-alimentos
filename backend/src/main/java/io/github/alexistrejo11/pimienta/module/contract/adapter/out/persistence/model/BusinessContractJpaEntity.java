package io.github.alexistrejo11.pimienta.module.contract.adapter.out.persistence.model;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "business_contracts",
    indexes = {
      @Index(name = "idx_business_contracts_employee_id", columnList = "employee_id"),
      @Index(name = "idx_business_contracts_opportunity_id", columnList = "opportunity_id"),
      @Index(name = "idx_business_contracts_project_id", columnList = "project_id"),
      @Index(name = "idx_business_contracts_reference_code", columnList = "reference_code"),
      @Index(name = "idx_business_contracts_deleted_at", columnList = "deleted_at")
    })
public class BusinessContractJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 400)
  private String name;

  @Column(length = 4000)
  private String description = "";

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private ContractCategory category;

  @Column(name = "employee_id")
  private Long employeeId;

  @Column(name = "opportunity_id")
  private Long opportunityId;

  @Column(name = "project_id")
  private Long projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "term_kind", length = 32)
  private ContractTermKind termKind;

  @Column(name = "effective_start")
  private LocalDate effectiveStart;

  @Column(name = "effective_end")
  private LocalDate effectiveEnd;

  @Column(name = "document_url", length = 2000)
  private String documentUrl;

  @Column(name = "terms_and_conditions", length = 8000)
  private String termsAndConditions;

  @Column(name = "reference_code", length = 80)
  private String referenceCode;

  @Column(name = "renewal_cycle_months")
  private Integer renewalCycleMonths;

  @Column(name = "agreed_value", precision = 19, scale = 4)
  private BigDecimal agreedValue;

  @Column(name = "currency_code", length = 3)
  private String currencyCode;

  @Column(name = "extension_count", nullable = false)
  private int extensionCount;

  @Column(name = "last_renewed_at")
  private LocalDateTime lastRenewedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ContractCategory getCategory() {
    return category;
  }

  public void setCategory(ContractCategory category) {
    this.category = category;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ContractTermKind getTermKind() {
    return termKind;
  }

  public void setTermKind(ContractTermKind termKind) {
    this.termKind = termKind;
  }

  public LocalDate getEffectiveStart() {
    return effectiveStart;
  }

  public void setEffectiveStart(LocalDate effectiveStart) {
    this.effectiveStart = effectiveStart;
  }

  public LocalDate getEffectiveEnd() {
    return effectiveEnd;
  }

  public void setEffectiveEnd(LocalDate effectiveEnd) {
    this.effectiveEnd = effectiveEnd;
  }

  public String getDocumentUrl() {
    return documentUrl;
  }

  public void setDocumentUrl(String documentUrl) {
    this.documentUrl = documentUrl;
  }

  public String getTermsAndConditions() {
    return termsAndConditions;
  }

  public void setTermsAndConditions(String termsAndConditions) {
    this.termsAndConditions = termsAndConditions;
  }

  public String getReferenceCode() {
    return referenceCode;
  }

  public void setReferenceCode(String referenceCode) {
    this.referenceCode = referenceCode;
  }

  public Integer getRenewalCycleMonths() {
    return renewalCycleMonths;
  }

  public void setRenewalCycleMonths(Integer renewalCycleMonths) {
    this.renewalCycleMonths = renewalCycleMonths;
  }

  public BigDecimal getAgreedValue() {
    return agreedValue;
  }

  public void setAgreedValue(BigDecimal agreedValue) {
    this.agreedValue = agreedValue;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public int getExtensionCount() {
    return extensionCount;
  }

  public void setExtensionCount(int extensionCount) {
    this.extensionCount = extensionCount;
  }

  public LocalDateTime getLastRenewedAt() {
    return lastRenewedAt;
  }

  public void setLastRenewedAt(LocalDateTime lastRenewedAt) {
    this.lastRenewedAt = lastRenewedAt;
  }
}
