package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "account_users",
    indexes = {
      @Index(name = "idx_account_users_phone", columnList = "phone"),
      @Index(name = "idx_account_users_deleted_at", columnList = "deleted_at"),
      @Index(name = "idx_account_users_account_status", columnList = "account_status")
    })
public class UserJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 320)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 200)
  private String passwordHash;

  @Column(name = "first_name", nullable = false, length = 120)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 120)
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Gender gender;

  @Column(nullable = false, length = 32)
  private String phone;

  @Column(name = "date_of_birth", nullable = false)
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", nullable = false, length = 32)
  private AccountStatus accountStatus;

  @Column(name = "banned_reason", length = 500)
  private String bannedReason;

  @Column(name = "banned_at")
  private LocalDateTime bannedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "account_user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      indexes = @Index(name = "idx_account_user_roles_user_id", columnList = "user_id"))
  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Set<Role> roles = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "account_user_headquarters",
      joinColumns = @JoinColumn(name = "user_id"),
      indexes = @Index(name = "idx_account_user_headquarters_hq", columnList = "headquarter_id"))
  @Column(name = "headquarter_id")
  private Set<Long> assignedHeadquarterIds = new HashSet<>();

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(nullable = false)
  private Long version;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(AccountStatus accountStatus) {
    this.accountStatus = accountStatus;
  }

  public String getBannedReason() {
    return bannedReason;
  }

  public void setBannedReason(String bannedReason) {
    this.bannedReason = bannedReason;
  }

  public LocalDateTime getBannedAt() {
    return bannedAt;
  }

  public void setBannedAt(LocalDateTime bannedAt) {
    this.bannedAt = bannedAt;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void setRoles(Set<Role> roles) {
    this.roles = roles;
  }

  public Set<Long> getAssignedHeadquarterIds() {
    return assignedHeadquarterIds;
  }

  public void setAssignedHeadquarterIds(Set<Long> assignedHeadquarterIds) {
    this.assignedHeadquarterIds = assignedHeadquarterIds;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
