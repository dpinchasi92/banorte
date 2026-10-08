package com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice;

import com.cbarkinozer.onlinebankingrestapi.app.crd.dao.CrdCreditCardDao;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardDetailsDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCard;
import com.cbarkinozer.onlinebankingrestapi.app.crd.enums.CrdErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.service.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CrdCreditCardEntityService extends BaseEntityService<CrdCreditCard, CrdCreditCardDao> {

    public CrdCreditCardEntityService(CrdCreditCardDao dao) {
        super(dao);
    }

    public List<CrdCreditCard> findAllByStatusType(GenStatusType statusType){
        return getDao().findAllByStatusType(statusType);
    }

    public List<CrdCreditCard> findAllActiveCreditCardListOfCurrentCustomer() {
        return getDao().findAllByCusCustomerIdAndStatusType(getCurrentCustomerId(), GenStatusType.ACTIVE);
    }

    /** Cards of other customers are reported as not found so their ids cannot be probed. */
    public CrdCreditCard getByIdOfCurrentCustomerWithControl(Long id) {
        return getDao().findByIdAndCusCustomerId(id, getCurrentCustomerId())
                .orElseThrow(() -> new ItemNotFoundException(CrdErrorMessage.CREDIT_CARD_NOT_FOUND));
    }

    public CrdCreditCard findByCardNoAndCvvNoAndExpireDateOfCurrentCustomer(Long cardNo, Long cvvNo, LocalDate expireDate){
        return getDao().findByCardNoAndCvvNoAndExpireDateAndCusCustomerIdAndStatusType(
                cardNo, cvvNo, expireDate, getCurrentCustomerId(), GenStatusType.ACTIVE);
    }

    public CrdCreditCardDetailsDto getCreditCardDetails(Long creditCardId) {
        return getDao().getCreditCardDetails(creditCardId);
    }
}
