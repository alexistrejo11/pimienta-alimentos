package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertPosSettingsCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterPosSettingsNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosSettingsUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosChangeLogService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeadquarterPosSettingsUseCasesImpl implements HeadquarterPosSettingsUseCases {

  private final HeadquarterRepository headquarterRepository;
  private final PosOperationalConfigRepository posOperationalConfigRepository;
  private final PosLocationUseCases posLocationUseCases;
  private final PosChangeLogService posChangeLogService;

  public HeadquarterPosSettingsUseCasesImpl(
      HeadquarterRepository headquarterRepository,
      PosOperationalConfigRepository posOperationalConfigRepository,
      PosLocationUseCases posLocationUseCases,
      PosChangeLogService posChangeLogService) {
    this.headquarterRepository = headquarterRepository;
    this.posOperationalConfigRepository = posOperationalConfigRepository;
    this.posLocationUseCases = posLocationUseCases;
    this.posChangeLogService = posChangeLogService;
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
                            .withAllowOpenProducts(
                                resolveBoolean(command.allowOpenProducts(), existing.isAllowOpenProducts()))
                            .withOpenAmountCategories(
                                resolveCategories(
                                    command.openAmountCategories(),
                                    existing.getOpenAmountCategories(),
                                    resolveBoolean(
                                        command.allowOpenProducts(), existing.isAllowOpenProducts())))
                            .withDefaultNegativeStockLimit(
                                command.defaultNegativeStockLimit() != null
                                    ? command.defaultNegativeStockLimit()
                                    : existing.getDefaultNegativeStockLimit())
                            .withStockless(
                                resolveBoolean(command.stockless(), existing.isStockless()))
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
                            .withAllowOpenProducts(resolveBoolean(command.allowOpenProducts(), false))
                            .withOpenAmountCategories(
                                resolveCategories(
                                    command.openAmountCategories(),
                                    List.of(),
                                    resolveBoolean(command.allowOpenProducts(), false)))
                            .withDefaultNegativeStockLimit(command.defaultNegativeStockLimit())
                            .withStockless(resolveBoolean(command.stockless(), false))
                            .register()));

    posLocationUseCases.ensurePosLocation(headquarterId);
    Map<String, Object> policyProjection = new LinkedHashMap<>();
    policyProjection.put("allowOpenProducts", saved.isAllowOpenProducts());
    policyProjection.put("openAmountCategories", saved.getOpenAmountCategories());
    policyProjection.put("catalogStaleWarnHours", saved.getCatalogStaleWarnHours());
    policyProjection.put("catalogStaleBlockHours", saved.getCatalogStaleBlockHours());
    policyProjection.put("defaultNegativeStockLimit", saved.getDefaultNegativeStockLimit());
    policyProjection.put("stockless", saved.isStockless());
    posChangeLogService.appendPolicy(headquarterId, policyProjection);
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

  private static List<String> resolveCategories(
      List<String> incoming, List<String> fallback, boolean allowOpenProducts) {
    List<String> source = incoming != null ? incoming : fallback;
    Map<String, String> normalized = new LinkedHashMap<>();
    for (String category : source) {
      if (category == null || category.isBlank()) {
        continue;
      }
      String value = category.strip();
      normalized.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
    }
    List<String> result = List.copyOf(normalized.values());
    if (allowOpenProducts && result.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one open product category is required when open products are enabled");
    }
    return result;
  }

  private static boolean resolveBoolean(Boolean incoming, boolean fallback) {
    return incoming != null ? incoming : fallback;
  }
}
