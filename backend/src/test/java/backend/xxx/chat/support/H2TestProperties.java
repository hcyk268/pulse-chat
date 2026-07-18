package backend.xxx.chat.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.TestPropertySource;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat_unit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.format_sql=false",
        "spring.flyway.enabled=false",
        "jwt.access-token-secret=test-only-access-token-secret-at-least-32-characters",
        "jwt.refresh-token-secret=test-only-refresh-token-secret-at-least-32-characters",
        "jwt.refresh-token-hash-secret=test-only-refresh-token-secret-at-least-32-characters",
        "jwt.access-token-expiration-ms=900000",
        "jwt.refresh-token-expiration-ms=604800000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.outbox.worker.enabled=false"
})
public @interface H2TestProperties {
}