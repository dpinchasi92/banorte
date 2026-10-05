package com.cbarkinozer.onlinebankingrestapi.app.acc.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountActivityDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyActivityRequestDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccountActivity;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccAccountActivityType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountActivityEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.GenBusinessException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.IllegalFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccAccountActivityServiceTest {

    private static final Long ACCOUNT_ID = 1L;

    @Mock
    private AccAccountEntityService accAccountEntityService;

    @Mock
    private AccAccountActivityEntityService accAccountActivityEntityService;

    @Mock
    private AccAccountValidationService accAccountValidationService;

    @InjectMocks
    private AccAccountActivityService accAccountActivityService;

    @Test
    void shouldWithdraw() {

        AccMoneyActivityRequestDto request = createRequest(ACCOUNT_ID, BigDecimal.valueOf(100));
        AccAccount accAccount = createAccount(ACCOUNT_ID, BigDecimal.valueOf(200));

        when(accAccountEntityService.getByIdWithControl(ACCOUNT_ID)).thenReturn(accAccount);
        when(accAccountActivityEntityService.save(any(AccAccountActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        AccAccountActivityDto result = accAccountActivityService.withdraw(request);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals(BigDecimal.valueOf(100), result.getCurrentBalance());
        assertEquals(AccAccountActivityType.WITHDRAW, result.getAccountActivityType());
        assertEquals(BigDecimal.valueOf(100), accAccount.getCurrentBalance());
        verify(accAccountEntityService).save(accAccount);
    }

    @Test
    void shouldNotWithdraw_WhenMoneyActivityRequestDto_IsNull(){

        GenBusinessException expected = new GenBusinessException(GenErrorMessage.PARAMETER_CANNOT_BE_NULL);

        doThrow(expected).when(accAccountValidationService).controlIsMoneyActivityRequestDtoNotNull(null);

        GenBusinessException result = assertThrows(GenBusinessException.class,
                () -> accAccountActivityService.withdraw(null));

        assertSame(expected, result);
        verifyNoInteractions(accAccountEntityService, accAccountActivityEntityService);
    }

    @Test
    void shouldNotWithdraw_WhenAccountId_DoesNotExist(){

        AccMoneyActivityRequestDto request = createRequest(ACCOUNT_ID, BigDecimal.valueOf(100));
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.ACCOUNT_NOT_FOUND);

        doThrow(expected).when(accAccountValidationService).controlIsAccountIdExist(ACCOUNT_ID);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountActivityService.withdraw(request));

        assertSame(expected, result);
        verifyNoInteractions(accAccountEntityService, accAccountActivityEntityService);
    }

    @Test
    void shouldNotWithdraw_WhenAmount_IsNotPositive(){

        AccMoneyActivityRequestDto request = createRequest(ACCOUNT_ID, BigDecimal.ZERO);
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.AMOUNT_MUST_BE_POSITIVE);

        doThrow(expected).when(accAccountValidationService).controlIsAmountPositive(BigDecimal.ZERO);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountActivityService.withdraw(request));

        assertSame(expected, result);
        verifyNoInteractions(accAccountEntityService, accAccountActivityEntityService);
    }

    @Test
    void shouldDeposit() {

        AccMoneyActivityRequestDto request = createRequest(ACCOUNT_ID, BigDecimal.valueOf(100));
        AccAccount accAccount = createAccount(ACCOUNT_ID, BigDecimal.valueOf(200));

        when(accAccountEntityService.getByIdWithControl(ACCOUNT_ID)).thenReturn(accAccount);
        when(accAccountActivityEntityService.save(any(AccAccountActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        AccAccountActivityDto result = accAccountActivityService.deposit(request);

        assertEquals(ACCOUNT_ID, result.getAccountId());
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals(BigDecimal.valueOf(300), result.getCurrentBalance());
        assertEquals(AccAccountActivityType.DEPOSIT, result.getAccountActivityType());
        assertEquals(BigDecimal.valueOf(300), accAccount.getCurrentBalance());
        verify(accAccountEntityService).save(accAccount);
    }

    @Test
    void shouldNotDeposit_WhenMoneyActivityRequestDto_IsNull(){

        GenBusinessException expected = new GenBusinessException(GenErrorMessage.PARAMETER_CANNOT_BE_NULL);

        doThrow(expected).when(accAccountValidationService).controlIsMoneyActivityRequestDtoNotNull(null);

        GenBusinessException result = assertThrows(GenBusinessException.class,
                () -> accAccountActivityService.deposit(null));

        assertSame(expected, result);
        verifyNoInteractions(accAccountEntityService, accAccountActivityEntityService);
    }

    @Test
    void shouldNotDeposit_WhenAccountId_DoesNotExist(){

        AccMoneyActivityRequestDto request = createRequest(ACCOUNT_ID, BigDecimal.valueOf(100));
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.ACCOUNT_NOT_FOUND);

        doThrow(expected).when(accAccountValidationService).controlIsAccountIdExist(ACCOUNT_ID);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountActivityService.deposit(request));

        assertSame(expected, result);
        verifyNoInteractions(accAccountEntityService, accAccountActivityEntityService);
    }

    @Test
    void shouldNotDeposit_WhenAmount_IsNotPositive(){

        AccMoneyActivityRequestDto request = createRequest(ACCOUNT_ID, BigDecimal.ZERO);
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.AMOUNT_MUST_BE_POSITIVE);

        doThrow(expected).when(accAccountValidationService).controlIsAmountPositive(BigDecimal.ZERO);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountActivityService.deposit(request));

        assertSame(expected, result);
        verifyNoInteractions(accAccountEntityService, accAccountActivityEntityService);
    }

    private AccMoneyActivityRequestDto createRequest(Long accountId, BigDecimal amount) {
        AccMoneyActivityRequestDto request = new AccMoneyActivityRequestDto();
        request.setAccountId(accountId);
        request.setAmount(amount);
        return request;
    }

    private AccAccount createAccount(Long id, BigDecimal currentBalance) {
        AccAccount accAccount = new AccAccount();
        accAccount.setId(id);
        accAccount.setCurrentBalance(currentBalance);
        return accAccount;
    }
}
