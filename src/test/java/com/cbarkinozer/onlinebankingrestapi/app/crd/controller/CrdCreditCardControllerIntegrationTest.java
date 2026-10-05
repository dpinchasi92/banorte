package com.cbarkinozer.onlinebankingrestapi.app.crd.controller;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCard;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCardActivity;
import com.cbarkinozer.onlinebankingrestapi.app.crd.enums.CrdCreditCardActivityType;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardActivityEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardEntityService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
@Transactional
class CrdCreditCardControllerIntegrationTest extends BaseTest {

    private static final String BASE_PATH = "/api/v1/credit-cards";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CusCustomerEntityService cusCustomerEntityService;

    @Autowired
    private CrdCreditCardEntityService crdCreditCardEntityService;

    @Autowired
    private CrdCreditCardActivityEntityService crdCreditCardActivityEntityService;

    private CusCustomer customer;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        customer = seedCustomer();
    }

    @Test
    void findAllCreditCards() throws Exception{

        seedCreditCard();

        MvcResult result = mockMvc.perform(
                get(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void findCreditCardById() throws Exception {

        CrdCreditCard card = seedCreditCard();

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + card.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void findCreditCardActivityByAmountInterval() throws Exception {

        seedActivity(seedCreditCard(), BigDecimal.valueOf(150));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH+ "/find-activity-by-amount-interval").param("min", "100")
                        .param("max", "200").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void findCreditCardActivityBetweenDates() throws Exception{

        CrdCreditCard card = seedCreditCard();
        seedActivity(card, BigDecimal.valueOf(150));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + card.getId() + "/activities")
                        .param("startDate", LocalDate.now().minusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void getCardActivityAnalysis() throws  Exception{

        CrdCreditCard card = seedCreditCard();
        seedActivity(card, BigDecimal.valueOf(150));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/get-card-activity-analysis/" + card.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void getCardDetails() throws Exception {

        CrdCreditCard card = seedCreditCard();

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + card.getId() + "/cardDetails").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void saveCreditCard() throws Exception{

        authenticateAs(customer);

        CrdCreditCardSaveDto crdCreditCardSaveDto = new CrdCreditCardSaveDto();
        crdCreditCardSaveDto.setCutOffDay(15);
        crdCreditCardSaveDto.setEarning(BigDecimal.valueOf(9000));

        String content = objectMapper.writeValueAsString(crdCreditCardSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-credit-card").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void spendMoney() throws Exception{

        CrdCreditCard card = seedCreditCard();

        CrdCreditCardSpendDto crdCreditCardSpendDto = new CrdCreditCardSpendDto();
        crdCreditCardSpendDto.setCardNo(card.getCardNo());
        crdCreditCardSpendDto.setCvvNo(card.getCvvNo());
        crdCreditCardSpendDto.setExpireDate(card.getExpireDate());
        crdCreditCardSpendDto.setAmount(BigDecimal.valueOf(100));
        crdCreditCardSpendDto.setDescription("Here some money");

        String content = objectMapper.writeValueAsString(crdCreditCardSpendDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/spend-money").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(crdCreditCardEntityService.getByIdWithControl(card.getId()).getCurrentDebt()));
    }

    @Test
    void refundMoney() throws Exception{

        CrdCreditCard card = seedCreditCard();
        CrdCreditCardActivity activity = seedActivity(card, BigDecimal.valueOf(150));

        MvcResult result = mockMvc.perform(
                post(BASE_PATH + "/refund/" + activity.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void receivePayment() throws Exception{

        CrdCreditCard card = seedCreditCard();

        CrdCreditCardPaymentDto crdCreditCardPaymentDto = new CrdCreditCardPaymentDto();
        crdCreditCardPaymentDto.setCrdCreditCardId(card.getId());
        crdCreditCardPaymentDto.setAmount(BigDecimal.valueOf(100));

        String content = objectMapper.writeValueAsString(crdCreditCardPaymentDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/receive-payment").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void cancelCreditCard() throws Exception{

        CrdCreditCard card = seedCreditCard();

        MvcResult result = mockMvc.perform(
                patch(BASE_PATH + "/" + card.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(GenStatusType.PASSIVE, crdCreditCardEntityService.getByIdWithControl(card.getId()).getStatusType());
    }

    private CusCustomer seedCustomer() {
        CusCustomer cusCustomer = new CusCustomer();
        cusCustomer.setName("Test");
        cusCustomer.setSurname("Customer");
        cusCustomer.setIdentityNo(StringUtil.getRandomNumber(11));
        cusCustomer.setPassword("test1234");
        return cusCustomerEntityService.save(cusCustomer);
    }

    private CrdCreditCard seedCreditCard() {
        CrdCreditCard crdCreditCard = new CrdCreditCard();
        crdCreditCard.setCusCustomerId(customer.getId());
        crdCreditCard.setCardNo(StringUtil.getRandomNumber(16));
        crdCreditCard.setCvvNo(StringUtil.getRandomNumber(3));
        crdCreditCard.setExpireDate(LocalDate.now().plusYears(3));
        crdCreditCard.setTotalLimit(BigDecimal.valueOf(1000));
        crdCreditCard.setAvailableCardLimit(BigDecimal.valueOf(1000));
        crdCreditCard.setCurrentDebt(BigDecimal.ZERO);
        crdCreditCard.setMinimumPaymentAmount(BigDecimal.ZERO);
        crdCreditCard.setCutoffDate(LocalDate.now().plusDays(10));
        crdCreditCard.setDueDate(LocalDate.now().plusDays(20));
        crdCreditCard.setStatusType(GenStatusType.ACTIVE);
        return crdCreditCardEntityService.save(crdCreditCard);
    }

    private CrdCreditCardActivity seedActivity(CrdCreditCard card, BigDecimal amount) {
        CrdCreditCardActivity activity = new CrdCreditCardActivity();
        activity.setCrdCreditCardId(card.getId());
        activity.setAmount(amount);
        activity.setTransactionDate(LocalDateTime.now());
        activity.setDescription("Seeded spend");
        activity.setCardActivityType(CrdCreditCardActivityType.SPEND);
        return crdCreditCardActivityEntityService.save(activity);
    }
}
