package io.github.alexistrejo11.pimienta.module.headquarter.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HeadquarterPosCatalogUseCases {

  Page<HeadquarterItem> list(long headquarterId, Pageable pageable);

  HeadquarterItem get(long headquarterId, long itemId);

  HeadquarterItem upsert(long headquarterId, long itemId, UpsertHeadquarterItemCommand command);
}
