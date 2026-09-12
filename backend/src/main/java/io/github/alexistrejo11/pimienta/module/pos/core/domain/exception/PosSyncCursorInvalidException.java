package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class PosSyncCursorInvalidException extends ConflictException {

  public PosSyncCursorInvalidException(String cursor) {
    super(
        ErrorCode.POS_SYNC_CURSOR_INVALID,
        "The sync cursor is invalid or belongs to another site. Re-bootstrap.",
        Map.of("cursor", cursor != null ? cursor : ""),
        "POS sync cursor invalid: " + cursor);
  }
}
