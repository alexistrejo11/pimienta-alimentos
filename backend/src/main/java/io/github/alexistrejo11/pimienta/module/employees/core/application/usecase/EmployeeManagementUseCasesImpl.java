package io.github.alexistrejo11.pimienta.module.employees.core.application.usecase;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.employees.core.application.command.ChangeEmployeeStatusCommand;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.RegisterEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.UpdateEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.query.EmployeeSearchCriteria;
import io.github.alexistrejo11.pimienta.module.employees.core.application.storage.EmployeeStorageNaming;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeStatistics;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeSummary;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.exception.EmployeeNotFoundException;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeManagementUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeRepository;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeStorageService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementUseCasesImpl implements EmployeeManagementUseCases {
  private static final Logger log = LoggerFactory.getLogger(EmployeeManagementUseCasesImpl.class);
  private final EmployeeRepository employeeRepository;
  private final EmployeeStorageService storageService;

  public EmployeeManagementUseCasesImpl(
      EmployeeRepository employeeRepository,
      EmployeeStorageService storageService) {
    this.employeeRepository = employeeRepository;
    this.storageService = storageService;
  }

  // -------- Queries --------

  @Override
  @Transactional(readOnly = true)
  public EmployeeSummary summary() {
    log.debug("employee summary query start");

    EmployeeSummary s = employeeRepository.summarize();

    log.debug(
        "employee summary query complete totalNotDeleted={} departmentBuckets={}",
        s.totalNotDeleted(),
        s.headcountByDepartment() != null ? s.headcountByDepartment().size() : 0);
    return s;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Employee> search(EmployeeSearchCriteria criteria, Pageable pageable) {
    EmployeeSearchCriteria effective = criteria != null ? criteria : EmployeeSearchCriteria.empty();

    log.debug(
        "search employees query start page={} size={} status={} department={} textLen={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.status(),
        effective.department(),
        effective.text() != null ? effective.text().length() : 0);

    Page<Employee> page = employeeRepository.search(effective, pageable);

    log.debug(
        "search employees query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  @Transactional(readOnly = true)
  public Employee getById(Long id) {
    log.debug("get employee by id query start employeeId={}", id);

    Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));

    log.debug("get employee by id query complete employeeId={}", employee.getId());
    return employee;
  }

  @Override
  @Transactional(readOnly = true)
  public EmployeeStatistics statistics() {
    log.debug("employee statistics query start");

    EmployeeStatistics stats = employeeRepository.statistics();

    log.debug(
        "employee statistics query complete total={} active={} notActive={}",
        stats.total(),
        stats.active(),
        stats.notActive());
    return stats;
  }

  // -------- Commands --------

  @Override
  @Transactional
  public Employee register(RegisterEmployeeParams params) {
    log.info(
        "register employee start contractType={} workShift={} department={} status={} hireDate={}",
        params.contractType(),
        params.workShift(),
        params.department(),
        params.status(),
        params.hireDate());

    String photoUrl = null;
    if (params.photo() != null && !params.photo().isEmpty()) {
      String segment = EmployeeStorageNaming.displayNameSegment(params.firstName(), params.lastName());
      photoUrl = storageService.uploadEmployeePhoto(params.photo(), segment);
    }

    Employee created = params.toEmployee(photoUrl);
    Employee saved = employeeRepository.save(created);

    log.info("register employee complete employeeId={}", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public Employee update(Long id, UpdateEmployeeParams params) {
    log.info("update employee start employeeId={}", id);
    Employee employee = getById(id);

    String photoUrl = null;
    if (params.photo() != null && !params.photo().isEmpty()) {
      String existing = employee.getPersonal().photoUrl();
      if (existing != null && !existing.isBlank()) {
        storageService.delete(existing);
      }
      String fn = params.firstName() != null ? params.firstName() : employee.getPersonal().firstName();
      String ln = params.lastName() != null ? params.lastName() : employee.getPersonal().lastName();
      String segment = EmployeeStorageNaming.displayNameSegment(fn, ln);
      photoUrl = storageService.uploadEmployeePhoto(params.photo(), segment);
      log.debug("photo uploaded for employeeId={}", id);

      employee.setPersonal(employee.getPersonal().setPhotoUrl(photoUrl));
      log.debug("photo set for employeeId={}", id);
    }

    params.applyPartialMerge(employee, photoUrl);
    employee.touch();
    Employee saved = employeeRepository.save(employee);

    log.info("update employee complete employeeId={}", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public Employee changeStatus(ChangeEmployeeStatusCommand command) {
    log.info(
        "change employee status start employeeId={} status={}",
        command.employeeId(),
        command.status());

    Employee employee = getById(command.employeeId());
    applyStatusMutation(employee, command.status());
    Employee saved = employeeRepository.save(employee);

    log.info(
        "change employee status complete employeeId={} status={}",
        saved.getId(),
        saved.getStatus());
    return saved;
  }

  @Override
  @Transactional
  public void delete(Long id) {
    log.info("delete employee start employeeId={}", id);
    Employee employee = getById(id);

    employee.markAsDeleted();
    employeeRepository.save(employee);

    log.info("delete employee complete employeeId={}", id);
  }

  /**
   * Applies status and side-effects on termination dates. Exit statuses stamp today when
   * missing; ACTIVE / INACTIVE clear termination date.
   */
  private static void applyStatusMutation(Employee employee, EmployeeStatus status) {
    employee.setStatus(status);
    switch (status) {
      case RESIGNED, TERMINATED, DOWN -> {
        if (employee.getEmployment().terminationDate() == null) {
          employee.setTerminationDate(LocalDate.now(ClientTimeZones.current()));
        }
      }
      case ACTIVE, INACTIVE -> employee.setTerminationDate(null);
    }
    employee.touch();
  }
}
