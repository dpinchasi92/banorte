# AGENTS.md: Domain Rules

All new and changed code in this repo must follow the rules below. Agents and humans must apply them to every change.

> **Caveat:** Most of the existing code predates these rules and is not compliant yet. Each rule lists the known violations. If you touch that code, bring it into compliance or open a follow-up. Don't copy the old patterns into new code.

Paths are relative to `src/main/java/com/cbarkinozer/onlinebankingrestapi/`.

## 1. PCI DSS card data handling

- Never persist the CVV/CVC, not even in encrypted form. Only check it at authorization time and then discard it.
- Never store or log a full PAN (card number) in plaintext. When you display, return, or log a PAN, mask it so only the last 4 digits show (`**** **** **** 1234`). If the full PAN has to be stored, it must be encrypted or tokenized.
- Card data must never appear in logs, `LogDetail` rows, exception messages, or Kafka messages.

**Known violations (to fix):**
- `app/crd/entity/CrdCreditCard.java` persists `cardNo` (full PAN, `CARD_NO`) and `cvvNo` (`CVV_NO`) in plaintext.
- `app/crd/service/CrdCreditCardService.java` generates and saves the CVV. `CrdCreditCardDao.findByCardNoAndCvvNoAndExpireDateAndStatusType` matches cards against the stored CVV.
- `app/crd/dto/CrdCreditCardDto.java` and `CrdCreditCardDetailsDto.java` expose the full PAN, and `CrdCreditCardDto` also exposes the CVV.
- `app/log` (`LogService`, `LogDetail`) and `app/kafka` (`LogMessage`) forward free-text `message`/`description`. Make sure no PAN or CVV can reach them.

## 2. Account identifiers: CLABE, not IBAN

- Mexican accounts are identified by an 18-digit **CLABE**: 3-digit bank code + 3-digit branch (plaza) code + 11-digit account number + 1 check digit. Banorte's bank code is `072`.
- Check digit: multiply each of the first 17 digits by the repeating weights `3, 7, 1`. Take each product mod 10 and add them up. The check digit is `(10 - (sum mod 10)) mod 10`.
- New code must generate valid CLABEs and validate them (exactly 18 digits, numeric, correct check digit). Don't use IBAN.

**Known violations (to fix):**
- `app/acc/entity/AccAccount.java` has `ibanNo` (`IBAN_NO`, length 40). `app/acc/service/AccAccountService.getIbanNo()` returns 26 random digits with no check digit. The IBAN naming also appears in `AccAccountDto`, `AccAccountDao.findByIbanNo`, `AccAccountValidationService` and `AccErrorMessage`.

## 3. Currency: MXN

- The operating currency is **MXN**. New accounts default to MXN, and new code must not assume any other currency.

**Known violations (to fix):**
- `app/acc/enums/AccCurrencyType.java` defines `TL, USD, EURO`, has no `MXN`, and sets no default. Restrict it to MXN (or add MXN as the default) and update `AccAccount.currencyType` to match.

## 4. Tax: IVA at 16%

- The tax is **IVA at 16%**. The single source of truth is `TaxProperties.IVA_RATE = new BigDecimal("0.16")` in `app/gen/config/TaxProperties.java`, which is the default for the `banorte.tax.iva-rate` property. Read the rate through `TaxProperties.getIvaRate()`; don't hardcode tax rates.
- IVA is charged on interest (loan interest and late-fee interest), not on principal.
