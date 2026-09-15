package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosShiftListFilter;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosShiftAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosShiftRepository;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PosShiftAdminUseCasesImpl implements PosShiftAdminUseCases {

  private final PosShiftRepository repository;
  private final PosOperatorRepository operatorRepository;
  private final PosDeviceRepository deviceRepository;
  private final PosSaleRepository saleRepository;

  public PosShiftAdminUseCasesImpl(
      PosShiftRepository repository,
      PosOperatorRepository operatorRepository,
      PosDeviceRepository deviceRepository,
      PosSaleRepository saleRepository) {
    this.repository = repository;
    this.operatorRepository = operatorRepository;
    this.deviceRepository = deviceRepository;
    this.saleRepository = saleRepository;
  }

  @Override
  public Page<PosShiftListItem> list(
      List<Long> hqs, PosShiftListFilter filter, Pageable page) {
    Page<PosShift> shifts = repository.findByHeadquarterIds(hqs, filter, page);
    return enrichPage(shifts);
  }

  @Override
  public ShiftDetail get(UUID id, long hq) {
    PosShift shift =
        repository
            .findById(id, hq)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Turno no encontrado",
                        Map.of("shiftId", id),
                        "POS shift not found: " + id));
    return new ShiftDetail(
        enrichOne(shift), repository.movements(id), repository.counts(id));
  }

  @Override
  public ShiftReconciliationDetail getReconciliation(UUID id, long hq) {
    ShiftDetail detail = get(id, hq);
    PosShiftSalesSummary sales = saleRepository.summarizeAcceptedSalesForShift(id);
    return new ShiftReconciliationDetail(
        detail.listItem(), detail.movements(), detail.counts(), sales);
  }

  private Page<PosShiftListItem> enrichPage(Page<PosShift> shifts) {
    List<PosShift> content = shifts.getContent();
    if (content.isEmpty()) {
      return new PageImpl<>(List.of(), shifts.getPageable(), shifts.getTotalElements());
    }
    Set<Long> operatorIds = new HashSet<>();
    Set<UUID> deviceIds = new HashSet<>();
    for (PosShift shift : content) {
      if (shift.cashierOperatorId() != null) {
        operatorIds.add(shift.cashierOperatorId());
      }
      deviceIds.add(shift.deviceId());
    }
    Map<Long, String> operatorNames = loadOperatorNames(operatorIds);
    Map<UUID, PosDevice> devices = loadDevices(deviceIds);
    List<PosShiftListItem> items =
        content.stream()
            .map(s -> toListItem(s, operatorNames, devices))
            .toList();
    return new PageImpl<>(items, shifts.getPageable(), shifts.getTotalElements());
  }

  private PosShiftListItem enrichOne(PosShift shift) {
    Map<Long, String> operatorNames =
        shift.cashierOperatorId() != null
            ? loadOperatorNames(Set.of(shift.cashierOperatorId()))
            : Map.of();
    Map<UUID, PosDevice> devices = loadDevices(Set.of(shift.deviceId()));
    return toListItem(shift, operatorNames, devices);
  }

  private static PosShiftListItem toListItem(
      PosShift shift, Map<Long, String> operatorNames, Map<UUID, PosDevice> devices) {
    String cashierName =
        shift.cashierOperatorId() != null
            ? operatorNames.getOrDefault(shift.cashierOperatorId(), "")
            : "";
    PosDevice device = devices.get(shift.deviceId());
    String deviceName = device != null ? device.getDeviceName() : "";
    String visibleCode = device != null ? device.getVisibleCode() : "";
    return new PosShiftListItem(shift, cashierName, deviceName, visibleCode);
  }

  private Map<Long, String> loadOperatorNames(Set<Long> operatorIds) {
    Map<Long, String> names = new HashMap<>();
    for (Long id : operatorIds) {
      operatorRepository
          .findById(id)
          .ifPresent(op -> names.put(id, op.getDisplayName()));
    }
    return names;
  }

  private Map<UUID, PosDevice> loadDevices(Set<UUID> deviceIds) {
    Map<UUID, PosDevice> devices = new HashMap<>();
    for (UUID id : deviceIds) {
      deviceRepository.findById(id).ifPresent(d -> devices.put(id, d));
    }
    return devices;
  }
}
