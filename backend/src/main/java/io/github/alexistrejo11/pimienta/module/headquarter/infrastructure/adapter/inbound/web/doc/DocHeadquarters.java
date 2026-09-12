package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** OpenAPI tag for {@code /api/v1/headquarters}. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag(
    name = "Operations — Headquarters",
    description =
        """
        Company sites / branches: CRUD, lookup by id or name, statistics, and Excel import/export. \
        Requires `Authorization: Bearer <access_token>`. Reads are available to authenticated staff; \
        create, update, soft-delete, and import require **ROLE_ADMIN**. Not-found responses use \
        `HEADQUARTER_NOT_FOUND` in `ApiErrorResponse`.""")
public @interface DocHeadquarters {}
