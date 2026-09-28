package io.github.alexistrejo11.pimienta.shared.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;

/**
 * Optimistic-lock version owned by persistence. New rows must reach persist with a {@code null}
 * version; Hibernate would otherwise seed {@code @Version} at 0, so the first persisted value is
 * forced to {@link #INITIAL_VERSION}. Never assign the version manually on new entities.
 */
@MappedSuperclass
public abstract class VersionedJpaEntity {

  public static final long INITIAL_VERSION = 1L;

  @Version
  @Column(nullable = false)
  private Long version;

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @PrePersist
  protected void seedVersion() {
    if (version == null || version < INITIAL_VERSION) {
      version = INITIAL_VERSION;
    }
  }
}
