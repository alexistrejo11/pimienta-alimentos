package io.github.alexistrejo11.pimienta.module.headquarter.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;

/**
 * Headquarter aggregate: persistence-shaped, no business rules. Use cases orchestrate changes;
 * format validation stays on Jakarta-annotated web DTOs. Build with {@link #builder()} and {@link
 * SafeBuilder#register()}, {@link SafeBuilder#revise(Headquarter)}, or {@link SafeBuilder#reconstruct()}.
 */
public class Headquarter extends BaseDomain<Long> {

  private String name;
  private String address;
  private String description;

  private Headquarter() {
    this.id = 0L;
    this.name = "";
    this.address = "";
    this.description = "";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.deletedAt = null;
  }

  public String getName() {
    return name != null ? name : "";
  }

  public String getAddress() {
    return address != null ? address : "";
  }

  public String getDescription() {
    return description != null ? description : "";
  }

  public void setName(String name) {
    this.name = name != null ? name.strip() : "";
  }

  public void setAddress(String address) {
    this.address = address != null ? address : "";
  }

  public void setDescription(String description) {
    this.description = description != null ? description : "";
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
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
    private String address;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withName(String name) {
      this.name = name != null ? name.strip() : "";
      return this;
    }

    public SafeBuilder withAddress(String address) {
      this.address = address != null ? address : "";
      return this;
    }

    public SafeBuilder withDescription(String description) {
      this.description = description != null ? description : "";
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
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

    /** Ensamblaje desde persistencia sin validación de negocio. */
    public Headquarter reconstruct() {
      Headquarter h = new Headquarter();
      h.id = id != null ? id : 0L;
      h.name = name != null ? name : "";
      h.address = address != null ? address : "";
      h.description = description != null ? description : "";
      h.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      h.updatedAt = updatedAt != null ? updatedAt : h.createdAt;
      h.deletedAt = deletedAt;
      h.version = version;
      return h;
    }

    /** New entity assembly; no business validation (shape rules live on HTTP DTOs). */
    public Headquarter register() {
      var now = LocalDateTime.now();
      Headquarter h = new Headquarter();
      h.id = 0L;
      h.name = normalizeName(name);
      h.address = address != null ? address : "";
      h.description = description != null ? description : "";
      h.createdAt = now;
      h.updatedAt = now;
      h.deletedAt = null;
      h.version = version;
      return h;
    }

    /** Field replacement keeping identity and audit; version is copied from {@code existing} for JPA. */
    public Headquarter revise(Headquarter existing) {
      Headquarter h = new Headquarter();
      h.id = existing.getId();
      h.createdAt = existing.getCreatedAt();
      h.deletedAt = existing.getDeletedAt();
      h.name = normalizeName(name);
      h.address = address != null ? address : "";
      h.description = description != null ? description : "";
      h.updatedAt = LocalDateTime.now();
      h.version = existing.getVersion();
      return h;
    }

    public Headquarter build() {
      return reconstruct();
    }
  }

  private static String normalizeName(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return raw.strip();
  }
}
