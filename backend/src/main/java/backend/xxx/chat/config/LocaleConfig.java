package backend.xxx.chat.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig extends AcceptHeaderLocaleResolver {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, new Locale("vi"));

    public LocaleConfig() {
        setDefaultLocale(DEFAULT_LOCALE);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String languageHeader = request.getHeader("Accept-Language");

        if (!StringUtils.hasText(languageHeader)) {
            return DEFAULT_LOCALE;
        }

        try {
            Locale locale = Locale.lookup(Locale.LanguageRange.parse(languageHeader), SUPPORTED_LOCALES);
            return locale != null ? locale : DEFAULT_LOCALE;
        } catch (IllegalArgumentException ex) {
            return DEFAULT_LOCALE;
        }
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource rs = new ResourceBundleMessageSource();
        rs.setBasename("messages");
        rs.setDefaultEncoding("UTF-8");
        rs.setUseCodeAsDefaultMessage(true);
        rs.setCacheSeconds(3600);
        return rs;
    }
}
