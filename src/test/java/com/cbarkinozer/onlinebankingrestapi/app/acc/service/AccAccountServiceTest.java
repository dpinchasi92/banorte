package com.cbarkinozer.onlinebankingrestapi.app.acc.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccAccountType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccCurrencyType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.cus.enums.CusErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.IllegalFieldException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.util.ClabeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccAccountServiceTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long CUSTOMER_ID = 5L;

    @Mock
    private AccAccountEntityService accAccountEntityService;

    @Mock
    private AccAccountValidationService accAccountValidationService;

    @InjectMocks
    private AccAccountService accAccountService;

    @Test
    void shouldFindAllAccounts() {

        List<AccAccount> accAccountList = new ArrayList<>();
        accAccountList.add(createAccount());

        when(accAccountEntityService.findAllActiveAccounts()).thenReturn(accAccountList);

        List<AccAccountDto> result = accAccountService.findAllAccounts();

        assertEquals(1, result.size());
        assertEquals(ACCOUNT_ID, result.get(0).getId());
    }

    @Test
    void shouldFindAllAccounts_WhenAccountList_IsEmpty() {

        when(accAccountEntityService.findAllActiveAccounts()).thenReturn(new ArrayList<>());

        List<AccAccountDto> result = accAccountService.findAllAccounts();

        assertEquals(0, result.size());
    }

    @Test
    void shouldFindAccountById() {

        when(accAccountEntityService.getByIdWithControl(ACCOUNT_ID)).thenReturn(createAccount());

        AccAccountDto accAccountDto = accAccountService.findAccountById(ACCOUNT_ID);

        assertEquals(ACCOUNT_ID, accAccountDto.getId());
    }

    @Test
    void shouldNotFindAccountById_WhenId_DoesNotExist(){

        ItemNotFoundException expected = new ItemNotFoundException(GenErrorMessage.ITEM_NOT_FOUND);
        when(accAccountEntityService.getByIdWithControl(ACCOUNT_ID)).thenThrow(expected);

        ItemNotFoundException result = assertThrows(ItemNotFoundException.class,
                () -> accAccountService.findAccountById(ACCOUNT_ID));

        assertSame(expected, result);
        verify(accAccountEntityService).getByIdWithControl(ACCOUNT_ID);
    }

    @Test
    void shouldFindAccountByCustomerId() {

        List<AccAccount> accAccountList = new ArrayList<>();
        accAccountList.add(createAccount());

        when(accAccountEntityService.findAccountByCustomerId(CUSTOMER_ID)).thenReturn(accAccountList);

        List<AccAccountDto> result = accAccountService.findAccountByCustomerId(CUSTOMER_ID);

        assertEquals(1, result.size());
        assertEquals(CUSTOMER_ID, result.get(0).getCustomerId());
    }

    @Test
    void shouldSaveAccount() {

        AccAccountSaveDto saveDto = createSaveDto();

        when(accAccountEntityService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        when(accAccountEntityService.save(any(AccAccount.class))).thenAnswer(inv -> {
            AccAccount saved = inv.getArgument(0);
            saved.setId(ACCOUNT_ID);
            return saved;
        });

        AccAccountDto result = accAccountService.saveAccount(saveDto);

        assertEquals(ACCOUNT_ID, result.getId());
        assertEquals(CUSTOMER_ID, result.getCustomerId());
        assertEquals(GenStatusType.ACTIVE, result.getStatusType());
        assertEquals(BigDecimal.valueOf(100), result.getCurrentBalance());
        assertEquals(18, result.getClabe().length());
        assertTrue(result.getClabe().startsWith("072"));
        assertTrue(ClabeUtil.isValidClabe(result.getClabe()));
        verify(accAccountValidationService).controlIsClabeValid(result.getClabe());
        verify(accAccountValidationService).controlIsCustomerExist(CUSTOMER_ID);
    }

    @Test
    void shouldNotSaveAccount_WhenCustomer_DoesNotExist(){

        IllegalFieldException expected = new IllegalFieldException(CusErrorMessage.CUSTOMER_NOT_FOUND);

        when(accAccountEntityService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        doThrow(expected).when(accAccountValidationService).controlIsCustomerExist(CUSTOMER_ID);

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountService.saveAccount(createSaveDto()));

        assertSame(expected, result);
        verify(accAccountEntityService, never()).save(any());
    }

    @Test
    void shouldNotSaveAccount_WhenFields_AreNull(){

        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.FIELD_CANNOT_BE_NULL);

        when(accAccountEntityService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        doThrow(expected).when(accAccountValidationService).controlAreFieldsNotNull(any(AccAccount.class));

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountService.saveAccount(new AccAccountSaveDto()));

        assertSame(expected, result);
        verify(accAccountEntityService, never()).save(any());
    }

    @Test
    void shouldNotSaveAccount_WhenBalance_IsNegative(){

        AccAccountSaveDto saveDto = createSaveDto();
        saveDto.setCurrentBalance(BigDecimal.valueOf(-1));
        IllegalFieldException expected = new IllegalFieldException(AccErrorMessage.BALANCE_CANNOT_BE_NEGATIVE);

        when(accAccountEntityService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        doThrow(expected).when(accAccountValidationService).controlIsBalanceNotNegative(any(AccAccount.class));

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> accAccountService.saveAccount(saveDto));

        assertSame(expected, result);
        verify(accAccountEntityService, never()).save(any());
    }

    @Test
    void shouldCancelAccount() {

        AccAccount accAccount = createAccount();

        when(accAccountEntityService.getByIdWithControl(ACCOUNT_ID)).thenReturn(accAccount);

        accAccountService.cancelAccount(ACCOUNT_ID);

        assertEquals(GenStatusType.PASSIVE, accAccount.getStatusType());
        verify(accAccountEntityService).save(accAccount);
    }

    @Test
    void shouldNotCancelAccount_WhenId_DoesNotExist(){

        when(accAccountEntityService.getByIdWithControl(ACCOUNT_ID))
                .thenThrow(new ItemNotFoundException(GenErrorMessage.ITEM_NOT_FOUND));

        assertThrows(ItemNotFoundException.class, () -> accAccountService.cancelAccount(ACCOUNT_ID));

        verify(accAccountEntityService, never()).save(any());
    }

    private AccAccount createAccount() {
        AccAccount accAccount = new AccAccount();
        accAccount.setId(ACCOUNT_ID);
        accAccount.setCustomerId(CUSTOMER_ID);
        accAccount.setClabe("072180000118359711");
        accAccount.setCurrentBalance(BigDecimal.valueOf(100));
        accAccount.setCurrencyType(AccCurrencyType.TL);
        accAccount.setAccountType(AccAccountType.DEPOSIT);
        accAccount.setStatusType(GenStatusType.ACTIVE);
        return accAccount;
    }

    private AccAccountSaveDto createSaveDto() {
        AccAccountSaveDto saveDto = new AccAccountSaveDto();
        saveDto.setCurrentBalance(BigDecimal.valueOf(100));
        saveDto.setCurrencyType(AccCurrencyType.TL);
        saveDto.setAccountType(AccAccountType.DEPOSIT);
        return saveDto;
    }
}
