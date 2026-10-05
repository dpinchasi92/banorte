package com.cbarkinozer.onlinebankingrestapi.app.loa.controller;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.cus.service.entityservice.CusCustomerEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.BaseTest;
import com.cbarkinozer.onlinebankingrestapi.app.gen.util.StringUtil;
import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.LoaApplyLoanDto;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoan;
import com.cbarkinozer.onlinebankingrestapi.app.loa.enums.LoaLoanStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanEntityService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
@Transactional
class LoaLoanControllerIntegrationTest extends BaseTest {

    private static final String BASE_PATH = "/api/v1/loans";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CusCustomerEntityService cusCustomerEntityService;

    @Autowired
    private LoaLoanEntityService loaLoanEntityService;

    private CusCustomer customer;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        customer = seedCustomer();
    }

    @Test
    void calculateLoan() throws Exception {

        MvcResult result = mockMvc.perform(
                get(BASE_PATH+"/calculate-loan")
                        .param("installmentCount", "24")
                        .param("principalLoanAmount", "3000")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void calculateLateFee() throws Exception {

        LoaLoan loan = seedLoan(LocalDate.now().minusDays(10));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/calculate-late-fee/" + loan.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(LoaLoanStatusType.LATE, loaLoanEntityService.getByIdWithControl(loan.getId()).getLoanStatusType());
    }

    @Test
    void findLoanById() throws Exception{

        LoaLoan loan = seedLoan(LocalDate.now().plusMonths(1));

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + loan.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void applyLoan() throws Exception {

        authenticateAs(customer);

        LoaApplyLoanDto loaApplyLoanDto = new LoaApplyLoanDto();
        loaApplyLoanDto.setInstallmentCount(24);
        loaApplyLoanDto.setPrincipalLoanAmount(BigDecimal.valueOf(3000));
        loaApplyLoanDto.setMonthlySalary(BigDecimal.valueOf(9000));

        String content = objectMapper.writeValueAsString(loaApplyLoanDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/apply-loan").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void payInstallment() throws Exception{

        LoaLoan loan = seedLoan(LocalDate.now().plusMonths(1));

        MvcResult result = mockMvc.perform(
                post(BASE_PATH + "/pay-installment/" + loan.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(0, BigDecimal.valueOf(900).compareTo(loaLoanEntityService.getByIdWithControl(loan.getId()).getRemainingPrincipal()));
    }

    @Test
    void payLoanOff() throws Exception{

        LoaLoan loan = seedLoan(LocalDate.now().plusMonths(1));

        MvcResult result = mockMvc.perform(
                delete(BASE_PATH + "/pay-loan-off/" + loan.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals(LoaLoanStatusType.PAID, loaLoanEntityService.getByIdWithControl(loan.getId()).getLoanStatusType());
    }

    private CusCustomer seedCustomer() {
        CusCustomer cusCustomer = new CusCustomer();
        cusCustomer.setName("Test");
        cusCustomer.setSurname("Customer");
        cusCustomer.setIdentityNo(StringUtil.getRandomNumber(11));
        cusCustomer.setPassword("test1234");
        return cusCustomerEntityService.save(cusCustomer);
    }

    private LoaLoan seedLoan(LocalDate dueDate) {
        LoaLoan loaLoan = new LoaLoan();
        loaLoan.setCustomerId(customer.getId());
        loaLoan.setInstallmentCount(10);
        loaLoan.setPrincipalLoanAmount(BigDecimal.valueOf(1000));
        loaLoan.setMonthlyInstallmentAmount(BigDecimal.valueOf(100));
        loaLoan.setInterestToBePaid(BigDecimal.valueOf(50));
        loaLoan.setPrincipalToBePaid(BigDecimal.valueOf(1000));
        loaLoan.setRemainingPrincipal(BigDecimal.valueOf(1000));
        loaLoan.setDueDate(dueDate);
        loaLoan.setLoanStatusType(LoaLoanStatusType.CONTINUING);
        return loaLoanEntityService.save(loaLoan);
    }
}
