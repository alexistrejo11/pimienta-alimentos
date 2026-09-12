package io.github.alexistrejo11.pimienta.module.headquarter.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertPosSettingsCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;

public interface HeadquarterPosSettingsUseCases {

  PosOperationalConfig get(long headquarterId);

  PosOperationalConfig upsert(long headquarterId, UpsertPosSettingsCommand command);
}
