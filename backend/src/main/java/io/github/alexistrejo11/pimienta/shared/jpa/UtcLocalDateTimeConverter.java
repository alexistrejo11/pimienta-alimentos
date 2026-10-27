package io.github.alexistrejo11.pimienta.shared.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Stores {@link LocalDateTime} as a UTC {@link Instant} so audit and business timestamps can live
 * in {@code timestamptz} while API JSON stays a zone-less date-time. The digits are UTC civil
 * time, which matches {@code LocalDateTime.now()} when the JVM zone is UTC.
 */
@Converter(autoApply = true)
public class UtcLocalDateTimeConverter implements AttributeConverter<LocalDateTime, Instant> {

  @Override
  public Instant convertToDatabaseColumn(LocalDateTime attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.toInstant(ZoneOffset.UTC);
  }

  @Override
  public LocalDateTime convertToEntityAttribute(Instant dbData) {
    if (dbData == null) {
      return null;
    }
    return LocalDateTime.ofInstant(dbData, ZoneOffset.UTC);
  }
}
