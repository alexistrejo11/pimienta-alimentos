package io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ForbiddenException;
import java.util.Map;

/**
 * Login attempt on an account that has not yet been approved by an
 * administrator.
 */
public class AccountPendingApprovalException extends ForbiddenException {

  public AccountPendingApprovalException(Long userId) {
    super(
        ErrorCode.ACCOUNT_PENDING_APPROVAL,
        "Your account is pending approval. Please wait until an administrator activates it.",
        Map.of("userId", userId),
        "Login blocked: account pending approval, userId=" + userId);
  }
}
