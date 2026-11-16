package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** OpenAPI tag for {@code /api/v1/contracts}. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag(
    name = "Contracts",
    description =
        """
        Business agreements (employment, supplier, customer, …). Requires `Authorization: Bearer \
        <access_token>`. Typical operators: **ADMIN**, **MANAGER**. An employee id is validated \
        when supplied. Domain rules may return \
        `CONTRACT_NOT_FOUND` or validation errors (`errorCode` in `ApiErrorResponse`).""")
public @interface DocContracts {}
