package io.github.alexistrejo11.pimienta.config.web;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

/** Resolves Spanish client-facing messages for {@link ErrorCode} (presentation layer only). */
@Component
public class ClientErrorMessages {

  private final MessageSource messageSource;

  public ClientErrorMessages(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String resolve(ErrorCode errorCode) {
    return resolve(errorCode.code(), null);
  }

  public String resolve(ErrorCode errorCode, String fallback) {
    return resolve(errorCode.code(), fallback);
  }

  public String resolve(String code, String fallback) {
    try {
      return messageSource.getMessage(code, null, MessageSourceConfig.DEFAULT_LOCALE);
    } catch (NoSuchMessageException ex) {
      if (fallback != null && !fallback.isBlank()) {
        return fallback;
      }
      return messageSource.getMessage(
          ErrorCode.INTERNAL_ERROR.code(),
          null,
          "Ocurrió un error inesperado.",
          MessageSourceConfig.DEFAULT_LOCALE);
    }
  }

  public Locale locale() {
    return MessageSourceConfig.DEFAULT_LOCALE;
  }
}
