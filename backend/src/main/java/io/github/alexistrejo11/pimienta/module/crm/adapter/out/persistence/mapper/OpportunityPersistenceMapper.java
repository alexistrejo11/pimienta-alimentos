package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.OpportunityJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity;

public class OpportunityPersistenceMapper {

  private OpportunityPersistenceMapper() {}

  public static OpportunityJpaEntity toJpa(Opportunity domain) {
    OpportunityJpaEntity e = new OpportunityJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setContactName(domain.getContactName());
    e.setContactEmail(domain.getContactEmail());
    e.setContactPhone(domain.getContactPhone());
    e.setCompanyName(domain.getCompanyName());
    e.setCompanyLocation(domain.getCompanyLocation());
    e.setIndustry(domain.getIndustry());
    e.setTitle(domain.getTitle());
    e.setDescription(domain.getDescription());
    e.setEstimatedValue(domain.getEstimatedValue());
    e.setProbabilityPercent(domain.getProbabilityPercent());
    e.setSource(domain.getSource());
    e.setStatus(domain.getStatus());
    e.setExpectedCloseDate(domain.getExpectedCloseDate());
    e.setActualCloseDate(domain.getActualCloseDate());
    e.setAssignedSalesmanId(domain.getAssignedSalesmanId());
    e.setLostReason(domain.getLostReason());
    e.setConvertedProjectId(domain.getConvertedProjectId());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  public static Opportunity toDomain(OpportunityJpaEntity e) {
    return Opportunity.builder()
        .withId(e.getId())
        .withContactName(e.getContactName())
        .withContactEmail(e.getContactEmail())
        .withContactPhone(e.getContactPhone())
        .withCompanyName(e.getCompanyName())
        .withCompanyLocation(e.getCompanyLocation())
        .withIndustry(e.getIndustry())
        .withTitle(e.getTitle())
        .withDescription(e.getDescription())
        .withEstimatedValue(e.getEstimatedValue())
        .withProbabilityPercent(e.getProbabilityPercent())
        .withSource(e.getSource())
        .withStatus(e.getStatus())
        .withExpectedCloseDate(e.getExpectedCloseDate())
        .withActualCloseDate(e.getActualCloseDate())
        .withAssignedSalesmanId(e.getAssignedSalesmanId())
        .withLostReason(e.getLostReason())
        .withConvertedProjectId(e.getConvertedProjectId())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }
}
