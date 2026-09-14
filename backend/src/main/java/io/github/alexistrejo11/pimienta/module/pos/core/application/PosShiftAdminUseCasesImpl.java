package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosShiftAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosShiftRepository;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.List; import java.util.Map; import java.util.UUID;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.stereotype.Service;

@Service
public class PosShiftAdminUseCasesImpl implements PosShiftAdminUseCases {
  private final PosShiftRepository repository;
  public PosShiftAdminUseCasesImpl(PosShiftRepository repository){this.repository=repository;}
  @Override public Page<PosShift> list(List<Long> hqs, Pageable page){return repository.findByHeadquarterIds(hqs,page);}
  @Override public ShiftDetail get(UUID id,long hq){
    PosShift shift=repository.findById(id,hq).orElseThrow(()->new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,"Turno no encontrado",Map.of("shiftId",id),"POS shift not found: "+id));
    return new ShiftDetail(shift,repository.movements(id),repository.counts(id));
  }
}
