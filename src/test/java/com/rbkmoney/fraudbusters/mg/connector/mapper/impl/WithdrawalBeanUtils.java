package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import dev.vality.fistful.account.Account;
import dev.vality.fistful.base.*;
import dev.vality.fistful.destination.DestinationState;
import dev.vality.fistful.wallet.WalletState;
import dev.vality.fistful.withdrawal.Change;
import dev.vality.fistful.withdrawal.StatusChange;
import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.status.Status;
import com.rbkmoney.fraudbusters.mg.connector.utils.InvoiceTestConstant;

public class WithdrawalBeanUtils {

    public static final String IDENTITY_ID = "identity_id";
    public static final String WALLET_ACCOUNT_ID = "wallet_account_id";
    public static final String RUB = "RUB";

    public static DestinationState createDestinationState() {
        final Resource resource = new Resource();
        resource.setBankCard(new ResourceBankCard()
                .setBankCard(new BankCard()
                        .setBankName(InvoiceTestConstant.BANK_NAME)
                        .setBin(InvoiceTestConstant.CARD_BIN)
                        .setCategory(InvoiceTestConstant.CARD_CATEGORY)
                        .setIssuerCountry(Residence.PAN)
                        .setPaymentSystem(new PaymentSystemRef(LegacyBankCardPaymentSystem.mastercard.name()))
                        .setToken(InvoiceTestConstant.CARD_TOKEN_PROVIDER)
                        .setMaskedPan(InvoiceTestConstant.CARD_MASKED_PAN)
                        .setCardType(CardType.debit)
                        .setCardholderName(InvoiceTestConstant.CARDHOLDER_NAME)));
        return new DestinationState().setResource(resource);
    }

    public static TimestampedChange createStatusCahnge(Status failed) {
        final Change change = new Change();
        final TimestampedChange timestampedChange = new TimestampedChange();
        change.setStatusChanged(new StatusChange()
                .setStatus(failed));

        timestampedChange.setChange(change);
        return timestampedChange;
    }

    public static WalletState createWallet() {
        return new WalletState()
                .setAccount(new Account()
                        .setCurrency(new CurrencyRef(RUB))
                        .setId(WALLET_ACCOUNT_ID)
                        .setIdentity(IDENTITY_ID));
    }

    public static Cash createCash() {
        return new Cash()
                .setAmount(100L)
                .setCurrency(new CurrencyRef(RUB));
    }

}
