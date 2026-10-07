package com.cbarkinozer.onlinebankingrestapi.app.acc.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.mapper.AccAccountMapper;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.util.ClabeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AccAccountService {

    private final AccAccountEntityService accAccountEntityService;
    private final AccAccountValidationService accAccountValidationService;

    public List<AccAccountDto> findAllAccounts() {

        List<AccAccount> accAccountList = accAccountEntityService.findAllActiveAccounts();

        List<AccAccountDto> accAccountDtoList = AccAccountMapper.INSTANCE.convertToAccAccountDtoList(accAccountList);

        return accAccountDtoList;
    }

    public AccAccountDto findAccountById(Long id) {

        AccAccount accAccount = accAccountEntityService.getByIdWithControl(id);

        AccAccountDto accAccountDto = AccAccountMapper.INSTANCE.convertToAccAccountDto(accAccount);

        return accAccountDto;
    }

    public List<AccAccountDto> findAccountByCustomerId(Long customerId) {

        List<AccAccount> accAccountList = accAccountEntityService.findAccountByCustomerId(customerId);

        List<AccAccountDto> accAccountDtoList = AccAccountMapper.INSTANCE.convertToAccAccountDtoList(accAccountList);

        return accAccountDtoList;
    }

    public AccAccountDto saveAccount(AccAccountSaveDto accAccountSaveDto) {

        Long currentCustomerId = accAccountEntityService.getCurrentCustomerId();

        accAccountValidationService.controlIsCustomerExist(currentCustomerId);

        AccAccount accAccount = AccAccountMapper.INSTANCE.convertToAccAccount(accAccountSaveDto);

        accAccount.setStatusType(GenStatusType.ACTIVE);
        accAccount.setClabe(ClabeUtil.generateClabe());
        accAccount.setCustomerId(currentCustomerId);

        accAccountValidationService.controlIsClabeValid(accAccount.getClabe());
        accAccountValidationService.controlIsClabeUnique(accAccount);

        accAccountValidationService.controlAreFieldsNotNull(accAccount);
        accAccountValidationService.controlIsBalanceNotNegative(accAccount);

        accAccount = accAccountEntityService.save(accAccount);

        AccAccountDto accAccountDto = AccAccountMapper.INSTANCE.convertToAccAccountDto(accAccount);

        return accAccountDto;
    }

    public void cancelAccount(Long id) {

        AccAccount accAccount = accAccountEntityService.getByIdWithControl(id);

        accAccount.setStatusType(GenStatusType.PASSIVE);
        accAccountEntityService.save(accAccount);
    }
}
