package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.PosSaleCategoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosSaleCategoryRepository;
import io.github.alexistrejo11.pimienta.module.product.core.application.command.CreateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import io.github.alexistrejo11.pimienta.module.product.core.port.input.ProductManagementUseCases;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosProductManagementUseCasesImpl implements PosProductManagementUseCases {

  private final HeadquarterRepository headquarters;
  private final ProductManagementUseCases products;
  private final HeadquarterItemRepository catalog;
  private final PosSaleCategoryRepository categories;

  public PosProductManagementUseCasesImpl(
      HeadquarterRepository headquarters,
      ProductManagementUseCases products,
      HeadquarterItemRepository catalog,
      PosSaleCategoryRepository categories) {
    this.headquarters = headquarters;
    this.products = products;
    this.catalog = catalog;
    this.categories = categories;
  }

  @Override
  @Transactional
  public CreatedPosProduct create(long headquarterId, CreatePosProductCommand command) {
    headquarters.findById(headquarterId).orElseThrow();

    var category =
        categories
            .findById(headquarterId, command.posSaleCategoryId())
            .filter(row -> row.isActive())
            .orElseThrow(() -> new PosSaleCategoryNotFoundException(command.posSaleCategoryId()));

    StockPolicy policy =
        command.stockPolicy() != null ? command.stockPolicy() : StockPolicy.NOT_CONTROLLED;
    Unit unit = command.unit() != null ? command.unit() : Unit.PIECE;

    Product saved =
        products.create(
            new CreateProductCommand(
                command.name(),
                command.description(),
                unit,
                command.barcode(),
                policy == StockPolicy.CONTROLLED));

    HeadquarterItem row =
        HeadquarterItem.builder()
            .withHeadquarterId(headquarterId)
            .withProductId(saved.getId())
            .withPosSaleCategoryId(category.getId())
            .withSaleCategory(category.getName())
            .withSalePrice(command.salePrice())
            .withAvailable(command.available() == null || command.available())
            .withStockPolicy(policy)
            .withNegativeStockLimit(command.negativeStockLimit())
            .register();

    return new CreatedPosProduct(saved, catalog.save(row));
  }
}
