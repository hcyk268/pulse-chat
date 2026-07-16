package backend.xxx.chat.common.web;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Translator {

    private static ResourceBundleMessageSource messageSource;

    public Translator(ResourceBundleMessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    public static String toLocale(String msgCode) {
        return toLocale(msgCode, (Object[]) null);
    }

    public static String toLocale(String msgCode, Object... args) {
        if (msgCode == null) {
            return null;
        }

        if (messageSource == null) {
            return msgCode;
        }

        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(msgCode, args, msgCode, locale);
    }
}
