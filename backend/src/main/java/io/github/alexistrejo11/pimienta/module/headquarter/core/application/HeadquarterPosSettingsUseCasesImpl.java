package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertPosSettingsCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterPosSettingsNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosSettingsUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeadquarterPosSettingsUseCasesImpl implements HeadquarterPosSettingsUseCases {

  private final HeadquarterRepository headquarterRepository;
  private final PosOperationalConfigRepository posOperationalConfigRepository;
  private final PosLocationUseCases posLocationUseCases;

  public HeadquarterPosSettingsUseCasesImpl(
      HeadquarterRepository headquarterRepository,
      PosOperationalConfigRepository posOperationalConfigRepository,
      PosLocationUseCases posLocationUseCases) {
    this.headquarterRepository = headquarterRepository;
    this.posOperationalConfigRepository = posOperationalConfigRepository;
    this.posLocationUseCases = posLocationUseCases;
  }

  @Override
  public PosOperationalConfig get(long headquarterId) {
    assertHeadquarterExists(headquarterId);
    return posOperationalConfigRepository
        .findByHeadquarterId(headquarterId)
        .orElseThrow(() -> new HeadquarterPosSettingsNotFoundException(headquarterId));
  }

  @Override
  @Transactional
  public PosOperationalConfig upsert(long headquarterId, UpsertPosSettingsCommand command) {
    assertHeadquarterExists(headquarterId);

    PosOperationalConfig saved =
        posOperationalConfigRepository
            .findByHeadquarterId(headquarterId)
            .map(
                existing ->
                    posOperationalConfigRepository.save(
                        PosOperationalConfig.builder()
                            .withCurrency(resolveCurrency(command.currency(), existing.getCurrency()))
                            .withCatalogStaleWarnHours(
                                resolveInt(
                                    command.catalogStaleWarnHours(),
                                    existing.getCatalogStaleWarnHours()))
                            .withCatalogStaleBlockHours(
                                resolveInt(
                                    command.catalogStaleBlockHours(),
                                    existing.getCatalogStaleBlockHours()))
                            .withOpenAmountCategories(
                                resolveCategories(
                                    command.openAmountCategories(),
                                    existing.getOpenAmountCategories()))
                            .withDefaultNegativeStockLimit(
                                command.defaultNegativeStockLimit() != null
                                    ? command.defaultNegativeStockLimit()
                                    : existing.getDefaultNegativeStockLimit())
                            .revise(existing)))
            .orElseGet(
                () ->
                    posOperationalConfigRepository.save(
                        PosOperationalConfig.builder()
                            .withHeadquarterId(headquarterId)
                            .withCurrency(resolveCurrency(command.currency(), "MXN"))
                            .withCatalogStaleWarnHours(
                                resolveInt(command.catalogStaleWarnHours(), 24))
                            .withCatalogStaleBlockHours(
                                resolveInt(command.catalogStaleBlockHours(), 72))
                            .withOpenAmountCategories(
                                resolveCategories(command.openAmountCategories(), List.of()))
                            .withDefaultNegativeStockLimit(command.defaultNegativeStockLimit())
                            .register()));

    posLocationUseCases.ensurePosLocation(headquarterId);
    return saved;
  }

  private void assertHeadquarterExists(long headquarterId) {
    headquarterRepository
        .findById(headquarterId)
        .orElseThrow(() -> new HeadquarterNotFoundException(headquarterId));
  }

  private static String resolveCurrency(String incoming, String fallback) {
    if (incoming == null || incoming.isBlank()) {
      return fallback;
    }
    return incoming.strip();
  }

  private static int resolveInt(Integer incoming, int fallback) {
    return incoming != null ? incoming : fallback;
  }

  private static List<String> resolveCategories(List<String> incoming, List<String> fallback) {
    return incoming != null ? incoming : fallback;
  }
}
