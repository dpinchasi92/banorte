package com.cbarkinozer.onlinebankingrestapi.app.cus.service;

import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerUpdateDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.cus.enums.CusErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.cus.service.entityservice.CusCustomerEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.IllegalFieldException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CusCustomerServiceTest {

    private static final Long CUSTOMER_ID = 1L;

    @Mock
    private CusCustomerEntityService cusCustomerEntityService;

    @Mock
    private CusCustomerValidationService cusCustomerValidationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CusCustomerService cusCustomerService;

    @Test
    void shouldFindAllCustomers() {

        List<CusCustomer> cusCustomerList = new ArrayList<>();
        cusCustomerList.add(createDummyCusCustomer());

        List<CusCustomerDto> expectedResult = new ArrayList<>();
        expectedResult.add(createDummyCusCustomerDto());

        when(cusCustomerEntityService.findAllCustomers()).thenReturn(cusCustomerList);

        List<CusCustomerDto> result = cusCustomerService.findAllCustomers();

        assertEquals(expectedResult, result);
    }

    @Test
    void shouldFindCustomerById() {

        when(cusCustomerEntityService.getByIdWithControl(CUSTOMER_ID)).thenReturn(createDummyCusCustomer());

        CusCustomerDto result = cusCustomerService.findCustomerById(CUSTOMER_ID);

        assertEquals(createDummyCusCustomerDto(), result);
    }

    @Test
    void shouldNotFindCustomerById_WhenCusCustomerId_DoesNotExist() {

        ItemNotFoundException expected = new ItemNotFoundException(GenErrorMessage.ITEM_NOT_FOUND);

        when(cusCustomerEntityService.getByIdWithControl(CUSTOMER_ID)).thenThrow(expected);

        ItemNotFoundException result = assertThrows(ItemNotFoundException.class,
                () -> cusCustomerService.findCustomerById(CUSTOMER_ID));

        assertSame(expected, result);
    }

    @Test
    void shouldSaveCustomer() {

        CusCustomerSaveDto cusCustomerSaveDto = createDummyCusCustomerSaveDto();

        when(passwordEncoder.encode("test1234")).thenReturn("encoded");
        when(cusCustomerEntityService.saveCustomer(any(CusCustomer.class))).thenReturn(createDummyCusCustomer());

        CusCustomerDto result = cusCustomerService.saveCustomer(cusCustomerSaveDto);

        assertEquals(createDummyCusCustomerDto(), result);

        ArgumentCaptor<CusCustomer> captor = ArgumentCaptor.forClass(CusCustomer.class);
        verify(cusCustomerEntityService).saveCustomer(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
    }

    @Test
    void shouldNotSaveCustomer_WhenIdentityNo_IsNotUnique() {

        IllegalFieldException expected = new IllegalFieldException(CusErrorMessage.IDENTITY_NO_MUST_BE_UNIQUE);

        doThrow(expected).when(cusCustomerValidationService).controlIsIdentityNoUnique(any(CusCustomer.class));

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> cusCustomerService.saveCustomer(createDummyCusCustomerSaveDto()));

        assertSame(expected, result);
        verify(cusCustomerEntityService, never()).saveCustomer(any());
    }

    @Test
    void shouldNotSaveCustomer_WhenFields_AreNull() {

        IllegalFieldException expected = new IllegalFieldException(CusErrorMessage.FIELD_CANNOT_BE_NULL);

        doThrow(expected).when(cusCustomerValidationService).controlAreFieldsNonNull(any(CusCustomer.class));

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> cusCustomerService.saveCustomer(createDummyCusCustomerSaveDto()));

        assertSame(expected, result);
        verify(cusCustomerEntityService, never()).saveCustomer(any());
    }

    @Test
    void shouldUpdateCustomer() {

        CusCustomerUpdateDto cusCustomerUpdateDto = createDummyCusCustomerUpdateDto();
        CusCustomer existing = createDummyCusCustomer();
        existing.setPassword(cusCustomerUpdateDto.getPassword());

        when(cusCustomerEntityService.findCustomerById(CUSTOMER_ID)).thenReturn(existing);
        when(cusCustomerEntityService.saveCustomer(any(CusCustomer.class))).thenReturn(createDummyCusCustomer());

        CusCustomerDto result = cusCustomerService.updateCustomer(cusCustomerUpdateDto);

        assertEquals(createDummyCusCustomerDto(), result);
        verify(cusCustomerValidationService).controlIsCustomerExist(CUSTOMER_ID);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldNotUpdateCustomer_WhenIdentityNo_IsNotUnique() {

        IllegalFieldException expected = new IllegalFieldException(CusErrorMessage.IDENTITY_NO_MUST_BE_UNIQUE);

        doThrow(expected).when(cusCustomerValidationService).controlIsIdentityNoUnique(any(CusCustomer.class));

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> cusCustomerService.updateCustomer(createDummyCusCustomerUpdateDto()));

        assertSame(expected, result);
        verify(cusCustomerEntityService, never()).saveCustomer(any());
    }

    @Test
    void shouldNotUpdateCustomer_WhenFields_AreNull() {

        IllegalFieldException expected = new IllegalFieldException(CusErrorMessage.FIELD_CANNOT_BE_NULL);

        doThrow(expected).when(cusCustomerValidationService).controlAreFieldsNonNull(any(CusCustomer.class));

        IllegalFieldException result = assertThrows(IllegalFieldException.class,
                () -> cusCustomerService.updateCustomer(createDummyCusCustomerUpdateDto()));

        assertSame(expected, result);
        verify(cusCustomerEntityService, never()).saveCustomer(any());
    }

    @Test
    void shouldDeleteCustomer() {

        CusCustomer cusCustomer = createDummyCusCustomer();

        when(cusCustomerEntityService.getByIdWithControl(CUSTOMER_ID)).thenReturn(cusCustomer);

        cusCustomerService.deleteCustomer(CUSTOMER_ID);

        verify(cusCustomerEntityService).delete(cusCustomer);
    }

    @Test
    void shouldNotDeleteCustomer_WhenId_DoesNotExist() {

        ItemNotFoundException expected = new ItemNotFoundException(GenErrorMessage.ITEM_NOT_FOUND);

        when(cusCustomerEntityService.getByIdWithControl(CUSTOMER_ID)).thenThrow(expected);

        ItemNotFoundException result = assertThrows(ItemNotFoundException.class,
                () -> cusCustomerService.deleteCustomer(CUSTOMER_ID));

        assertSame(expected, result);
        verify(cusCustomerEntityService, never()).delete(any());
    }

    private CusCustomer createDummyCusCustomer(){

        CusCustomer cusCustomer = new CusCustomer();
        cusCustomer.setId(CUSTOMER_ID);
        cusCustomer.setName("testName");
        cusCustomer.setSurname("testSurname");
        cusCustomer.setIdentityNo(11111111111L);
        cusCustomer.setPassword("testPassword");
        return  cusCustomer;
    }

    private CusCustomerDto createDummyCusCustomerDto(){

        CusCustomerDto cusCustomerDto = new CusCustomerDto();
        cusCustomerDto.setId(CUSTOMER_ID);
        cusCustomerDto.setName("testName");
        cusCustomerDto.setSurname("testSurname");
        cusCustomerDto.setIdentityNo(11111111111L);
        return  cusCustomerDto;
    }

    private CusCustomerSaveDto createDummyCusCustomerSaveDto(){

        CusCustomerSaveDto cusCustomerSaveDto = new CusCustomerSaveDto();
        cusCustomerSaveDto.setName("testName");
        cusCustomerSaveDto.setSurname("testSurname");
        cusCustomerSaveDto.setIdentityNo(11111111111L);
        cusCustomerSaveDto.setPassword("test1234");
        return cusCustomerSaveDto;
    }

    private CusCustomerUpdateDto createDummyCusCustomerUpdateDto(){

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();
        cusCustomerUpdateDto.setId(CUSTOMER_ID);
        cusCustomerUpdateDto.setName("testName");
        cusCustomerUpdateDto.setSurname("testSurname");
        cusCustomerUpdateDto.setIdentityNo(11111111111L);
        cusCustomerUpdateDto.setPassword("test1234");
        return cusCustomerUpdateDto;
    }
}
