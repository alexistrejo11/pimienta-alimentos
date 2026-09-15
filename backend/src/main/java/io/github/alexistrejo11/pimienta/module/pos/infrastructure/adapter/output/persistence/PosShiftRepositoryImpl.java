package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashCount;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashMovement;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosShiftRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosShiftMaterializationException;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosCashCountJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosCashMovementJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosShiftJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosCashCountSpringDataRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosCashMovementSpringDataRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosShiftSpringDataRepository;
import java.time.Instant; import java.util.List; import java.util.Map; import java.util.Optional; import java.util.UUID;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.stereotype.Repository;

@Repository
public class PosShiftRepositoryImpl implements PosShiftRepository {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final PosShiftSpringDataRepository shifts; private final PosCashMovementSpringDataRepository movements; private final PosCashCountSpringDataRepository counts;
  public PosShiftRepositoryImpl(PosShiftSpringDataRepository shifts, PosCashMovementSpringDataRepository movements, PosCashCountSpringDataRepository counts){this.shifts=shifts;this.movements=movements;this.counts=counts;}
  @Override public void materialize(PosSyncEvent event){
    Map<String,Object> p; try {p=JSON.readValue(event.getPayloadJson(),new TypeReference<>(){});} catch(Exception e){throw invalid("El payload del turno no es válido", "Invalid POS event payload", e);}
    UUID payloadShiftId=uuid(p.get("shiftId"));
    UUID shiftId=event.getShiftId()!=null?event.getShiftId():payloadShiftId;
    if(shiftId==null || (payloadShiftId!=null && !shiftId.equals(payloadShiftId))) throw invalid("El turno indicado no es válido", "shiftId is missing or inconsistent", null);
    switch(event.getEventType()){
      case "SHIFT_OPENED" -> { if(shifts.existsById(shiftId)) return; shifts.save(new PosShiftJpaEntity(shiftId,event.getHeadquarterId(),event.getDeviceId(),nullableLong(p,"cashierOperatorId"),requiredLong(p,"openingCashCentavos"),event.getOccurredAt(),event.getId())); }
      case "CASH_WITHDRAWAL_RECORDED", "CASH_DEPOSIT_RECORDED" -> {PosShiftJpaEntity s=requiredOpenShift(shiftId,event); if(!movements.existsById(event.getAggregateId())) movements.save(new PosCashMovementJpaEntity(event.getAggregateId(),shiftId,event.getHeadquarterId(),event.getId(),event.getEventType().equals("CASH_DEPOSIT_RECORDED")?"DEPOSIT":"WITHDRAWAL",positiveLong(p,"amountCentavos"),string(p.get("folio")),string(p.get("reason")),nullableLong(p,"authorizedByUserId"),string(p.get("authorizedByRole")),event.getOccurredAt()));}
      case "CASH_COUNT_SUBMITTED" -> {requiredOpenShift(shiftId,event); if(!counts.existsById(event.getAggregateId())) counts.save(new PosCashCountJpaEntity(event.getAggregateId(),shiftId,event.getHeadquarterId(),event.getId(),nonNegativeLong(p,"totalCentavos"),requiredJson(p,"denominations"),event.getOccurredAt()));}
      case "SHIFT_CLOSED" -> {PosShiftJpaEntity s=requiredOpenShift(shiftId,event); s.close(event.getOccurredAt(),nonNegativeLong(p,"cashExpectedCentavos"),nonNegativeLong(p,"countedCashCentavos"),requiredLong(p,"differenceCentavos"),event.getId()); shifts.save(s);}
      default -> throw invalid("Tipo de evento de turno no soportado", "Unsupported POS materialization event: "+event.getEventType(), null);
    }
  }
  @Override public Page<PosShift> findByHeadquarterIds(List<Long> hqs, Instant closedFrom, Instant closedTo, String status, Pageable page){
    boolean filtered = closedFrom != null || closedTo != null || status != null;
    if (!filtered) {
      return (hqs == null ? shifts.findAllByOrderByOpenedAtDesc(page) : shifts.findByHeadquarterIdInOrderByOpenedAtDesc(hqs, page)).map(PosShiftRepositoryImpl::shift);
    }
    if (hqs == null) {
      return shifts.findAllFiltered(closedFrom, closedTo, status, page).map(PosShiftRepositoryImpl::shift);
    }
    return shifts.findFilteredByHeadquarterIds(hqs, closedFrom, closedTo, status, page).map(PosShiftRepositoryImpl::shift);
  }
  @Override public Optional<PosShift> findById(UUID id,long hq){return shifts.findByShiftIdAndHeadquarterId(id,hq).map(PosShiftRepositoryImpl::shift);}
  @Override public List<PosCashMovement> movements(UUID id){return movements.findByShiftIdOrderByOccurredAtAsc(id).stream().map(x->new PosCashMovement(x.getMovementId(),x.getShiftId(),x.getMovementType(),x.getAmountCentavos(),x.getFolio(),x.getReason(),x.getOccurredAt())).toList();}
  @Override public List<PosCashCount> counts(UUID id){return counts.findByShiftIdOrderBySubmittedAtAsc(id).stream().map(x->new PosCashCount(x.getCountId(),x.getShiftId(),x.getTotalCentavos(),x.getDenominations(),x.getSubmittedAt())).toList();}
  private static PosShift shift(PosShiftJpaEntity x){return new PosShift(x.getShiftId(),x.getHeadquarterId(),x.getDeviceId(),x.getCashierOperatorId(),x.getOpeningCashCentavos(),x.getOpenedAt(),x.getClosedAt(),x.getStatus(),x.getExpectedCashCentavos(),x.getCountedCashCentavos(),x.getDifferenceCentavos());}
  private PosShiftJpaEntity requiredOpenShift(UUID id, PosSyncEvent event){PosShiftJpaEntity s=shifts.findById(id).orElseThrow(()->invalid("El turno debe abrirse antes de registrar este evento","SHIFT_OPENED must arrive before "+event.getEventType(),null)); if(s.getHeadquarterId()!=event.getHeadquarterId() || !s.getDeviceId().equals(event.getDeviceId()) || "CLOSED".equals(s.getStatus())) throw invalid("El evento no corresponde a un turno abierto","Shift scope or status mismatch",null); return s;}
  private static UUID uuid(Object x){try{return x==null?null:UUID.fromString(String.valueOf(x));}catch(Exception e){return null;}}
  private static long requiredLong(Map<String,Object> p,String key){Object x=p.get(key); if(x instanceof Number n && n.longValue()==n.doubleValue()) return n.longValue(); try{return x==null?fail(key):Long.parseLong(String.valueOf(x));}catch(Exception e){throw invalid("El campo "+key+" debe ser un número entero válido","Invalid numeric field: "+key,e);}}
  private static long nonNegativeLong(Map<String,Object> p,String key){long value=requiredLong(p,key); if(value<0) throw invalid("El campo "+key+" no puede ser negativo","Negative numeric field: "+key,null); return value;}
  private static long positiveLong(Map<String,Object> p,String key){long value=requiredLong(p,key); if(value<=0) throw invalid("El campo "+key+" debe ser positivo","Non-positive numeric field: "+key,null); return value;}
  private static Long nullableLong(Map<String,Object> p,String key){return p.get(key)==null?null:requiredLong(p,key);}
  private static long fail(String key){throw invalid("Falta el campo "+key,"Missing numeric field: "+key,null);}
  private String requiredJson(Map<String,Object> p,String key){Object value=p.get(key); if(value==null) throw invalid("Falta el campo "+key,"Missing JSON field: "+key,null); try{return JSON.writeValueAsString(value);}catch(Exception e){throw invalid("El campo "+key+" no es válido","Invalid JSON field: "+key,e);}}
  private static String string(Object x){return x==null?null:String.valueOf(x);}
  private static PosShiftMaterializationException invalid(String message,String details,Throwable cause){return new PosShiftMaterializationException(message,details+(cause==null?"":" cause="+cause.getMessage()));}
}
