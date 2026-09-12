package io.github.alexistrejo11.pimienta.config.web;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@Configuration
public class MessageSourceConfig {

  public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es");

  @Bean
  MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:i18n/messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setDefaultLocale(DEFAULT_LOCALE);
    return source;
  }

  @Bean
  LocaleResolver localeResolver() {
    return new FixedLocaleResolver(DEFAULT_LOCALE);
  }
}
