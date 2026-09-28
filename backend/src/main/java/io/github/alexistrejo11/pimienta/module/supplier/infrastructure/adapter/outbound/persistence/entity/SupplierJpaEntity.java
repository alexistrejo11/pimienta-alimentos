package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "suppliers")
public class SupplierJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 40)
  private String phone;

  @Column(nullable = false, length = 120)
  private String brand;

  @OneToMany(
      mappedBy = "supplier",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private Set<SupplierHeadquarterJpaEntity> headquarters = new HashSet<>();

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

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public Set<SupplierHeadquarterJpaEntity> getHeadquarters() {
    return headquarters;
  }

  public void setHeadquarters(Set<SupplierHeadquarterJpaEntity> headquarters) {
    this.headquarters = headquarters != null ? headquarters : new HashSet<>();
  }
}
