package com.cbarkinozer.onlinebankingrestapi.app.acc.controller;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyActivityRequestDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyTransferSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccAccountType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccCurrencyType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccMoneyTransferType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.cus.service.entityservice.CusCustomerEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.BaseTest;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.util.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
@Transactional
class AccAccountControllerIntegrationTest extends BaseTest {

    private static final String BASE_PATH = "/api/v1/accounts";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CusCustomerEntityService cusCustomerEntityService;

    @Autowired
    private AccAccountEntityService accAccountEntityService;

    private CusCustomer customer;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        customer = seedCustomer();
    }

    @Test
    void findAllAccounts() throws Exception{

        seedAccount(BigDecimal.valueOf(1000));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void findAccountById() throws Exception {

        AccAccount account = seedAccount(BigDecimal.valueOf(1000));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + account.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void findAccountByCustomerId() throws Exception{

        seedAccount(BigDecimal.valueOf(1000));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/account/customerId/" + customer.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void saveAccount() throws Exception {

        authenticateAs(customer);

        AccAccountSaveDto accAccountSaveDto = new AccAccountSaveDto();
        accAccountSaveDto.setAccountType(AccAccountType.DEPOSIT);
        accAccountSaveDto.setCurrentBalance(BigDecimal.valueOf(100));
        accAccountSaveDto.setCurrencyType(AccCurrencyType.TL);

        String content = objectMapper.writeValueAsString(accAccountSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-account").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(1, accAccountEntityService.findAccountByCustomerId(customer.getId()).size());
    }

    @Test
    void cancelAccount() throws Exception {

        AccAccount account = seedAccount(BigDecimal.valueOf(1000));

        MvcResult result = mockMvc.perform(
                patch(BASE_PATH + "/" + account.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(GenStatusType.PASSIVE, accAccountEntityService.getByIdWithControl(account.getId()).getStatusType());
    }

    @Test
    void transferMoney() throws Exception {

        AccAccount from = seedAccount(BigDecimal.valueOf(1000));
        AccAccount to = seedAccount(BigDecimal.valueOf(1000));

        AccMoneyTransferSaveDto accMoneyTransferSaveDto = new AccMoneyTransferSaveDto();
        accMoneyTransferSaveDto.setAccountIdFrom(from.getId());
        accMoneyTransferSaveDto.setAccountIdTo(to.getId());
        accMoneyTransferSaveDto.setTransferType(AccMoneyTransferType.DUE);
        accMoneyTransferSaveDto.setAmount(BigDecimal.valueOf(100));
        accMoneyTransferSaveDto.setDescription("Here some test money");

        String content = objectMapper.writeValueAsString(accMoneyTransferSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/transfer-money").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(0, BigDecimal.valueOf(900).compareTo(accAccountEntityService.getByIdWithControl(from.getId()).getCurrentBalance()));
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(accAccountEntityService.getByIdWithControl(to.getId()).getCurrentBalance()));
    }

    @Test
    void withdraw() throws Exception {

        AccAccount account = seedAccount(BigDecimal.valueOf(1000));

        String content = objectMapper.writeValueAsString(createMoneyActivityRequest(account.getId()));

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/withdraw").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(0, BigDecimal.valueOf(900).compareTo(accAccountEntityService.getByIdWithControl(account.getId()).getCurrentBalance()));
    }

    @Test
    void deposit() throws Exception {

        AccAccount account = seedAccount(BigDecimal.valueOf(1000));

        String content = objectMapper.writeValueAsString(createMoneyActivityRequest(account.getId()));

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/deposit").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(0, BigDecimal.valueOf(1100).compareTo(accAccountEntityService.getByIdWithControl(account.getId()).getCurrentBalance()));
    }

    private AccMoneyActivityRequestDto createMoneyActivityRequest(Long accountId) {
        AccMoneyActivityRequestDto accMoneyActivityRequestDto = new AccMoneyActivityRequestDto();
        accMoneyActivityRequestDto.setAccountId(accountId);
        accMoneyActivityRequestDto.setAmount(BigDecimal.valueOf(100));
        return accMoneyActivityRequestDto;
    }

    private CusCustomer seedCustomer() {
        CusCustomer cusCustomer = new CusCustomer();
        cusCustomer.setName("Test");
        cusCustomer.setSurname("Customer");
        cusCustomer.setIdentityNo(StringUtil.getRandomNumber(11));
        cusCustomer.setPassword("test1234");
        return cusCustomerEntityService.save(cusCustomer);
    }

    private AccAccount seedAccount(BigDecimal balance) {
        AccAccount accAccount = new AccAccount();
        accAccount.setCustomerId(customer.getId());
        accAccount.setIbanNo(StringUtil.getRandomNumberAsString(26));
        accAccount.setCurrentBalance(balance);
        accAccount.setCurrencyType(AccCurrencyType.TL);
        accAccount.setAccountType(AccAccountType.DEPOSIT);
        accAccount.setStatusType(GenStatusType.ACTIVE);
        return accAccountEntityService.save(accAccount);
    }
}
