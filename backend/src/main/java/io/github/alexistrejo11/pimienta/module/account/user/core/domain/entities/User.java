package io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class User extends BaseDomain<Long> {

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

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Gender getGender() {
    return gender;
  }

  public String getPhone() {
    return phone;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public boolean isPendingApproval() {
    return AccountStatus.PENDING_APPROVAL == accountStatus;
  }

  public boolean isBanned() {
    return AccountStatus.BANNED == accountStatus;
  }

  public String getBannedReason() {
    return bannedReason;
  }

  public LocalDateTime getBannedAt() {
    return bannedAt;
  }

  public List<Role> getRoles() {
    return List.copyOf(roles);
  }

  public List<Long> getAssignedHeadquarterIds() {
    return assignedHeadquarterIds != null ? List.copyOf(assignedHeadquarterIds) : List.of();
  }

  private User() {
    super();
  }

  /**
   * New registration starts with the unassigned baseline role.
   */
  public static User register(UserRegisterParams params) {
    Objects.requireNonNull(params, "params");
    Objects.requireNonNull(params.email(), "email");
    Objects.requireNonNull(params.passwordHash(), "passwordHash");
    Objects.requireNonNull(params.gender(), "gender");
    if (params.email().isBlank()) {
      throw new IllegalArgumentException("email must not be blank");
    }
    if (params.firstName() == null || params.firstName().isBlank()) {
      throw new IllegalArgumentException("firstName must not be blank");
    }
    if (params.lastName() == null || params.lastName().isBlank()) {
      throw new IllegalArgumentException("lastName must not be blank");
    }
    if (params.phone() == null || params.phone().isBlank()) {
      throw new IllegalArgumentException("phone must not be blank");
    }
    var now = LocalDateTime.now();
    var u = new User();
    u.email = params.email().trim().toLowerCase();
    u.passwordHash = params.passwordHash();
    u.firstName = params.firstName().trim();
    u.lastName = params.lastName().trim();
    u.gender = params.gender();
    u.phone = params.phone().trim();
    u.dateOfBirth = null;
    u.accountStatus = AccountStatus.PENDING_APPROVAL;
    u.bannedReason = null;
    u.bannedAt = null;
    u.roles = new ArrayList<>(List.of(Role.USER));
    u.assignedHeadquarterIds = new ArrayList<>();
    u.createdAt = now;
    u.updatedAt = now;
    u.deletedAt = null;
    u.version = 0L;
    u.id = null;
    return u;
  }

  public static User reconstruct(UserReconstructParams params) {
    Objects.requireNonNull(params, "params");
    var u = new User();
    u.id = params.id() != null ? params.id() : 0L;
    u.email = params.email() != null ? params.email() : "";
    u.passwordHash = params.passwordHash() != null ? params.passwordHash() : "";
    u.firstName = params.firstName() != null ? params.firstName() : "";
    u.lastName = params.lastName() != null ? params.lastName() : "";
    u.gender = params.gender();
    u.phone = params.phone() != null ? params.phone() : "";
    u.dateOfBirth = params.dateOfBirth();
    u.accountStatus = params.accountStatus() != null ? params.accountStatus() : AccountStatus.PENDING_APPROVAL;
    u.bannedReason = params.bannedReason();
    u.bannedAt = params.bannedAt();
    u.roles = params.roles() != null ? new ArrayList<>(params.roles()) : new ArrayList<>();
    u.assignedHeadquarterIds =
        params.assignedHeadquarterIds() != null
            ? new ArrayList<>(params.assignedHeadquarterIds())
            : new ArrayList<>();
    u.createdAt = params.createdAt() != null ? params.createdAt() : LocalDateTime.now();
    u.updatedAt = params.updatedAt() != null ? params.updatedAt() : u.createdAt;
    u.deletedAt = params.deletedAt();
    u.version = params.version() != null ? params.version() : 0L;
    return u;
  }

  public void assignPasswordHash(String newHash) {
    this.passwordHash = Objects.requireNonNull(newHash);
    touch();
  }

  /**
   * Profile self-service: name, gender, phone. Does not change email or password.
   */
  public void updateProfile(String firstName, String lastName, Gender gender, String phone) {
    Objects.requireNonNull(gender, "gender");
    if (firstName == null || firstName.isBlank()) {
      throw new IllegalArgumentException("firstName must not be blank");
    }
    if (lastName == null || lastName.isBlank()) {
      throw new IllegalArgumentException("lastName must not be blank");
    }
    if (phone == null || phone.isBlank()) {
      throw new IllegalArgumentException("phone must not be blank");
    }
    this.firstName = firstName.trim();
    this.lastName = lastName.trim();
    this.gender = gender;
    this.phone = phone.trim();
    touch();
  }

  /** Approves a pending account, allowing the user to log in. */
  public void activate() {
    this.accountStatus = AccountStatus.ACTIVE;
    touch();
  }

  public void ban(String reason) {
    this.accountStatus = AccountStatus.BANNED;
    this.bannedReason = reason;
    this.bannedAt = LocalDateTime.now();
    touch();
  }

  public void unban() {
    this.accountStatus = AccountStatus.ACTIVE;
    this.bannedReason = null;
    this.bannedAt = null;
    touch();
  }

  /**
   * Adds roles not already present; merges permissions via
   * {@link Role#getPermissions()}.
   */
  public void addRoles(List<Role> incoming) {
    if (incoming == null || incoming.isEmpty()) {
      return;
    }
    var set = EnumSet.noneOf(Role.class);
    set.addAll(roles);
    for (Role r : incoming) {
      if (r != null) {
        set.add(r);
      }
    }
    this.roles = new ArrayList<>(set);
    touch();
  }

  public void replaceRoles(List<Role> newRoles) {
    var uniqueRoles = EnumSet.noneOf(Role.class);
    if (newRoles != null) {
      newRoles.stream().filter(Objects::nonNull).forEach(uniqueRoles::add);
    }
    this.roles = new ArrayList<>(uniqueRoles);
    touch();
  }

  public void replaceAssignedHeadquarters(List<Long> headquarterIds) {
    this.assignedHeadquarterIds =
        headquarterIds != null ? new ArrayList<>(headquarterIds) : new ArrayList<>();
    touch();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  private void touch() {
    this.updatedAt = LocalDateTime.now();
    this.version = this.version != null ? this.version + 1 : 1L;
  }
}
