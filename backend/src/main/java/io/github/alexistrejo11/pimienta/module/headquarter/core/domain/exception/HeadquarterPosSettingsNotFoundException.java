package io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class HeadquarterPosSettingsNotFoundException extends ResourceNotFoundException {

  public HeadquarterPosSettingsNotFoundException(long headquarterId) {
    super(
        ErrorCode.HEADQUARTER_POS_SETTINGS_NOT_FOUND,
        "POS settings were not found for this headquarter.",
        Map.of("headquarterId", headquarterId),
        "POS settings missing for headquarterId=" + headquarterId);
  }
}
