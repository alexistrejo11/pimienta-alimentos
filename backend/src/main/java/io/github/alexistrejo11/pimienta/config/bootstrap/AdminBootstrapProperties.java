package io.github.alexistrejo11.pimienta.config.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time first-admin bootstrap from environment variables. Not a Flyway seed: runs after schema
 * is ready (Hibernate {@code update} in dev, Flyway in prod) and is a no-op once an ACTIVE ADMIN
 * already exists.
 */
@ConfigurationProperties(prefix = "pimienta.bootstrap.admin")
public class AdminBootstrapProperties {

  /**
   * When false the runner is skipped. Tests force this off so a local {@code .env} cannot create
   * users in H2.
   */
  private boolean enabled = true;

  private String email = "";

  private String password = "";

  private String firstName = "Admin";

  private String lastName = "Pimienta";

  private String phone = "+520000000001";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean hasCredentials() {
    return email != null
        && !email.isBlank()
        && password != null
        && !password.isBlank();
  }
}
