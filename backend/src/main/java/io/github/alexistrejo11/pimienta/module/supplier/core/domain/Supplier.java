package io.github.alexistrejo11.pimienta.module.supplier.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Supplier extends BaseDomain<Long> {

  private String name;
  private String contactName;
  private String phone;
  private String brand;
  private List<Long> headquarterIds = new ArrayList<>();

  private Supplier() {
    this.id = 0L;
    this.name = "";
    this.contactName = "";
    this.phone = "";
    this.brand = "";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public String getName() {
    return name != null ? name : "";
  }

  public String getContactName() {
    return contactName != null ? contactName : "";
  }

  public String getPhone() {
    return phone != null ? phone : "";
  }

  public String getBrand() {
    return brand != null ? brand : "";
  }

  public List<Long> getHeadquarterIds() {
    return headquarterIds != null ? List.copyOf(headquarterIds) : List.of();
  }

  public void setName(String name) {
    this.name = name != null ? name.strip() : "";
  }

  public void setContactName(String contactName) {
    this.contactName = contactName != null ? contactName.strip() : "";
  }

  public void setPhone(String phone) {
    this.phone = phone != null ? phone.strip() : "";
  }

  public void setBrand(String brand) {
    this.brand = brand != null ? brand.strip() : "";
  }

  public void setHeadquarterIds(List<Long> headquarterIds) {
    this.headquarterIds =
        headquarterIds != null ? new ArrayList<>(headquarterIds) : new ArrayList<>();
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
    private String contactName;
    private String phone;
    private String brand;
    private List<Long> headquarterIds;
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

    public SafeBuilder withContactName(String contactName) {
      this.contactName = contactName;
      return this;
    }

    public SafeBuilder withPhone(String phone) {
      this.phone = phone;
      return this;
    }

    public SafeBuilder withBrand(String brand) {
      this.brand = brand;
      return this;
    }

    public SafeBuilder withHeadquarterIds(List<Long> headquarterIds) {
      this.headquarterIds = headquarterIds;
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

    public Supplier register() {
      Supplier s = new Supplier();
      s.name = name != null ? name.strip() : "";
      s.contactName = contactName != null ? contactName.strip() : "";
      s.phone = phone != null ? phone.strip() : "";
      s.brand = brand != null ? brand.strip() : "";
      s.headquarterIds =
          headquarterIds != null ? new ArrayList<>(headquarterIds) : new ArrayList<>();
      s.createdAt = LocalDateTime.now();
      s.updatedAt = s.createdAt;
      s.version = 0L;
      return s;
    }

    public Supplier reconstruct() {
      Supplier s = new Supplier();
      s.id = id != null ? id : 0L;
      s.name = name;
      s.contactName = contactName;
      s.phone = phone;
      s.brand = brand;
      s.headquarterIds =
          headquarterIds != null ? new ArrayList<>(headquarterIds) : new ArrayList<>();
      s.createdAt = createdAt;
      s.updatedAt = updatedAt;
      s.deletedAt = deletedAt;
      s.version = version != null ? version : 0L;
      return s;
    }
  }
}
