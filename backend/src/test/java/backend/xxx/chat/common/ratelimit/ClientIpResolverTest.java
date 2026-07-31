package backend.xxx.chat.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void usesRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.25");
        request.addHeader("X-Real-IP", "198.51.100.26");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void normalizesIpAddress() {
        MockHttpServletRequest mappedIpv4 = new MockHttpServletRequest();
        mappedIpv4.setRemoteAddr("::FFFF:192.0.2.44");

        MockHttpServletRequest zonedIpv6 = new MockHttpServletRequest();
        zonedIpv6.setRemoteAddr("FE80::1%ETH0");

        assertThat(resolver.resolve(mappedIpv4)).isEqualTo("192.0.2.44");
        assertThat(resolver.resolve(zonedIpv6)).isEqualTo("fe80::1");
    }

    @Test
    void usesFallbackAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(" ");

        assertThat(resolver.resolve(request)).isEqualTo(ClientIpResolver.UNKNOWN_IP);
        assertThat(resolver.resolve(null)).isEqualTo(ClientIpResolver.UNKNOWN_IP);
    }
}
