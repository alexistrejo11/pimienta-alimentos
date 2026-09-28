package io.github.alexistrejo11.pimienta.module.contract.core.domain;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.exception.ContractRuleViolationException;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Business contract aggregate: null-tolerant in persistence; getters expose
 * stable defaults.
 * Construct {@link #builder()} and finish {@link SafeBuilder#register()},
 * {@link SafeBuilder#revise(Contract)}, or {@link SafeBuilder#reconstruct()}.
 */
public class Contract extends BaseDomain<Long> {

  private String name;
  private String description;
  private ContractCategory category;
  private Long employeeId;
  private Long opportunityId;
  private Long projectId;
  private ContractTermKind termKind;
  private LocalDate effectiveStart;
  private LocalDate effectiveEnd;
  private String documentUrl;
  private String termsAndConditions;
  private String referenceCode;
  private BigDecimal agreedValue;
  private String currencyCode;
  private Integer renewalCycleMonths;
  private int extensionCount;
  private LocalDateTime lastRenewedAt;

  private Contract() {
    this.id = 0L;
    this.name = "";
    this.description = "";
    this.extensionCount = 0;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.deletedAt = null;
  }

  public void renew() {
    ensureOpenForLifecycle();
    if (getTermKind() != ContractTermKind.FIXED_TERM) {
      throw new ContractRuleViolationException(
          "Only fixed-term contracts can be renewed this way.",
          Map.of("termKind", String.valueOf(getTermKind())),
          "renew() called  termKind=" + termKind);
    }
    if (effectiveEnd == null) {
      throw new ContractRuleViolationException(
          "The contract has no end date to renew from.",
          Map.of(),
          "renew()  null effectiveEnd");
    }
    if (renewalCycleMonths == null || renewalCycleMonths <= 0) {
      throw new ContractRuleViolationException(
          "Configure a positive renewal cycle in months before using renew().",
          Map.of("renewalCycleMonths", String.valueOf(renewalCycleMonths)),
          "renew() out renewalCycleMonths");
    }
    effectiveEnd = effectiveEnd.plusMonths(renewalCycleMonths);
    extensionCount += 1;
    lastRenewedAt = LocalDateTime.now();
    touch();
  }

  public void extendTo(LocalDate newEnd) {
    ensureOpenForLifecycle();
    if (getTermKind() != ContractTermKind.FIXED_TERM) {
      throw new ContractRuleViolationException(
          "Extensions apply only to fixed-term contracts.",
          Map.of("termKind", String.valueOf(getTermKind())),
          "extendTo() called  termKind=" + termKind);
    }
    if (effectiveEnd == null) {
      throw new ContractRuleViolationException(
          "The contract has no current end date to extend.",
          Map.of(),
          "extendTo()  null effectiveEnd");
    }
    if (newEnd == null || !newEnd.isAfter(effectiveEnd)) {
      throw new ContractRuleViolationException(
          "The new end date must be after the current end date.",
          Map.of("currentEnd", effectiveEnd.toString(), "requestedEnd", String.valueOf(newEnd)),
          "extendTo invalid newEnd=" + newEnd);
    }
    effectiveEnd = newEnd;
    extensionCount += 1;
    lastRenewedAt = LocalDateTime.now();
    touch();
  }

  public void updateTermsAndConditions(String termsAndConditions) {
    ensureOpenForLifecycle();
    if (termsAndConditions == null || termsAndConditions.isBlank()) {
      throw new ContractRuleViolationException(
          "Terms and conditions cannot be empty.",
          Map.of(),
          "updateTermsAndConditions  blank input");
    }
    this.termsAndConditions = termsAndConditions.strip();
    touch();
  }

  public void delete() {
    deletedAt = LocalDateTime.now();
    touch();
  }

  public void touch() {
    updatedAt = LocalDateTime.now();
  }

  public boolean isIndefinite() {
    return getTermKind() == ContractTermKind.INDEFINITE;
  }

  public boolean isExpired(LocalDate onDate) {
    if (getTermKind() != ContractTermKind.FIXED_TERM || effectiveEnd == null) {
      return false;
    }
    return onDate.isAfter(effectiveEnd);
  }

  public String getName() {
    return name != null ? name : "";
  }

  public String getDescription() {
    return description != null ? description : "";
  }

  public ContractCategory getCategory() {
    return category != null ? category : ContractCategory.UNDEFINED;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public ContractTermKind getTermKind() {
    return termKind != null ? termKind : ContractTermKind.UNDEFINED;
  }

  public LocalDate getEffectiveStart() {
    return effectiveStart;
  }

  public LocalDate getEffectiveEnd() {
    return effectiveEnd;
  }

  public String getDocumentUrl() {
    return documentUrl != null ? documentUrl : "";
  }

  public String getTermsAndConditions() {
    return termsAndConditions != null ? termsAndConditions : "";
  }

  public String getReferenceCode() {
    return referenceCode != null ? referenceCode : "";
  }

  public BigDecimal getAgreedValue() {
    return agreedValue;
  }

  public String getCurrencyCode() {
    return currencyCode != null ? currencyCode : "";
  }

  public Integer getRenewalCycleMonths() {
    return renewalCycleMonths;
  }

  public int getExtensionCount() {
    return extensionCount;
  }

  public LocalDateTime getLastRenewedAt() {
    return lastRenewedAt;
  }

  /**
   * Raw category for persistence; use when {@link #getCategory()} must not map
   * {@code UNDEFINED}.
   */
  public ContractCategory getCategoryOrNull() {
    return category;
  }

  /** Raw term kind for persistence. */
  public ContractTermKind getTermKindOrNull() {
    return termKind;
  }

  public void setName(String name) {
    this.name = name != null ? name.strip() : "";
  }

  public void setDescription(String description) {
    this.description = description != null ? description : "";
  }

  public void setCategory(ContractCategory category) {
    this.category = category;
  }

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public void setTermKind(ContractTermKind termKind) {
    this.termKind = termKind;
  }

  public void setEffectiveStart(LocalDate effectiveStart) {
    this.effectiveStart = effectiveStart;
  }

  public void setEffectiveEnd(LocalDate effectiveEnd) {
    this.effectiveEnd = effectiveEnd;
  }

  public void setDocumentUrl(String documentUrl) {
    this.documentUrl = blankToNull(documentUrl);
  }

  public void setTermsAndConditions(String termsAndConditions) {
    this.termsAndConditions = blankToNull(termsAndConditions);
  }

  public void setReferenceCode(String referenceCode) {
    this.referenceCode = blankToNull(referenceCode);
  }

  public void setAgreedValue(BigDecimal agreedValue) {
    this.agreedValue = agreedValue;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = normalizeCurrency(currencyCode);
  }

  public void setRenewalCycleMonths(Integer renewalCycleMonths) {
    this.renewalCycleMonths = renewalCycleMonths;
  }

  public void setExtensionCount(int extensionCount) {
    this.extensionCount = Math.max(0, extensionCount);
  }

  public void setLastRenewedAt(LocalDateTime lastRenewedAt) {
    this.lastRenewedAt = lastRenewedAt;
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private String name;
    private String description;
    private ContractCategory category;
    private Long employeeId;
    private Long opportunityId;
    private Long projectId;
    private ContractTermKind termKind;
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;
    private String documentUrl;
    private String termsAndConditions;
    private String referenceCode;
    private Integer renewalCycleMonths;
    private BigDecimal agreedValue;
    private String currencyCode;
    private Integer extensionCount;
    private LocalDateTime lastRenewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder Id(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder Name(String name) {
      this.name = name != null ? name.strip() : "";
      return this;
    }

    public SafeBuilder Description(String description) {
      this.description = description != null ? description : "";
      return this;
    }

    public SafeBuilder Category(ContractCategory category) {
      this.category = category;
      return this;
    }

    public SafeBuilder EmployeeId(Long employeeId) {
      this.employeeId = employeeId;
      return this;
    }

    public SafeBuilder OpportunityId(Long opportunityId) {
      this.opportunityId = opportunityId;
      return this;
    }

    public SafeBuilder ProjectId(Long projectId) {
      this.projectId = projectId;
      return this;
    }

    public SafeBuilder TermKind(ContractTermKind termKind) {
      this.termKind = termKind;
      return this;
    }

    public SafeBuilder EffectiveStart(LocalDate effectiveStart) {
      this.effectiveStart = effectiveStart;
      return this;
    }

    public SafeBuilder EffectiveEnd(LocalDate effectiveEnd) {
      this.effectiveEnd = effectiveEnd;
      return this;
    }

    public SafeBuilder DocumentUrl(String documentUrl) {
      this.documentUrl = blankToNull(documentUrl);
      return this;
    }

    public SafeBuilder TermsAndConditions(String termsAndConditions) {
      this.termsAndConditions = blankToNull(termsAndConditions);
      return this;
    }

    public SafeBuilder ReferenceCode(String referenceCode) {
      this.referenceCode = blankToNull(referenceCode);
      return this;
    }

    public SafeBuilder RenewalCycleMonths(Integer renewalCycleMonths) {
      this.renewalCycleMonths = renewalCycleMonths;
      return this;
    }

    public SafeBuilder AgreedValue(BigDecimal agreedValue) {
      this.agreedValue = agreedValue;
      return this;
    }

    public SafeBuilder CurrencyCode(String currencyCode) {
      this.currencyCode = normalizeCurrency(currencyCode);
      return this;
    }

    public SafeBuilder ExtensionCount(int extensionCount) {
      this.extensionCount = extensionCount;
      return this;
    }

    public SafeBuilder LastRenewedAt(LocalDateTime lastRenewedAt) {
      this.lastRenewedAt = lastRenewedAt;
      return this;
    }

    public SafeBuilder CreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      return this;
    }

    public SafeBuilder UpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
      return this;
    }

    public SafeBuilder DeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder Version(Long version) {
      this.version = version;
      return this;
    }

    /** Assemble from persistence out aggregate validation. */
    public Contract reconstruct() {
      Contract c = new Contract();
      c.id = id != null ? id : 0L;
      c.name = name != null ? name : "";
      c.description = description != null ? description : "";
      c.category = category;
      c.employeeId = employeeId;
      c.opportunityId = opportunityId;
      c.projectId = projectId;
      c.termKind = termKind;
      c.effectiveStart = effectiveStart;
      c.effectiveEnd = effectiveEnd;
      c.documentUrl = documentUrl;
      c.termsAndConditions = termsAndConditions;
      c.referenceCode = referenceCode;
      c.agreedValue = agreedValue;
      c.currencyCode = currencyCode;
      c.renewalCycleMonths = renewalCycleMonths;
      c.extensionCount = extensionCount != null ? extensionCount : 0;
      c.lastRenewedAt = lastRenewedAt;
      c.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      c.updatedAt = updatedAt != null ? updatedAt : c.createdAt;
      c.deletedAt = deletedAt;
      c.version = version;
      return c;
    }

    /** New aggregate after validation; timestamps set to now. */
    public Contract register() {
      validateRegistration(this);
      var now = LocalDateTime.now();
      Contract c = new Contract();
      c.id = 0L;
      c.name = name != null && !name.isBlank() ? name.strip() : "";
      c.description = description != null ? description : "";
      c.category = category;
      c.employeeId = employeeId;
      c.opportunityId = opportunityId;
      c.projectId = projectId;
      c.termKind = termKind;
      c.effectiveStart = effectiveStart;
      c.effectiveEnd = effectiveEnd;
      c.documentUrl = documentUrl;
      c.termsAndConditions = termsAndConditions;
      c.referenceCode = referenceCode;
      c.agreedValue = agreedValue;
      c.currencyCode = currencyCode;
      c.renewalCycleMonths = renewalCycleMonths;
      c.extensionCount = 0;
      c.lastRenewedAt = null;
      c.createdAt = now;
      c.updatedAt = now;
      c.deletedAt = null;
      c.version = version;
      return c;
    }

    /**
     * New state for update: keeps identity, renewal history, audit markers and the
     * loaded version (persistence increments it).
     */
    public Contract revise(Contract existing) {
      validateRevision(this);
      Contract c = new Contract();
      c.id = existing.getId();
      c.createdAt = existing.getCreatedAt();
      c.deletedAt = existing.getDeletedAt();
      c.extensionCount = existing.getExtensionCount();
      c.lastRenewedAt = existing.getLastRenewedAt();
      c.name = name != null && !name.isBlank() ? name.strip() : "";
      c.description = description != null ? description : "";
      c.category = category;
      c.employeeId = employeeId;
      c.opportunityId = opportunityId;
      c.projectId = projectId;
      c.termKind = termKind;
      c.effectiveStart = effectiveStart;
      c.effectiveEnd = effectiveEnd;
      c.documentUrl = documentUrl;
      c.termsAndConditions = termsAndConditions;
      c.referenceCode = referenceCode;
      c.agreedValue = agreedValue;
      c.currencyCode = currencyCode;
      c.renewalCycleMonths = renewalCycleMonths;
      c.updatedAt = LocalDateTime.now();
      c.version = existing.getVersion();
      return c;
    }

    /** Same as {@link #reconstruct()} for symmetry other aggregates. */
    public Contract build() {
      return reconstruct();
    }
  }

  private void ensureOpenForLifecycle() {
    if (deletedAt != null) {
      throw new ContractRuleViolationException(
          "This contract is no longer active.",
          Map.of("deletedAt", deletedAt.toString()),
          "lifecycle on deleted contract id=" + id);
    }
  }

  private static void validateRegistration(SafeBuilder b) {
    if (b.name == null || b.name.isBlank()) {
      throw new ContractRuleViolationException(
          "Contract name is required.",
          Map.of(),
          "create  blank name");
    }
    if (b.category == null || b.category == ContractCategory.UNDEFINED) {
      throw new ContractRuleViolationException(
          "Contract category is required.",
          Map.of(),
          "create  null category");
    }
    if (b.termKind == null || b.termKind == ContractTermKind.UNDEFINED) {
      throw new ContractRuleViolationException(
          "Contract term kind is required.",
          Map.of(),
          "create  null termKind");
    }
    if (b.effectiveStart == null) {
      throw new ContractRuleViolationException(
          "Effective start date is required.",
          Map.of(),
          "create  null effectiveStart");
    }
    validateTermDates(b.termKind, b.effectiveStart, b.effectiveEnd);
    validateMonetaryPair(b.agreedValue, b.currencyCode);
    validateRelationshipIds(b.category, b.employeeId, b.opportunityId, b.projectId);
  }

  private static void validateRevision(SafeBuilder b) {
    if (b.name == null || b.name.isBlank()) {
      throw new ContractRuleViolationException(
          "Contract name is required.",
          Map.of(),
          "revise  blank name");
    }
    if (b.category == null || b.category == ContractCategory.UNDEFINED) {
      throw new ContractRuleViolationException(
          "Contract category is required.",
          Map.of(),
          "revise  null category");
    }
    if (b.termKind == null || b.termKind == ContractTermKind.UNDEFINED) {
      throw new ContractRuleViolationException(
          "Contract term kind is required.",
          Map.of(),
          "revise  null termKind");
    }
    if (b.effectiveStart == null) {
      throw new ContractRuleViolationException(
          "Effective start date is required.",
          Map.of(),
          "revise  null effectiveStart");
    }
    validateTermDates(b.termKind, b.effectiveStart, b.effectiveEnd);
    validateMonetaryPair(b.agreedValue, b.currencyCode);
    validateRelationshipIds(b.category, b.employeeId, b.opportunityId, b.projectId);
  }

  private static void validateRelationshipIds(
      ContractCategory category, Long employeeId, Long opportunityId, Long projectId) {
    if (category == ContractCategory.EMPLOYEE) {
      if (employeeId == null) {
        throw new ContractRuleViolationException(
            "Employment contracts must reference the employee they belong to.",
            Map.of("category", category.name()),
            "EMPLOYEE category out employeeId");
      }
    } else if (employeeId != null) {
      throw new ContractRuleViolationException(
          "Only employment contracts may reference an employee.",
          Map.of("category", category.name()),
          "employeeId set for non-EMPLOYEE category");
    }
    if (opportunityId != null && opportunityId <= 0) {
      throw new ContractRuleViolationException(
          "When set, opportunity id must be positive.",
          Map.of("opportunityId", opportunityId),
          "invalid opportunityId");
    }
    if (projectId != null && projectId <= 0) {
      throw new ContractRuleViolationException(
          "When set, project id must be positive.",
          Map.of("projectId", projectId),
          "invalid projectId");
    }
  }

  private static void validateTermDates(
      ContractTermKind termKind, LocalDate effectiveStart, LocalDate effectiveEnd) {
    if (termKind == ContractTermKind.FIXED_TERM) {
      if (effectiveEnd == null) {
        throw new ContractRuleViolationException(
            "Fixed-term contracts require an end date.",
            Map.of("termKind", termKind.name()),
            "FIXED_TERM  null effectiveEnd");
      }
      if (!effectiveEnd.isAfter(effectiveStart)) {
        throw new ContractRuleViolationException(
            "The end date must be after the start date.",
            Map.of("effectiveStart", effectiveStart.toString(), "effectiveEnd", effectiveEnd.toString()),
            "invalid fixed term range");
      }
      return;
    }
    if (termKind == ContractTermKind.INDEFINITE && effectiveEnd != null) {
      throw new ContractRuleViolationException(
          "Indefinite contracts must not have an end date in the system.",
          Map.of("termKind", termKind.name(), "effectiveEnd", effectiveEnd.toString()),
          "INDEFINITE  effectiveEnd set");
    }
  }

  private static void validateMonetaryPair(BigDecimal agreedValue, String currencyCode) {
    if (agreedValue != null && agreedValue.signum() < 0) {
      throw new ContractRuleViolationException(
          "Agreed value cannot be negative.",
          Map.of(),
          "negative agreedValue");
    }
    String cur = currencyCode != null ? currencyCode : "";
    if (agreedValue != null && cur.length() != 3) {
      throw new ContractRuleViolationException(
          "Provide a 3-letter ISO currency when an agreed value is set.",
          Map.of("currencyCode", cur),
          "agreedValue out valid currency");
    }
    if (agreedValue == null && !cur.isEmpty()) {
      throw new ContractRuleViolationException(
          "Remove the currency or set an agreed value.",
          Map.of("currencyCode", cur),
          "currency out agreedValue");
    }
  }

  private static String blankToNull(String s) {
    if (s == null) {
      return null;
    }
    var t = s.strip();
    return t.isEmpty() ? null : t;
  }

  private static String normalizeCurrency(String currencyCode) {
    if (currencyCode == null) {
      return null;
    }
    var t = currencyCode.strip().toUpperCase(Locale.ROOT);
    return t.isEmpty() ? null : t;
  }
}
