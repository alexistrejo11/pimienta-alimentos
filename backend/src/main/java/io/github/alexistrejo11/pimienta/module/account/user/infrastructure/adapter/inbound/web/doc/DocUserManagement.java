package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** OpenAPI tag for {@code /api/v1/users/management}. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag(
    name = "Account — User management",
    description =
        """
        Administrative APIs under `/api/v1/users/management`. **Requires** `Authorization: Bearer \
         <access_token>`. Supported account roles include `ROLE_ADMIN`, `ROLE_DIRECTOR`, `ROLE_MANAGER`, \
         `ROLE_SALES`, `ROLE_EMPLOYEE`, `ROLE_SUPPORT`, and baseline `ROLE_USER`. \
        for operators; fine-grained authorization may be enforced in the application layer as the product \
        evolves.""")
public @interface DocUserManagement {}
