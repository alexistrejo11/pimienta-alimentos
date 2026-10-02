package io.github.alexistrejo11.pimienta.module.product.core.port.output;

/** Tells each sede that sells this product to refresh name and barcode. */
public interface ProductIdentityChangePort {

  void publish(long productId);
}
