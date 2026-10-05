package com.cbarkinozer.onlinebankingrestapi.app.gen;

import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.gen.dto.RestResponse;
import com.cbarkinozer.onlinebankingrestapi.app.sec.security.JwtUserDetails;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;

public class BaseTest {

    protected ObjectMapper objectMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected boolean isSuccess(MvcResult result) throws com.fasterxml.jackson.core.JsonProcessingException,
            UnsupportedEncodingException {

        RestResponse restResponse = getRestResponse(result);

        return isSuccess(restResponse);
    }

    protected RestResponse getRestResponse(MvcResult result) throws com.fasterxml.jackson.core.JsonProcessingException, UnsupportedEncodingException {
        return objectMapper.readerFor(RestResponse.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(result.getResponse().getContentAsString());
    }

    /** Services read the current customer from the security context, so log the seeded customer in. */
    protected void authenticateAs(CusCustomer cusCustomer) {
        JwtUserDetails jwtUserDetails = JwtUserDetails.create(cusCustomer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwtUserDetails, null, jwtUserDetails.getAuthorities()));
    }

    private boolean isSuccess(RestResponse restResponse) {
        return restResponse.isSuccess();
    }

}
