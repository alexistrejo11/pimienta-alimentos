package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;

public record PosSaleReportRow(PosSale sale, PosEventResultStatus syncStatus) {}
