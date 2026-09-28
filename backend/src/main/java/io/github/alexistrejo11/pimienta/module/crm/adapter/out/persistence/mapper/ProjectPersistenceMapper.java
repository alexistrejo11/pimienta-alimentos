package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.ProjectJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Project;

public class ProjectPersistenceMapper {

  private ProjectPersistenceMapper() {}

  public static ProjectJpaEntity toJpa(Project domain) {
    ProjectJpaEntity e = new ProjectJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setClientId(domain.getClientId());
    e.setOriginOpportunityId(domain.getOriginOpportunityId());
    e.setProjectCode(domain.getProjectCode());
    e.setProjectName(domain.getProjectName());
    e.setDescription(domain.getDescription());
    e.setType(domain.getType());
    e.setStatus(domain.getStatus());
    e.setPriority(domain.getPriority());
    e.setProjectManagerId(domain.getProjectManagerId());
    e.setAssignedSalesmanId(domain.getAssignedSalesmanId());
    e.setPlannedStartDate(domain.getPlannedStartDate());
    e.setPlannedEndDate(domain.getPlannedEndDate());
    e.setActualStartDate(domain.getActualStartDate());
    e.setActualEndDate(domain.getActualEndDate());
    e.setOnHoldReason(domain.getOnHoldReason());
    e.setContractedValue(domain.getContractedValue());
    e.setEstimatedCost(domain.getEstimatedCost());
    e.setActualCost(domain.getActualCost());
    e.setProgressPercent(domain.getProgressPercent());
    e.setCancellationReason(domain.getCancellationReason());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  public static Project toDomain(ProjectJpaEntity e) {
    return Project.builder()
        .withId(e.getId())
        .withClientId(e.getClientId())
        .withOriginOpportunityId(e.getOriginOpportunityId())
        .withProjectCode(e.getProjectCode())
        .withProjectName(e.getProjectName())
        .withDescription(e.getDescription())
        .withType(e.getType())
        .withStatus(e.getStatus())
        .withPriority(e.getPriority())
        .withProjectManagerId(e.getProjectManagerId())
        .withAssignedSalesmanId(e.getAssignedSalesmanId())
        .withPlannedStartDate(e.getPlannedStartDate())
        .withPlannedEndDate(e.getPlannedEndDate())
        .withActualStartDate(e.getActualStartDate())
        .withActualEndDate(e.getActualEndDate())
        .withOnHoldReason(e.getOnHoldReason())
        .withContractedValue(e.getContractedValue())
        .withEstimatedCost(e.getEstimatedCost())
        .withActualCost(e.getActualCost())
        .withProgressPercent(e.getProgressPercent())
        .withCancellationReason(e.getCancellationReason())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }
}
