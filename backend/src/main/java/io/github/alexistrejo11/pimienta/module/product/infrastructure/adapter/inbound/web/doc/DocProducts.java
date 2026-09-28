package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Tag(name = "Products", description = "Sale catalog. SKU is assigned by the database.")
public @interface DocProducts {}
