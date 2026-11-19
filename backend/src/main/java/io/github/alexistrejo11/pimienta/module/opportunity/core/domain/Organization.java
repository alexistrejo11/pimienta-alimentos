package io.github.alexistrejo11.pimienta.module.opportunity.core.domain;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import io.github.alexistrejo11.pimienta.shared.util.Strings;
import java.time.LocalDateTime;

public class Organization extends BaseDomain<Long> {

  private String name;
  private OrganizationKind kind;
  private String notes;
  private String contactName;
  private String contactEmail;
  private String contactPhone;

  private Organization() {
    this.id = 0L;
    this.name = "";
    this.kind = OrganizationKind.UNDEFINED;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public String getName() {
    return name != null ? name : "";
  }

  public OrganizationKind getKind() {
    return kind != null ? kind : OrganizationKind.UNDEFINED;
  }

  public String getNotes() {
    return notes;
  }

  public String getContactName() {
    return contactName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void applyProfile(
      String name,
      OrganizationKind kind,
      String notes,
      String contactName,
      String contactEmail,
      String contactPhone) {
    this.name = name != null ? name.strip() : "";
    this.kind = kind != null ? kind : OrganizationKind.OTHER;
    this.notes = Strings.blankToNull(notes);
    this.contactName = Strings.blankToNull(contactName);
    this.contactEmail = Strings.blankToNull(contactEmail);
    this.contactPhone = Strings.blankToNull(contactPhone);
    touch();
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

  public static final class SafeBuilder {
    private Long id;
    private String name;
    private OrganizationKind kind;
    private String notes;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public SafeBuilder withKind(OrganizationKind kind) {
      this.kind = kind;
      return this;
    }

    public SafeBuilder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public SafeBuilder withContactName(String contactName) {
      this.contactName = contactName;
      return this;
    }

    public SafeBuilder withContactEmail(String contactEmail) {
      this.contactEmail = contactEmail;
      return this;
    }

    public SafeBuilder withContactPhone(String contactPhone) {
      this.contactPhone = contactPhone;
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

    public Organization register() {
      Organization o = new Organization();
      o.name = name != null ? name.strip() : "";
      o.kind = kind != null ? kind : OrganizationKind.OTHER;
      o.notes = Strings.blankToNull(notes);
      o.contactName = Strings.blankToNull(contactName);
      o.contactEmail = Strings.blankToNull(contactEmail);
      o.contactPhone = Strings.blankToNull(contactPhone);
      o.createdAt = LocalDateTime.now();
      o.updatedAt = o.createdAt;
      return o;
    }

    public Organization reconstruct() {
      Organization o = new Organization();
      o.id = id != null ? id : 0L;
      o.name = name;
      o.kind = kind != null ? kind : OrganizationKind.UNDEFINED;
      o.notes = notes;
      o.contactName = contactName;
      o.contactEmail = contactEmail;
      o.contactPhone = contactPhone;
      o.createdAt = createdAt;
      o.updatedAt = updatedAt;
      o.deletedAt = deletedAt;
      o.version = version;
      return o;
    }
  }
}
