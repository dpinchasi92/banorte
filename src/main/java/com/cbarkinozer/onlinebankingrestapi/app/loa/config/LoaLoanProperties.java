package com.cbarkinozer.onlinebankingrestapi.app.loa.config;

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
@ConfigurationProperties(prefix = "onlinebankingrestapi.loan")
public class LoaLoanProperties {

    public static final BigDecimal DEFAULT_IVA_RATE = new BigDecimal("0.16");

    /** IVA applied to loan interest and late-fee interest, as a fraction (0.16 = 16%). */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal ivaRate = DEFAULT_IVA_RATE;
}
