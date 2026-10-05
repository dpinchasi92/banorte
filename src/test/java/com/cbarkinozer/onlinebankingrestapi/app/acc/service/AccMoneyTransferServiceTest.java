package com.cbarkinozer.onlinebankingrestapi.app.acc.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyActivityDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyTransferDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyTransferSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccMoneyTransfer;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccAccountActivityType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccMoneyTransferType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccMoneyTransferEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.GenBusinessException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.IllegalFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccMoneyTransferServiceTest {

    @Mock
    private AccMoneyTransferEntityService accMoneyTransferEntityService;

    @Mock
    private AccAccountActivityService accAccountActivityService;

    @Mock
    private AccAccountValidationService accAccountValidationService;

    @InjectMocks
    private AccMoneyTransferService accMoneyTransferService;

    @Test
    void transferMoney() {

        AccMoneyTransferSaveDto saveDto = createSaveDto(1L, 2L, BigDecimal.valueOf(100));

        when(accMoneyTransferEntityService.save(any(AccMoneyTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        AccMoneyTransferDto result = accMoneyTransferService.transferMoney(saveDto);

        assertEquals(1L, result.getAccountIdFrom());
        assertEquals(2L, result.getAccountIdTo());
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals(LocalDate.now(), result.getTransferDate());
        assertEquals(AccMoneyTransferType.DUE, result.getTransferType());

        ArgumentCaptor<AccMoneyActivityDto> outCaptor = ArgumentCaptor.forClass(AccMoneyActivityDto.class);
        ArgumentCaptor<AccMoneyActivityDto> inCaptor = ArgumentCaptor.forClass(AccMoneyActivityDto.class);
        verify(accAccountActivityService).moneyOut(outCaptor.capture());
        verify(accAccountActivityService).moneyIn(inCaptor.capture());
        verify(accMoneyTransferEntityService).save(any(AccMoneyTransfer.class));

        assertEquals(1L, outCaptor.getValue().getAccountId());
        assertEquals(AccAccountActivityType.SEND, outCaptor.getValue().getActivityType());
        assertEquals(2L, inCaptor.getValue().getAccountId());
        assertEquals(AccAccountActivityType.GET, inCaptor.getValue().getActivityType());
    }

    @Test
    void shouldNotTransferMoney_WhenMoneyTransferSaveDto_IsNull(){

        GenBusinessException expected = new GenBusinessException(GenErrorMessage.PARAMETER_CANNOT_BE_NULL);

        doThrow(expected).when(accAccountValidationService).controlIsMoneyTransferSaveDtoIsNull(null);

        GenBusinessException result = assertThrows(GenBusinessException.class,
                () -> accMoneyTransferService.transferMoney(null));

        assertSame(expected, result);
        verifyNoInteractions(accAccountActivityService, accMoneyTransferEntityService);
    }

    @Test
    void shouldNotTransferMoney_WhenAccountId_DoesNotExist(){

        AccMoneyTransferSaveDto saveDto = createSaveDto(0L, 2L, BigDecimal.valueOf(100));
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.ACCOUNT_NOT_FOUND);

        doThrow(expected).when(accAccountValidationService).controlIsAccountIdExist(0L);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accMoneyTransferService.transferMoney(saveDto));

        assertSame(expected, result);
        verifyNoInteractions(accAccountActivityService, accMoneyTransferEntityService);
    }

    @Test
    void shouldNotTransferMoney_WhenAmount_IsNotPositive(){

        AccMoneyTransferSaveDto saveDto = createSaveDto(1L, 2L, BigDecimal.ZERO);
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.AMOUNT_MUST_BE_POSITIVE);

        doThrow(expected).when(accAccountValidationService).controlIsAmountPositive(BigDecimal.ZERO);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accMoneyTransferService.transferMoney(saveDto));

        assertSame(expected, result);
        verifyNoInteractions(accAccountActivityService, accMoneyTransferEntityService);
    }

    private AccMoneyTransferSaveDto createSaveDto(Long from, Long to, BigDecimal amount) {
        AccMoneyTransferSaveDto saveDto = new AccMoneyTransferSaveDto();
        saveDto.setAccountIdFrom(from);
        saveDto.setAccountIdTo(to);
        saveDto.setAmount(amount);
        saveDto.setDescription("test transfer");
        saveDto.setTransferType(AccMoneyTransferType.DUE);
        return saveDto;
    }
}
