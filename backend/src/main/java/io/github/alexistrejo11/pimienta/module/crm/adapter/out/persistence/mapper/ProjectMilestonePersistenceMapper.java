package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.ProjectMilestoneJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.ProjectMilestone;

public class ProjectMilestonePersistenceMapper {

  private ProjectMilestonePersistenceMapper() {}

  public static ProjectMilestoneJpaEntity toJpa(ProjectMilestone domain) {
    ProjectMilestoneJpaEntity e = new ProjectMilestoneJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setProjectId(domain.getProjectId());
    e.setName(domain.getName());
    e.setDescription(domain.getDescription());
    e.setStatus(domain.getStatus());
    e.setPlannedDate(domain.getPlannedDate());
    e.setActualDate(domain.getActualDate());
    e.setBillingAmount(domain.getBillingAmount());
    e.setBilled(domain.isBilled());
    e.setSortOrder(domain.getSortOrder());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  public static ProjectMilestone toDomain(ProjectMilestoneJpaEntity e) {
    return ProjectMilestone.builder()
        .withId(e.getId())
        .withProjectId(e.getProjectId())
        .withName(e.getName())
        .withDescription(e.getDescription())
        .withStatus(e.getStatus())
        .withPlannedDate(e.getPlannedDate())
        .withActualDate(e.getActualDate())
        .withBillingAmount(e.getBillingAmount())
        .withBilled(e.isBilled())
        .withSortOrder(e.getSortOrder())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }
}
