package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

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
    name = "POS Admin Reports",
    description = "Read-only POS reports over ACCEPTED facts (staff JWT).")
public @interface DocPosAdminReports {}
