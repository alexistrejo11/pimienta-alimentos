package io.github.alexistrejo11.pimienta.shared.validation;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {

  private int minimumYears;

  @Override
  public void initialize(MinimumAge constraintAnnotation) {
    this.minimumYears = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
    if (dateOfBirth == null) {
      return false;
    }
    LocalDate today = LocalDate.now(ClientTimeZones.current());
    if (dateOfBirth.isAfter(today)) {
      return false;
    }
    return Period.between(dateOfBirth, today).getYears() >= minimumYears;
  }
}
