package com.cbarkinozer.onlinebankingrestapi.app.gen.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldDefaultIvaRateTo16Percent() {
        contextRunner.run(context ->
                assertThat(context.getBean(TaxProperties.class).getIvaRate()).isEqualByComparingTo("0.16"));
    }

    @Test
    void shouldBindConfiguredIvaRate() {
        contextRunner.withPropertyValues("banorte.tax.iva-rate=0.08").run(context ->
                assertThat(context.getBean(TaxProperties.class).getIvaRate()).isEqualByComparingTo(new BigDecimal("0.08")));
    }

    @Test
    void shouldRejectNegativeIvaRate() {
        contextRunner.withPropertyValues("banorte.tax.iva-rate=-0.01").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectIvaRateAboveOne() {
        contextRunner.withPropertyValues("banorte.tax.iva-rate=16").run(context ->
                assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(TaxProperties.class)
    static class TestConfig {
    }
}
