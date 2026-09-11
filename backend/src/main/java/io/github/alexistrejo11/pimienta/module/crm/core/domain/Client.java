package io.github.alexistrejo11.pimienta.module.crm.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;

/** CRM client aggregate: persistence-shaped; validation on web DTOs. */
public class Client extends BaseDomain<Long> {

  private String name;
  private String companyName;

  private Client() {
    this.id = 0L;
    this.name = "";
    this.companyName = "";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public String getName() {
    return name != null ? name : "";
  }

  public String getCompanyName() {
    return companyName != null ? companyName : "";
  }

  public void setName(String name) {
    this.name = name != null ? name.strip() : "";
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName != null ? companyName.strip() : "";
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private String name;
    private String companyName;
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

    public SafeBuilder withCompanyName(String companyName) {
      this.companyName = companyName;
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

    public Client register() {
      Client c = new Client();
      c.name = name != null ? name.strip() : "";
      c.companyName = companyName != null ? companyName.strip() : "";
      c.createdAt = LocalDateTime.now();
      c.updatedAt = c.createdAt;
      c.version = 0L;
      return c;
    }

    public Client reconstruct() {
      Client c = new Client();
      c.id = id != null ? id : 0L;
      c.name = name != null ? name : "";
      c.companyName = companyName != null ? companyName : "";
      c.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      c.updatedAt = updatedAt != null ? updatedAt : c.createdAt;
      c.deletedAt = deletedAt;
      c.version = version != null ? version : 0L;
      return c;
    }
  }
}
