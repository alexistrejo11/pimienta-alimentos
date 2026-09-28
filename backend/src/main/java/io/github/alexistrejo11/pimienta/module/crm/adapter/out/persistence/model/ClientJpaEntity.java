package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "crm_clients",
    indexes = {
      @Index(name = "idx_crm_clients_deleted_at", columnList = "deleted_at"),
      @Index(name = "idx_crm_clients_name", columnList = "name")
    })
public class ClientJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 500)
  private String name;

  @Column(name = "company_name", length = 500)
  private String companyName;

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

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }
}
