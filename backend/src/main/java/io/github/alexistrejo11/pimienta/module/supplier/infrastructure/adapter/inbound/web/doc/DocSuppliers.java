package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc;

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
    name = "Operations — Suppliers",
    description =
        """
        Vendor contact directory (name, contact, phone, parent brand) linked to headquarters. \
        Requires staff JWT. OPS roles: ADMIN, DIRECTOR, MANAGER, EMPLOYEE.""")
public @interface DocSuppliers {}
