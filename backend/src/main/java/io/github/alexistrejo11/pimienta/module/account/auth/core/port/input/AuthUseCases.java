package io.github.alexistrejo11.pimienta.module.account.auth.core.port.input;

import io.github.alexistrejo11.pimienta.module.account.auth.core.application.RegisterResult;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LoginCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LogoutCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RefreshSessionCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RegisterCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.IssuedTokens;

public interface AuthUseCases {

  RegisterResult register(RegisterCommand command);

  IssuedTokens login(LoginCommand command);

  IssuedTokens refresh(RefreshSessionCommand command);

  void logout(LogoutCommand command);
}
