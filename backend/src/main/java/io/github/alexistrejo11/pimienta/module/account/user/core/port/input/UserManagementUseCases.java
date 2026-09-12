package io.github.alexistrejo11.pimienta.module.account.user.core.port.input;

import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.AddRolesCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.AssignHeadquartersCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.BanUserCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.UserStatistics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserManagementUseCases {

  UserStatistics statistics();

  Page<User> getBy(Pageable pageable);

  User getById(Long id);

  User getByEmail(String email);

  void ban(Long userId, BanUserCommand command);

  void unban(Long userId);

  User addRoles(Long userId, AddRolesCommand command);

  User assignHeadquarters(Long userId, AssignHeadquartersCommand command);

  void approve(Long userId);
}
