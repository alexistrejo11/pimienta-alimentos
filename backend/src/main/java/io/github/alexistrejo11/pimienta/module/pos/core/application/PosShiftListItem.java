package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;

public record PosShiftListItem(
    PosShift shift,
    String cashierDisplayName,
    String deviceName,
    String deviceVisibleCode) {}
