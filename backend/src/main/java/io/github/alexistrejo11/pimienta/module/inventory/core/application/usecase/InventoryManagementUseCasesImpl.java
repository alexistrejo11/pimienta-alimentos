package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.InventoryDashboard;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventorySearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryGlobalSummary;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryAlreadyExistsException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.StorageLocationNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryCountRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryGlobalSummaryRepository;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class InventoryManagementUseCasesImpl implements InventoryManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(InventoryManagementUseCasesImpl.class);

  private final InventoryRepository inventoryRepository;
  private final ItemRepository itemRepository;
  private final StorageLocationRepository storageLocationRepository;
  private final InventoryGlobalSummaryRepository globalSummaryRepository;
  private final InventoryCountRepository inventoryCountRepository;

  public InventoryManagementUseCasesImpl(
      InventoryRepository inventoryRepository,
      ItemRepository itemRepository,
      StorageLocationRepository storageLocationRepository,
      InventoryGlobalSummaryRepository globalSummaryRepository,
      InventoryCountRepository inventoryCountRepository) {
    this.inventoryRepository = inventoryRepository;
    this.itemRepository = itemRepository;
    this.storageLocationRepository = storageLocationRepository;
    this.globalSummaryRepository = globalSummaryRepository;
    this.inventoryCountRepository = inventoryCountRepository;
  }

  @Override
  public Page<InventoryGlobalSummary> searchGlobalSummary(String search, Item.ItemCategory category, Inventory.InventoryStatus status, java.math.BigDecimal minCost, java.math.BigDecimal maxCost, List<Long> headquarterIds, Pageable pageable) {
    return globalSummaryRepository.search(search, category, status, minCost, maxCost, headquarterIds, pageable);
  }

  @Override
  public InventoryDashboard dashboard(List<Long> headquarterIds) {
    InventoryDashboard stock = globalSummaryRepository.stockKpis(headquarterIds);
    long openCounts = inventoryCountRepository.countOpen(headquarterIds);
    return new InventoryDashboard(
        stock.skuCount(),
        stock.lowStockCount(),
        stock.outOfStockCount(),
        openCounts,
        stock.totalAvailableQuantity(),
        stock.totalStockValue());
  }

  @Override
  public Page<Inventory> search(InventorySearchCriteria criteria, Pageable pageable) {
    InventorySearchCriteria effective = criteria != null ? criteria : InventorySearchCriteria.empty();

    log.debug(
        "search inventory balances query start page={} size={} itemId={} locationId={} status={} headquarterId={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.itemId(),
        effective.locationId(),
        effective.status(),
        effective.headquarterIds());

    Page<Inventory> page = inventoryRepository.search(effective, pageable);

    log.debug(
        "search inventory balances query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Inventory getById(Long id) {
    log.debug("get inventory by id query start inventoryId={}", id);

    Inventory inv = inventoryRepository.findById(id).orElseThrow(() -> new InventoryNotFoundException(id));

    log.debug("get inventory by id query complete inventoryId={}", inv.getId());
    return inv;
  }

  @Override
  public List<Inventory> findByItemId(Long itemId) {
    log.debug("find inventory by item query start itemId={}", itemId);

    List<Inventory> list = inventoryRepository.findByItemId(itemId);

    log.debug("find inventory by item query complete itemId={} size={}", itemId, list.size());
    return list;
  }

  @Override
  public List<Inventory> findByLocationId(Long locationId) {
    log.debug("find inventory by location query start locationId={}", locationId);

    List<Inventory> list = inventoryRepository.findByLocationId(locationId);

    log.debug("find inventory by location query complete locationId={} size={}", locationId, list.size());
    return list;
  }

  @Override
  public Page<Inventory> findLowStock(InventorySearchCriteria criteria, Pageable pageable) {
    log.debug(
        "find low stock inventory query start page={} size={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null);

    Page<Inventory> page = inventoryRepository.findLowStock(criteria, pageable);

    log.debug(
        "find low stock inventory query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Page<Inventory> findOutOfStock(InventorySearchCriteria criteria, Pageable pageable) {
    log.debug(
        "find out of stock inventory query start page={} size={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null);

    Page<Inventory> page = inventoryRepository.findOutOfStock(criteria, pageable);

    log.debug(
        "find out of stock inventory query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Inventory createInitialStock(long itemId, long locationId, int initialQuantity) {
    log.info(
        "create initial stock start itemId={} locationId={} initialQuantity={}",
        itemId,
        locationId,
        initialQuantity);

    if (inventoryRepository.findByItemIdAndLocationId(itemId, locationId).isPresent()) {
      throw new InventoryAlreadyExistsException(itemId, locationId);
    }
    Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
    StorageLocation location = storageLocationRepository
        .findById(locationId)
        .orElseThrow(() -> new StorageLocationNotFoundException(locationId));
    if (location.getStatus() == LocationStatus.BLOCKED) {
      throw new BusinessValidationException(
          "Cannot create stock in a blocked location.",
          Map.of("locationId", locationId),
          "location blocked");
    }
    Inventory inv = Inventory.create(item, location, initialQuantity);

    Inventory saved = inventoryRepository.save(inv);

    log.info("create initial stock complete inventoryId={}", saved.getId());
    return saved;
  }
}
