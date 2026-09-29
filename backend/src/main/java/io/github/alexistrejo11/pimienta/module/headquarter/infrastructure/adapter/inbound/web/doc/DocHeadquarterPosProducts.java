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
    name = "Headquarters — POS products",
    description =
        """
        Creates a global sellable product and the headquarter catalog row in one call. \
        Staff JWT with access to the headquarter (ADMIN, DIRECTOR, MANAGER, EMPLOYEE).""")
public @interface DocHeadquarterPosProducts {}
