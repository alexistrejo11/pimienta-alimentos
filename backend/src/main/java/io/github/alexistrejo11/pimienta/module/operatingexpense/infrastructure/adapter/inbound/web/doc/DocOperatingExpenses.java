package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc;

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
    name = "Operations — OperatingExpenses",
    description =
        """
        Operating expenses (gastos operativos) with optional receipt images. ADMIN and DIRECTOR have full access. \
        MANAGER and EMPLOYEE can list and create only for today at an assigned sede. \
        Create and update use multipart: JSON part `data` and optional file part `attachments`.""")
public @interface DocOperatingExpenses {}
