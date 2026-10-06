package com.cbarkinozer.onlinebankingrestapi.app.sec.config;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.service.CusCustomerService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.util.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pins the endpoint protection rules of the SecurityFilterChain (the other integration tests run without the filter chain). */
@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
@Transactional
class SecurityConfigIntegrationTest {

    private static final String PASSWORD = "Secr3t!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CusCustomerService cusCustomerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void shouldAllowSwaggerAndApiDocsWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void shouldAllowRegisterWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCustomer())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProtectedEndpointsWithoutToken() throws Exception {
        for (String path : new String[]{"/api/v1/customers", "/api/v1/accounts", "/api/v1/credit-cards", "/api/v1/loans/1"}) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowProtectedEndpointWithTokenFromLogin() throws Exception {
        CusCustomerSaveDto customer = newCustomer();
        cusCustomerService.saveCustomer(customer);

        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityNo\":" + customer.getIdentityNo() + ",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        String token = response.get("data").asText();
        assertTrue(token.startsWith("Bearer "));

        mockMvc.perform(get("/api/v1/customers").header("Authorization", token))
                .andExpect(status().isOk());
    }

    private CusCustomerSaveDto newCustomer() {
        CusCustomerSaveDto dto = new CusCustomerSaveDto();
        dto.setName("Ana");
        dto.setSurname("Lopez");
        dto.setIdentityNo(StringUtil.getRandomNumber(11));
        dto.setPassword(PASSWORD);
        return dto;
    }
}
