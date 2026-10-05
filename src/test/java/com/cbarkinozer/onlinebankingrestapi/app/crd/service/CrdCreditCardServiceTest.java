package com.cbarkinozer.onlinebankingrestapi.app.crd.service;

import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCard;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCardActivity;
import com.cbarkinozer.onlinebankingrestapi.app.crd.enums.CrdCreditCardActivityType;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardActivityEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrdCreditCardServiceTest {

    private static final Long CARD_ID = 1L;
    private static final Long CUSTOMER_ID = 5L;

    @Mock
    private  CrdCreditCardEntityService crdCreditCardEntityService;

    @Mock
    private  CrdCreditCardValidationService crdCreditCardValidationService;

    @Mock
    private  CrdCreditCardActivityEntityService crdCreditCardActivityEntityService;

    @InjectMocks
    private CrdCreditCardService crdCreditCardService;

    @Test
    void shouldFindAllCreditCards() {

        List<CrdCreditCard> crdCreditCardList = new ArrayList<>();
        crdCreditCardList.add(createCreditCard());

        when(crdCreditCardEntityService.findAllActiveCreditCardList()).thenReturn(crdCreditCardList);

        List<CrdCreditCardDto> result = crdCreditCardService.findAllCreditCards();

        assertEquals(1, result.size());
        assertEquals(CARD_ID, result.get(0).getId());
    }

    @Test
    void shouldFindCreditCardById() {

        when(crdCreditCardEntityService.getByIdWithControl(CARD_ID)).thenReturn(createCreditCard());

        CrdCreditCardDto crdCreditCardDto = crdCreditCardService.findCreditCardById(CARD_ID);

        assertEquals(CARD_ID, crdCreditCardDto.getId());
    }

    @Test
    void shouldGetCardDetails() {

        CrdCreditCard crdCreditCard = createCreditCard();
        CrdCreditCardDetailsDto detailsDto = new CrdCreditCardDetailsDto("name", "surname",
                crdCreditCard.getCardNo(), crdCreditCard.getExpireDate(), crdCreditCard.getCurrentDebt(),
                crdCreditCard.getMinimumPaymentAmount(), crdCreditCard.getCutoffDate(), crdCreditCard.getDueDate());

        List<CrdCreditCardActivity> activityList = new ArrayList<>();
        activityList.add(createActivity(10L, BigDecimal.valueOf(200), CrdCreditCardActivityType.SPEND));

        LocalDateTime termEndDate = crdCreditCard.getCutoffDate().atStartOfDay();

        when(crdCreditCardEntityService.getByIdWithControl(CARD_ID)).thenReturn(crdCreditCard);
        when(crdCreditCardEntityService.getCreditCardDetails(CARD_ID)).thenReturn(detailsDto);
        when(crdCreditCardActivityEntityService.findAllByCrdCreditCardIdAndTransactionDateBetween(
                CARD_ID, termEndDate.minusMonths(1), termEndDate)).thenReturn(activityList);

        CrdCreditCardDetailsDto result = crdCreditCardService.getCardDetails(CARD_ID);

        assertSame(detailsDto, result);
        assertEquals(1, result.getCrdCreditCardActivityDtoList().size());
        assertEquals(10L, result.getCrdCreditCardActivityDtoList().get(0).getId());
    }

    @Test
    void shouldSaveCreditCard() {

        CrdCreditCardSaveDto crdCreditCardSaveDto = new CrdCreditCardSaveDto();
        crdCreditCardSaveDto.setEarning(BigDecimal.valueOf(1000));
        crdCreditCardSaveDto.setCutOffDay(15);

        when(crdCreditCardEntityService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        when(crdCreditCardEntityService.save(any(CrdCreditCard.class))).thenAnswer(inv -> {
            CrdCreditCard saved = inv.getArgument(0);
            saved.setId(CARD_ID);
            return saved;
        });

        CrdCreditCardDto result = crdCreditCardService.saveCreditCard(crdCreditCardSaveDto);

        assertEquals(CARD_ID, result.getId());
        assertEquals(CUSTOMER_ID, result.getCusCustomerId());
        assertEquals(BigDecimal.valueOf(3000), result.getTotalLimit());
        assertEquals(BigDecimal.valueOf(3000), result.getAvailableCardLimit());
        assertEquals(15, result.getCutoffDate().getDayOfMonth());
        assertEquals(result.getCutoffDate().plusDays(10), result.getDueDate());
    }

    @Test
    void shouldCancelCreditCard() {

        CrdCreditCard crdCreditCard = createCreditCard();

        when(crdCreditCardEntityService.getByIdWithControl(CARD_ID)).thenReturn(crdCreditCard);

        crdCreditCardService.cancelCreditCard(CARD_ID);

        assertEquals(GenStatusType.PASSIVE, crdCreditCard.getStatusType());
        assertNotNull(crdCreditCard.getCancelDate());
        verify(crdCreditCardEntityService).save(crdCreditCard);
    }

    @Test
    void shouldFindCreditCardActivityBetweenDates() {

        List<CrdCreditCardActivity> activityList = new ArrayList<>();
        activityList.add(createActivity(10L, BigDecimal.valueOf(200), CrdCreditCardActivityType.SPEND));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusMonths(24);

        when(crdCreditCardActivityEntityService.findCreditCardActivityBetweenDates(
                CARD_ID, startDate.atStartOfDay(), endDate.atStartOfDay())
        ).thenReturn(activityList);

        List<CrdCreditCardActivityDto> result = crdCreditCardService.findCreditCardActivityBetweenDates(
                CARD_ID, startDate, endDate);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void shouldSpendMoney() {

        CrdCreditCard crdCreditCard = createCreditCard();

        CrdCreditCardSpendDto spendDto = new CrdCreditCardSpendDto();
        spendDto.setCardNo(crdCreditCard.getCardNo());
        spendDto.setCvvNo(crdCreditCard.getCvvNo());
        spendDto.setExpireDate(crdCreditCard.getExpireDate());
        spendDto.setAmount(BigDecimal.valueOf(200));
        spendDto.setDescription("market");

        when(crdCreditCardEntityService.findByCardNoAndCvvNoAndExpireDate(
                crdCreditCard.getCardNo(), crdCreditCard.getCvvNo(), crdCreditCard.getExpireDate()))
                .thenReturn(crdCreditCard);
        when(crdCreditCardEntityService.save(crdCreditCard)).thenReturn(crdCreditCard);
        when(crdCreditCardActivityEntityService.save(any(CrdCreditCardActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        CrdCreditCardActivityDto result = crdCreditCardService.spendMoney(spendDto);

        assertEquals(BigDecimal.valueOf(200), result.getAmount());
        assertEquals(CARD_ID, result.getCrdCreditCardId());
        assertEquals(CrdCreditCardActivityType.SPEND, result.getCardActivityType());
        assertEquals("market", result.getDescription());
        assertEquals(BigDecimal.valueOf(200), crdCreditCard.getCurrentDebt());
        assertEquals(BigDecimal.valueOf(800), crdCreditCard.getAvailableCardLimit());
        verify(crdCreditCardValidationService).validateCardLimit(BigDecimal.valueOf(800));
    }

    @Test
    void shouldRefundMoney() {

        CrdCreditCard crdCreditCard = createCreditCard();
        crdCreditCard.setCurrentDebt(BigDecimal.valueOf(200));
        crdCreditCard.setAvailableCardLimit(BigDecimal.valueOf(800));

        CrdCreditCardActivity oldActivity = createActivity(10L, BigDecimal.valueOf(200), CrdCreditCardActivityType.SPEND);

        when(crdCreditCardActivityEntityService.getByIdWithControl(10L)).thenReturn(oldActivity);
        when(crdCreditCardEntityService.getByIdWithControl(CARD_ID)).thenReturn(crdCreditCard);
        when(crdCreditCardEntityService.save(crdCreditCard)).thenReturn(crdCreditCard);
        when(crdCreditCardActivityEntityService.save(any(CrdCreditCardActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        CrdCreditCardActivityDto result = crdCreditCardService.refundMoney(10L);

        assertEquals(BigDecimal.valueOf(200), result.getAmount());
        assertEquals(CrdCreditCardActivityType.REFUND, result.getCardActivityType());
        assertEquals("REFUND : " + oldActivity.getDescription(), result.getDescription());
        assertEquals(0, BigDecimal.ZERO.compareTo(crdCreditCard.getCurrentDebt()));
        assertEquals(BigDecimal.valueOf(1000), crdCreditCard.getAvailableCardLimit());
    }

    @Test
    void shouldReceivePayment() {

        CrdCreditCard crdCreditCard = createCreditCard();
        crdCreditCard.setCurrentDebt(BigDecimal.valueOf(200));
        crdCreditCard.setAvailableCardLimit(BigDecimal.valueOf(800));

        CrdCreditCardPaymentDto paymentDto = new CrdCreditCardPaymentDto();
        paymentDto.setCrdCreditCardId(CARD_ID);
        paymentDto.setAmount(BigDecimal.valueOf(200));

        when(crdCreditCardEntityService.getByIdWithControl(CARD_ID)).thenReturn(crdCreditCard);
        when(crdCreditCardEntityService.save(crdCreditCard)).thenReturn(crdCreditCard);
        when(crdCreditCardActivityEntityService.save(any(CrdCreditCardActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        CrdCreditCardActivityDto result = crdCreditCardService.receivePayment(paymentDto);

        assertEquals(BigDecimal.valueOf(200), result.getAmount());
        assertEquals(CARD_ID, result.getCrdCreditCardId());
        assertEquals(CrdCreditCardActivityType.PAYMENT, result.getCardActivityType());
        assertEquals(BigDecimal.valueOf(1000), crdCreditCard.getAvailableCardLimit());
        verify(crdCreditCardValidationService).controlAreFieldsNull(CARD_ID, BigDecimal.valueOf(200));
    }

    private CrdCreditCard createCreditCard() {
        CrdCreditCard crdCreditCard = new CrdCreditCard();
        crdCreditCard.setId(CARD_ID);
        crdCreditCard.setCusCustomerId(CUSTOMER_ID);
        crdCreditCard.setCardNo(1234567890123456L);
        crdCreditCard.setCvvNo(123L);
        crdCreditCard.setExpireDate(LocalDate.now().plusYears(3));
        crdCreditCard.setTotalLimit(BigDecimal.valueOf(1000));
        crdCreditCard.setAvailableCardLimit(BigDecimal.valueOf(1000));
        crdCreditCard.setCurrentDebt(BigDecimal.ZERO);
        crdCreditCard.setMinimumPaymentAmount(BigDecimal.ZERO);
        crdCreditCard.setCutoffDate(LocalDate.now().plusDays(10));
        crdCreditCard.setDueDate(LocalDate.now().plusDays(20));
        crdCreditCard.setStatusType(GenStatusType.ACTIVE);
        return crdCreditCard;
    }

    private CrdCreditCardActivity createActivity(Long id, BigDecimal amount, CrdCreditCardActivityType type) {
        CrdCreditCardActivity activity = new CrdCreditCardActivity();
        activity.setId(id);
        activity.setCrdCreditCardId(CARD_ID);
        activity.setAmount(amount);
        activity.setDescription("market");
        activity.setTransactionDate(LocalDateTime.now());
        activity.setCardActivityType(type);
        return activity;
    }
}
