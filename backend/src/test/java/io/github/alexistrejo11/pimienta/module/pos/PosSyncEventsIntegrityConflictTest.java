package io.github.alexistrejo11.pimienta.module.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosSaleInventoryUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosEventStockProjector;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosSyncEventsUseCasesImpl;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.IngestPosEventItem;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncEventsUseCases.EventIngestResult;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosShiftRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncEventRepository;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

class PosSyncEventsIntegrityConflictTest {

  @Test
  void integrityConflictWithoutPriorEvent_isStoredAsRejectedInsteadOfFailingBatch() {
    UUID deviceId = UUID.randomUUID();
    long hqId = 7L;
    PosDevice device =
        PosDevice.builder()
            .withId(deviceId)
            .withHeadquarterId(hqId)
            .withStatus(PosDeviceStatus.AUTHORIZED)
            .reconstruct();

    PosDeviceRepository deviceRepository = mock(PosDeviceRepository.class);
    when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
    PosSyncEventRepository syncEventRepository = mock(PosSyncEventRepository.class);
    when(syncEventRepository.findByEventId(any())).thenReturn(Optional.empty());
    when(syncEventRepository.findByDeviceIdAndDeviceSequence(any(), anyLong()))
        .thenReturn(Optional.empty());
    when(syncEventRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("ux_pos_shifts_active_operator"))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PosSyncEventsUseCasesImpl useCases =
        new PosSyncEventsUseCasesImpl(
            deviceRepository,
            syncEventRepository,
            mock(PosSaleRepository.class),
            mock(HeadquarterItemRepository.class),
            mock(PosOperationalConfigRepository.class),
            mock(PosSaleInventoryUseCases.class),
            mock(PosEventStockProjector.class),
            mock(PlatformTransactionManager.class),
            mock(PosShiftRepository.class),
            mock(ProductRepository.class));

    UUID eventId = UUID.randomUUID();
    IngestPosEventItem item =
        new IngestPosEventItem(
            eventId,
            "DEVICE_HEARTBEAT",
            1,
            deviceId,
            String.valueOf(hqId),
            42L,
            null,
            null,
            Instant.parse("2026-10-01T18:00:00Z"),
            "{}",
            null);

    List<EventIngestResult> results =
        useCases.ingest(deviceId, new IngestPosEventsCommand(List.of(item)));

    assertEquals(1, results.size());
    assertEquals(PosEventResultStatus.REJECTED, results.getFirst().status());
    assertEquals(eventId, results.getFirst().eventId());

    ArgumentCaptor<PosSyncEvent> saved = ArgumentCaptor.forClass(PosSyncEvent.class);
    verify(syncEventRepository, times(2)).save(saved.capture());
    PosSyncEvent rejected = saved.getAllValues().get(1);
    assertEquals(PosEventResultStatus.REJECTED, rejected.getStatus());
    assertEquals(42L, rejected.getDeviceSequence());
    verify(deviceRepository).save(any());
  }
}
