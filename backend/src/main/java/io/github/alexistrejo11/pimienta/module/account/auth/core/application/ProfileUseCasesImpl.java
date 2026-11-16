package io.github.alexistrejo11.pimienta.module.account.auth.core.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.UserManagerDashboard;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.ProfileUseCases;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.UpdateProfileCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions.UserNotFoundException;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.output.UserRepository;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.task.core.port.TaskRepository;

@Service
public class ProfileUseCasesImpl implements ProfileUseCases {
  private static final Logger log = LoggerFactory.getLogger(ProfileUseCasesImpl.class);

  private final UserRepository userRepository;
  private final EmployeeRepository employeeRepository;
  private final HeadquarterRepository headquarterRepository;
  private final TaskRepository taskRepository;

  public ProfileUseCasesImpl(
      UserRepository userRepository,
      EmployeeRepository employeeRepository,
      HeadquarterRepository headquarterRepository,
      TaskRepository taskRepository) {
    this.userRepository = userRepository;
    this.employeeRepository = employeeRepository;
    this.headquarterRepository = headquarterRepository;
    this.taskRepository = taskRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserManagerDashboard getDashboard(long userId) {
    log.info("getDashboard start userId={}", userId);

    userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    int totalActiveEmployees = safeInt(employeeRepository.countActive());
    int totalActiveHeadquarters = safeInt(headquarterRepository.countActive());
    int totalOpenTasks = safeInt(taskRepository.countOpenTasks());
    int totalPendingTasks = safeInt(taskRepository.countPendingTasks());
    var dashboard = new UserManagerDashboard(
        totalActiveEmployees,
        totalActiveHeadquarters,
        totalOpenTasks,
        totalPendingTasks);

    log.info("getDashboard complete userId={}", userId);
    return dashboard;
  }

  private static int safeInt(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) value;
  }

  @Override
  @Transactional(readOnly = true)
  public User getProfile(long userId) {
    log.info("getProfile start userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    log.info("getProfile complete userId={}", userId);
    return user;
  }

  @Override
  @Transactional
  public User updateProfile(long userId, UpdateProfileCommand command) {
    log.info("updateProfile start userId={} (command fields not logged)", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    user.updateProfile(
        command.firstName(),
        command.lastName(),
        command.gender(),
        command.phone());

    User saved = userRepository.save(user);

    log.info("updateProfile complete userId={}", userId);
    return saved;
  }
}