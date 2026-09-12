package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosSaleCategorySpringDataRepository extends JpaRepository<PosSaleCategoryJpaEntity, Long> {
  List<PosSaleCategoryJpaEntity> findByHeadquarterIdAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(Long id);
  List<PosSaleCategoryJpaEntity> findByHeadquarterIdAndDeletedAtIsNullAndActiveTrueOrderByDisplayOrderAscNameAsc(Long id);
  Optional<PosSaleCategoryJpaEntity> findByIdAndHeadquarterIdAndDeletedAtIsNull(Long id, Long headquarterId);
  @Query("select c from PosSaleCategoryJpaEntity c where c.headquarterId = :hq and c.deletedAt is null and c.active = true and lower(c.name) = lower(:name)")
  Optional<PosSaleCategoryJpaEntity> findActiveByName(@Param("hq") Long hq, @Param("name") String name);
  @Query("select count(h) from HeadquarterItemJpaEntity h where h.headquarterId = :hq and h.posSaleCategoryId = :category and h.deletedAt is null")
  long countActiveCatalogItems(@Param("hq") Long hq, @Param("category") Long category);
}
