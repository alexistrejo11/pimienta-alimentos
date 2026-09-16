package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.CreatePosOperatorCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.UpdatePosOperatorCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosOperatorNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.OperatorManagementUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncTombstoneRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorManagementUseCasesImpl implements OperatorManagementUseCases {

  private final PosOperatorRepository operatorRepository;
  private final HeadquarterRepository headquarterRepository;
  private final PosSyncTombstoneRepository tombstoneRepository;
  private final PosChangeLogService posChangeLogService;
  private final PasswordEncoder pinPasswordEncoder;

  public OperatorManagementUseCasesImpl(
      PosOperatorRepository operatorRepository,
      HeadquarterRepository headquarterRepository,
      PosSyncTombstoneRepository tombstoneRepository,
      PosChangeLogService posChangeLogService,
      @Qualifier("pinPasswordEncoder") PasswordEncoder pinPasswordEncoder) {
    this.operatorRepository = operatorRepository;
    this.headquarterRepository = headquarterRepository;
    this.tombstoneRepository = tombstoneRepository;
    this.posChangeLogService = posChangeLogService;
    this.pinPasswordEncoder = pinPasswordEncoder;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosOperator> list(Long headquarterId, Pageable pageable) {
    if (headquarterId == null) {
      return operatorRepository.findAll(pageable);
    }
    return operatorRepository.findByHeadquarterId(headquarterId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public PosOperator get(long id) {
    return operatorRepository.findById(id).orElseThrow(() -> new PosOperatorNotFoundException(id));
  }

  @Override
  @Transactional
  public PosOperator create(CreatePosOperatorCommand command) {
    Set<Long> hqIds =
        command.headquarterIds() != null ? new HashSet<>(command.headquarterIds()) : Set.of();
    for (Long hqId : hqIds) {
      ensureHeadquarter(hqId);
    }
    PosOperator operator =
        PosOperator.builder()
            .withDisplayName(command.displayName())
            .withPosRole(command.posRole() != null ? command.posRole() : PosRole.CASHIER)
            .withPinHash(pinPasswordEncoder.encode(command.pin()))
            .withUserId(command.userId())
            .withActive(true)
            .withHeadquarterIds(hqIds)
            .register();
    PosOperator saved = operatorRepository.save(operator);
    hqIds.forEach(hqId -> posChangeLogService.appendOperator(hqId, saved.getId(), "UPSERT"));
    return saved;
  }

  @Override
  @Transactional
  public PosOperator update(long id, UpdatePosOperatorCommand command) {
    PosOperator operator = get(id);
    if (command.displayName() != null) {
      operator.setDisplayName(command.displayName());
    }
    if (command.posRole() != null) {
      operator.setPosRole(command.posRole());
    }
    if (command.pin() != null && !command.pin().isBlank()) {
      operator.setPinHash(pinPasswordEncoder.encode(command.pin()));
    }
    if (command.userId() != null) {
      operator.setUserId(command.userId());
    }
    if (command.active() != null) {
      operator.setActive(command.active());
    }
    if (command.headquarterIds() != null) {
      command.headquarterIds().forEach(this::ensureHeadquarter);
      operator.replaceHeadquarters(command.headquarterIds());
    }
    operator.touch();
    PosOperator saved = operatorRepository.save(operator);
    saved.getHeadquarterIds()
        .forEach(hqId -> posChangeLogService.appendOperator(hqId, saved.getId(), "UPSERT"));
    return saved;
  }

  @Override
  @Transactional
  public PosOperator assignHeadquarter(long operatorId, long headquarterId) {
    ensureHeadquarter(headquarterId);
    PosOperator operator = get(operatorId);
    operator.assignHeadquarter(headquarterId);
    PosOperator saved = operatorRepository.save(operator);
    saved.getHeadquarterIds()
        .forEach(hqId -> posChangeLogService.appendOperator(hqId, saved.getId(), "UPSERT"));
    return saved;
  }

  @Override
  @Transactional
  public PosOperator unassignHeadquarter(long operatorId, long headquarterId) {
    PosOperator operator = get(operatorId);
    boolean wasAssigned = operator.getHeadquarterIds().contains(headquarterId);
    operator.unassignHeadquarter(headquarterId);
    PosOperator saved = operatorRepository.save(operator);
    if (wasAssigned) {
      tombstoneRepository.save(PosSyncTombstone.operator(headquarterId, operatorId));
      posChangeLogService.appendOperator(headquarterId, operatorId, "DEACTIVATE");
    }
    return saved;
  }

  @Override
  @Transactional
  public PosOperator softDelete(long operatorId) {
    PosOperator operator = get(operatorId);
    operator.softDelete();
    PosOperator saved = operatorRepository.save(operator);
    for (Long headquarterId : operator.getHeadquarterIds()) {
      tombstoneRepository.save(PosSyncTombstone.operator(headquarterId, operatorId));
      posChangeLogService.appendOperator(headquarterId, operatorId, "DEACTIVATE");
    }
    return saved;
  }

  private void ensureHeadquarter(long headquarterId) {
    headquarterRepository
        .findById(headquarterId)
        .orElseThrow(() -> new HeadquarterNotFoundException(headquarterId));
  }
}
