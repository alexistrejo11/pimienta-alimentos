package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductCatalogPolicy;
import org.springframework.stereotype.Component;

@Component
public class ProductCatalogPolicyImpl implements ProductCatalogPolicy {

  private final HeadquarterItemRepository headquarterItemRepository;

  public ProductCatalogPolicyImpl(HeadquarterItemRepository headquarterItemRepository) {
    this.headquarterItemRepository = headquarterItemRepository;
  }

  @Override
  public boolean isControlledAtAnyHeadquarter(long productId) {
    return headquarterItemRepository.existsControlledByProductId(productId);
  }
}
