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
    name = "Headquarters — POS settings",
    description =
        """
        Per-headquarter POS operational config (currency, catalog stale thresholds, open-amount \
        categories, default negative stock limit). Staff JWT. Upsert also ensures the canonical \
        POS storage location (`POS-{id}`).""")
public @interface DocHeadquarterPosSettings {}
