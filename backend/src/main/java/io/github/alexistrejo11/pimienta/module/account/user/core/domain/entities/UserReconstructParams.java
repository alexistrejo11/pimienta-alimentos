package io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;

/**
 * Parameter object for {@link User#reconstruct(UserReconstructParams)}.
 *
 * <p>
 * Use the nested {@link Builder} to construct an instance when loading a user
 * from persistence.
 *
 * <pre>{@code
 * User.reconstruct(
 *     UserReconstructParams.builder()
 *         .id(entity.getId())
 *         .email(entity.getEmail())
 *         // ...
 *         .build());
 * }</pre>
 */
public record UserReconstructParams(
    Long id,
    String email,
    String passwordHash,
    String firstName,
    String lastName,
    Gender gender,
    String phone,
    LocalDate dateOfBirth,
    AccountStatus accountStatus,
    String bannedReason,
    LocalDateTime bannedAt,
    List<Role> roles,
    List<Long> assignedHeadquarterIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    Long version) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private Long id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private Gender gender;
    private String phone;
    private LocalDate dateOfBirth;
    private AccountStatus accountStatus;
    private String bannedReason;
    private LocalDateTime bannedAt;
    private List<Role> roles;
    private List<Long> assignedHeadquarterIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    private Builder() {
    }

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder passwordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder gender(Gender gender) {
      this.gender = gender;
      return this;
    }

    public Builder phone(String phone) {
      this.phone = phone;
      return this;
    }

    public Builder dateOfBirth(LocalDate dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder accountStatus(AccountStatus accountStatus) {
      this.accountStatus = accountStatus;
      return this;
    }

    public Builder bannedReason(String bannedReason) {
      this.bannedReason = bannedReason;
      return this;
    }

    public Builder bannedAt(LocalDateTime bannedAt) {
      this.bannedAt = bannedAt;
      return this;
    }

    public Builder roles(List<Role> roles) {
      this.roles = roles;
      return this;
    }

    public Builder assignedHeadquarterIds(List<Long> assignedHeadquarterIds) {
      this.assignedHeadquarterIds = assignedHeadquarterIds;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder deletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public Builder version(Long version) {
      this.version = version;
      return this;
    }

    public UserReconstructParams build() {
      return new UserReconstructParams(
          id,
          email,
          passwordHash,
          firstName,
          lastName,
          gender,
          phone,
          dateOfBirth,
          accountStatus,
          bannedReason,
          bannedAt,
          roles,
          assignedHeadquarterIds,
          createdAt,
          updatedAt,
          deletedAt,
          version);
    }
  }
}
