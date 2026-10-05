package com.cbarkinozer.onlinebankingrestapi.app.loa.service;

import com.cbarkinozer.onlinebankingrestapi.app.loa.config.LoaLoanProperties;
import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoan;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoanPayment;
import com.cbarkinozer.onlinebankingrestapi.app.loa.enums.LoaLoanStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanPaymentEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoaLoanServiceTest {

    private static final Long LOAN_ID = 1L;
    private static final Long CUSTOMER_ID = 5L;

    @Mock
    private LoaLoanValidationService loaLoanValidationService;

    @Mock
    private LoaLoanEntityService loaLoanEntityService;

    @Mock
    private LoaLoanPaymentEntityService loaLoanPaymentEntityService;

    @Spy
    private LoaLoanProperties loaLoanProperties = new LoaLoanProperties();

    @InjectMocks
    private LoaLoanService loaLoanService;

    @Test
    void shouldCalculateLoan() {

        LoaCalculateLoanResponseDto result = loaLoanService.calculateLoan(24, BigDecimal.valueOf(3000));

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(45), result.getAllocationFee());
        assertTrue(result.getTotalPayment().compareTo(BigDecimal.valueOf(3000)) > 0);
        assertTrue(result.getMonthlyInstallmentAmount().compareTo(BigDecimal.ZERO) > 0);
        verify(loaLoanValidationService).controlIsParameterNotNull(24, BigDecimal.valueOf(3000));
    }

    @Test
    void shouldApplyDefaultIvaOnLoanInterest() {

        LoaCalculateLoanResponseDto result = loaLoanService.calculateLoan(24, BigDecimal.valueOf(3000));

        // interest = 3000 * 0.0159 * 1 * 24 = 1144.8, IVA = 16% of interest
        assertEquals(0, new BigDecimal("0.16").compareTo(result.getIvaRate()));
        assertEquals(0, new BigDecimal("183.168").compareTo(result.getIvaAmount()));
        assertEquals(0, new BigDecimal("1327.968").compareTo(result.getTotalInterest()));
        assertEquals(0, new BigDecimal("4372.968").compareTo(result.getTotalPayment()));
        verify(loaLoanValidationService).controlIsTaxRateNotNegative(new BigDecimal("0.16"));
    }

    @Test
    void shouldUseConfiguredIvaRate() {

        loaLoanProperties.setIvaRate(new BigDecimal("0.08"));

        LoaCalculateLoanResponseDto result = loaLoanService.calculateLoan(24, BigDecimal.valueOf(3000));

        assertEquals(0, new BigDecimal("0.08").compareTo(result.getIvaRate()));
        assertEquals(0, new BigDecimal("91.584").compareTo(result.getIvaAmount()));
        assertEquals(0, new BigDecimal("1236.384").compareTo(result.getTotalInterest()));
    }

    @Test
    void shouldCalculateLateFee() {

        LoaLoan loaLoan = createLoan(LocalDate.now().minusDays(10));

        when(loaLoanEntityService.getByIdWithControl(LOAN_ID)).thenReturn(loaLoan);
        when(loaLoanValidationService.controlIsLoanDueDatePast(loaLoan.getDueDate())).thenReturn(10L);

        LoaCalculateLateFeeResponseDto result = loaLoanService.calculateLateFee(LOAN_ID);

        assertEquals(10L, result.getLateDayCount());
        BigDecimal lateFeeBeforeIva = result.getTotalLateFee().subtract(result.getLateInterestTax());
        assertTrue(result.getLateInterestTax().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, lateFeeBeforeIva.multiply(new BigDecimal("0.16")).compareTo(result.getLateInterestTax()));
        assertTrue(result.getTotalLateFee().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(LoaLoanStatusType.LATE, loaLoan.getLoanStatusType());
        assertEquals(0, BigDecimal.valueOf(1000).add(result.getTotalLateFee()).compareTo(loaLoan.getRemainingPrincipal()));
        verify(loaLoanEntityService).save(loaLoan);
    }

    @Test
    void shouldFindLoanById() {

        when(loaLoanEntityService.getByIdWithControl(LOAN_ID)).thenReturn(createLoan(LocalDate.now().plusMonths(1)));

        LoaLoanDto loaLoanDto = loaLoanService.findLoanById(LOAN_ID);

        assertEquals(LOAN_ID, loaLoanDto.getId());
        assertEquals(LoaLoanStatusType.CONTINUING, loaLoanDto.getLoanStatusType());
    }

    @Test
    void shouldApplyLoan() {

        LoaApplyLoanDto applyLoanDto = new LoaApplyLoanDto();
        applyLoanDto.setInstallmentCount(24);
        applyLoanDto.setPrincipalLoanAmount(BigDecimal.valueOf(3000));
        applyLoanDto.setMonthlySalary(BigDecimal.valueOf(9000));

        when(loaLoanEntityService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        when(loaLoanEntityService.save(any(LoaLoan.class))).thenAnswer(inv -> {
            LoaLoan saved = inv.getArgument(0);
            saved.setId(LOAN_ID);
            return saved;
        });

        LoaLoanDto result = loaLoanService.applyLoan(applyLoanDto);

        assertEquals(LOAN_ID, result.getId());
        assertEquals(CUSTOMER_ID, result.getCustomerId());
        assertEquals(24, result.getInstallmentCount());
        assertEquals(BigDecimal.valueOf(3000), result.getRemainingPrincipal());
        assertEquals(0, new BigDecimal("1327.968").compareTo(result.getInterestToBePaid()));
        assertEquals(LocalDate.now().plusMonths(24), result.getDueDate());
        assertEquals(LoaLoanStatusType.CONTINUING, result.getLoanStatusType());
    }

    @Test
    void shouldPayInstallment() {

        LoaLoan loaLoan = createLoan(LocalDate.now().plusMonths(1));

        when(loaLoanEntityService.getByIdWithControl(LOAN_ID)).thenReturn(loaLoan);
        when(loaLoanEntityService.save(loaLoan)).thenReturn(loaLoan);
        when(loaLoanPaymentEntityService.save(any(LoaLoanPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        LoaPayInstallmentResponseDto result = loaLoanService.payInstallment(LOAN_ID);

        assertEquals(LOAN_ID, result.getLoanId());
        assertEquals(BigDecimal.valueOf(100), result.getPaymentAmount());
        assertEquals(BigDecimal.valueOf(900), result.getRemainingPrincipal());
        assertEquals(LocalDate.now(), result.getPaymentDate());
    }

    @Test
    void shouldPayLoanOff() {

        LoaLoan loaLoan = createLoan(LocalDate.now().plusMonths(1));

        when(loaLoanEntityService.getByIdWithControl(LOAN_ID)).thenReturn(loaLoan);
        when(loaLoanEntityService.save(loaLoan)).thenReturn(loaLoan);

        LoaPayLoanOffResponseDto result = loaLoanService.payLoanOff(LOAN_ID);

        assertEquals(LOAN_ID, result.getId());
        assertEquals(BigDecimal.valueOf(1000), result.getPaidAmount());
        assertEquals(BigDecimal.ZERO, result.getRemainingAmount());
        assertEquals(LoaLoanStatusType.PAID, result.getLoanStatusType());
        verify(loaLoanValidationService).controlIsLoanNotAlreadyPaidOff(loaLoan);
    }

    private LoaLoan createLoan(LocalDate dueDate) {
        LoaLoan loaLoan = new LoaLoan();
        loaLoan.setId(LOAN_ID);
        loaLoan.setCustomerId(CUSTOMER_ID);
        loaLoan.setInstallmentCount(10);
        loaLoan.setPrincipalLoanAmount(BigDecimal.valueOf(1000));
        loaLoan.setMonthlyInstallmentAmount(BigDecimal.valueOf(100));
        loaLoan.setInterestToBePaid(BigDecimal.valueOf(50));
        loaLoan.setPrincipalToBePaid(BigDecimal.valueOf(1000));
        loaLoan.setRemainingPrincipal(BigDecimal.valueOf(1000));
        loaLoan.setDueDate(dueDate);
        loaLoan.setLoanStatusType(LoaLoanStatusType.CONTINUING);
        return loaLoan;
    }
}
