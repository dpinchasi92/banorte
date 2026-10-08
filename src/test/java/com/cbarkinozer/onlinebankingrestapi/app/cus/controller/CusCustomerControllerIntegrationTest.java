package com.cbarkinozer.onlinebankingrestapi.app.cus.controller;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerUpdateDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.cus.service.entityservice.CusCustomerEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.BaseTest;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
@Transactional
public class CusCustomerControllerIntegrationTest extends BaseTest {

    private static final String BASE_PATH = "/api/v1/customers";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CusCustomerEntityService cusCustomerEntityService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void shouldFindAllCustomers() throws Exception {

        CusCustomer customer = seedCustomer();
        seedCustomer();
        authenticateAs(customer);

        MvcResult result = mockMvc.perform(
                get(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        List<?> customers = (List<?>) getRestResponse(result).getData();
        assertEquals(1, customers.size());
        assertEquals(customer.getId().intValue(), ((Map<?, ?>) customers.get(0)).get("id"));
    }

    @Test
    void shouldFindCustomerById() throws Exception {

        CusCustomer customer = seedCustomer();
        authenticateAs(customer);

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + customer.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
    }

    @Test
    void shouldNotFindCustomerByIdWhenUsrUserIdIsNotExist() throws Exception {

        Long deletedId = seedAndDeleteCustomer();

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + deletedId).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldSaveCustomer() throws Exception {

        CusCustomerSaveDto cusCustomerSaveDto = new CusCustomerSaveDto();
        cusCustomerSaveDto.setName("Test");
        cusCustomerSaveDto.setSurname("Test");
        cusCustomerSaveDto.setIdentityNo(StringUtil.getRandomNumber(11));
        cusCustomerSaveDto.setPassword("test1234");

        String content = objectMapper.writeValueAsString(cusCustomerSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-customer").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertNotNull(cusCustomerEntityService.findByIdentityNo(cusCustomerSaveDto.getIdentityNo()));
    }

    @Test
    void shouldNotSaveCustomer_WhenIdentityNo_IsNotUnique() throws Exception {

        CusCustomer existing = seedCustomer();

        CusCustomerSaveDto cusCustomerSaveDto = new CusCustomerSaveDto();
        cusCustomerSaveDto.setName("Test");
        cusCustomerSaveDto.setSurname("Test");
        cusCustomerSaveDto.setIdentityNo(existing.getIdentityNo());
        cusCustomerSaveDto.setPassword("test1234");

        String content = objectMapper.writeValueAsString(cusCustomerSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-customer").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {

        CusCustomer customer = seedCustomer();
        authenticateAs(customer);

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();
        cusCustomerUpdateDto.setId(customer.getId());
        cusCustomerUpdateDto.setName("Updated");
        cusCustomerUpdateDto.setSurname("Test");
        cusCustomerUpdateDto.setIdentityNo(customer.getIdentityNo());
        cusCustomerUpdateDto.setPassword("test1234");

        String content = objectMapper.writeValueAsString(cusCustomerUpdateDto);

        MvcResult result = mockMvc.perform(
                put(BASE_PATH +"/update-customer").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertEquals("Updated", cusCustomerEntityService.getByIdWithControl(customer.getId()).getName());
    }

    @Test
    void shouldNotUpdateCustomer_WhenFields_AreNull() throws Exception {

        CusCustomer customer = seedCustomer();
        authenticateAs(customer);

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();
        cusCustomerUpdateDto.setId(customer.getId());
        cusCustomerUpdateDto.setName(" ");
        cusCustomerUpdateDto.setSurname("");
        cusCustomerUpdateDto.setIdentityNo(null);
        cusCustomerUpdateDto.setPassword(null);

        String content = objectMapper.writeValueAsString(cusCustomerUpdateDto);

        MvcResult result = mockMvc.perform(
                put(BASE_PATH +"/update-customer").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldNotUpdateCustomer_WhenIdentityNo_IsNotUnique() throws Exception {

        CusCustomer existing = seedCustomer();
        CusCustomer customer = seedCustomer();
        authenticateAs(customer);

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();
        cusCustomerUpdateDto.setId(customer.getId());
        cusCustomerUpdateDto.setName("Test4");
        cusCustomerUpdateDto.setSurname("Test4");
        cusCustomerUpdateDto.setIdentityNo(existing.getIdentityNo());
        cusCustomerUpdateDto.setPassword("123456");

        String content = objectMapper.writeValueAsString(cusCustomerUpdateDto);

        MvcResult result = mockMvc.perform(
                put(BASE_PATH + "/update-customer").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldDeleteCustomer() throws Exception {

        CusCustomer customer = seedCustomer();
        authenticateAs(customer);

        MvcResult result = mockMvc.perform(
                delete(BASE_PATH + "/" + customer.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertTrue(isSuccess(result));
        assertFalse(cusCustomerEntityService.existsById(customer.getId()));
    }

    @Test
    void shouldNoDeleteCustomer_WhenId_DoesNotExist() throws Exception {

        Long deletedId = seedAndDeleteCustomer();

        MvcResult result = mockMvc.perform(
                delete(BASE_PATH + "/" + deletedId).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldNotFindCustomerById_WhenCustomer_BelongsToAnotherCustomer() throws Exception {

        CusCustomer victim = seedCustomer();
        authenticateAs(seedCustomer());

        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/" + victim.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldNotFindAllCustomers_WhenNotAuthenticated() throws Exception {

        seedCustomer();

        MvcResult result = mockMvc.perform(
                get(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        assertFalse(isSuccess(result));
    }

    @Test
    void shouldNotUpdateAnotherCustomer_WhenBodyId_BelongsToVictim() throws Exception {

        CusCustomer victim = seedCustomer();
        CusCustomer attacker = seedCustomer();
        authenticateAs(attacker);

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();
        cusCustomerUpdateDto.setId(victim.getId());
        cusCustomerUpdateDto.setName("Hacked");
        cusCustomerUpdateDto.setSurname("Hacked");
        cusCustomerUpdateDto.setIdentityNo(attacker.getIdentityNo());
        cusCustomerUpdateDto.setPassword("attackerPassword");

        String content = objectMapper.writeValueAsString(cusCustomerUpdateDto);

        mockMvc.perform(
                put(BASE_PATH + "/update-customer").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        CusCustomer victimAfter = cusCustomerEntityService.getByIdWithControl(victim.getId());
        assertEquals("Test", victimAfter.getName());
        assertEquals("test1234", victimAfter.getPassword());
        assertEquals("Hacked", cusCustomerEntityService.getByIdWithControl(attacker.getId()).getName());
    }

    @Test
    void shouldNotDeleteCustomer_WhenCustomer_BelongsToAnotherCustomer() throws Exception {

        CusCustomer victim = seedCustomer();
        authenticateAs(seedCustomer());

        MvcResult result = mockMvc.perform(
                delete(BASE_PATH + "/" + victim.getId()).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        assertFalse(isSuccess(result));
        assertTrue(cusCustomerEntityService.existsById(victim.getId()));
    }

    private CusCustomer seedCustomer() {
        CusCustomer cusCustomer = new CusCustomer();
        cusCustomer.setName("Test");
        cusCustomer.setSurname("Customer");
        cusCustomer.setIdentityNo(StringUtil.getRandomNumber(11));
        cusCustomer.setPassword("test1234");
        return cusCustomerEntityService.save(cusCustomer);
    }

    private Long seedAndDeleteCustomer() {
        CusCustomer cusCustomer = seedCustomer();
        cusCustomerEntityService.delete(cusCustomer);
        return cusCustomer.getId();
    }
}
