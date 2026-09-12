package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc;

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
    name = "CRM — Clients",
    description =
        """
        CRM client accounts for project linkage. Requires `Authorization: Bearer <access_token>`.""")
public @interface DocClients {}
