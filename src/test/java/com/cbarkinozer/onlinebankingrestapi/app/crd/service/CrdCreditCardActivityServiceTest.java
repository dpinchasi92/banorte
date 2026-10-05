package com.cbarkinozer.onlinebankingrestapi.app.crd.service;

import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardActivityAnalysisDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardActivityDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCardActivity;
import com.cbarkinozer.onlinebankingrestapi.app.crd.enums.CrdCreditCardActivityType;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardActivityEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrdCreditCardActivityServiceTest {

    @Mock
    private CrdCreditCardActivityEntityService crdCreditCardActivityEntityService;

    @Mock
    private CrdCreditCardActivityValidationService crdCreditCardActivityValidationService;

    @InjectMocks
    private CrdCreditCardActivityService crdCreditCardActivityService;

    @Test
    void shouldFindCreditCardActivityByAmountInterval() {

        BigDecimal min = BigDecimal.ONE;
        BigDecimal max = BigDecimal.valueOf(10000);

        List<CrdCreditCardActivity> crdCreditCardActivityList = new ArrayList<>();
        crdCreditCardActivityList.add(createDummyCrdCreditCardActivity());

        when(crdCreditCardActivityEntityService.findCreditCardActivityByAmountInterval(min, max))
                .thenReturn(crdCreditCardActivityList);

        List<CrdCreditCardActivityDto> result = crdCreditCardActivityService.findCreditCardActivityByAmountInterval(min, max);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(BigDecimal.valueOf(9000), result.get(0).getAmount());
        assertEquals(CrdCreditCardActivityType.SPEND, result.get(0).getCardActivityType());
        verify(crdCreditCardActivityValidationService).controlIsParameterMinLargerThanMax(min, max);
    }

    @Test
    void shouldGetCardActivityAnalysis() {

        List<CrdCreditCardActivityAnalysisDto> analysisList = new ArrayList<>();
        analysisList.add(new CrdCreditCardActivityAnalysisDto(CrdCreditCardActivityType.SPEND,
                BigDecimal.valueOf(100), BigDecimal.valueOf(10000), 505.0, 12L));

        when(crdCreditCardActivityEntityService.getCardActivityAnalysis(1L)).thenReturn(analysisList);

        List<CrdCreditCardActivityAnalysisDto> result = crdCreditCardActivityService.getCardActivityAnalysis(1L);

        assertSame(analysisList, result);
        verify(crdCreditCardActivityValidationService).controlIsCreditCardExist(1L);
    }

    private CrdCreditCardActivity createDummyCrdCreditCardActivity() {

        CrdCreditCardActivity crdCreditCardActivity = new CrdCreditCardActivity();
        crdCreditCardActivity.setId(1L);
        crdCreditCardActivity.setCrdCreditCardId(1L);
        crdCreditCardActivity.setAmount(BigDecimal.valueOf(9000));
        crdCreditCardActivity.setTransactionDate(LocalDateTime.now());
        crdCreditCardActivity.setDescription("creditCardActivity");
        crdCreditCardActivity.setCardActivityType(CrdCreditCardActivityType.SPEND);

        return crdCreditCardActivity;
    }
}
