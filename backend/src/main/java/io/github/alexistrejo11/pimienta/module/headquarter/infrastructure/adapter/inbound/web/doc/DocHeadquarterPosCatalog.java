package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag(
    name = "Headquarters — POS catalog",
    description =
        """
        Effective POS catalog (`headquarter_items`) per headquarter: sale category, sale price, \
        availability, stock policy. Staff JWT. Global item master remains under `/api/v1/inventory/items`.""")
public @interface DocHeadquarterPosCatalog {}
