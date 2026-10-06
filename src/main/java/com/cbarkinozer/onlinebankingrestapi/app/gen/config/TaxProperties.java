package com.cbarkinozer.onlinebankingrestapi.app.gen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "banorte.tax")
public class TaxProperties {

    public static final BigDecimal IVA_RATE = new BigDecimal("0.16");

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal ivaRate = IVA_RATE;
}
