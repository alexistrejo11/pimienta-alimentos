package io.github.alexistrejo11.pimienta.module.product.core.port.output;

/** Whether any headquarter sells this product with controlled stock. */
public interface ProductCatalogPolicy {

  boolean isControlledAtAnyHeadquarter(long productId);
}
