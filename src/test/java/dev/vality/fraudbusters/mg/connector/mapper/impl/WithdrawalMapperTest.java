package dev.vality.fraudbusters.mg.connector.mapper.impl;

import dev.vality.damsel.fraudbusters.Withdrawal;
import dev.vality.damsel.fraudbusters.WithdrawalStatus;
import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.WithdrawalState;
import dev.vality.fistful.withdrawal.status.Failed;
import dev.vality.fistful.withdrawal.status.Pending;
import dev.vality.fistful.withdrawal.status.Status;
import dev.vality.fraudbusters.mg.connector.converter.FistfulAccountToDomainAccountConverter;
import dev.vality.fraudbusters.mg.connector.converter.FistfulCashToDomainCashConverter;
import dev.vality.fraudbusters.mg.connector.converter.FistfulCurrencyToDomainCurrencyConverter;
import dev.vality.fraudbusters.mg.connector.converter.FistfulResourceToDomainResourceConverter;
import dev.vality.fraudbusters.mg.connector.mapper.Mapper;
import dev.vality.fraudbusters.mg.connector.service.DestinationClientService;
import dev.vality.fraudbusters.mg.connector.service.WalletClientService;
import dev.vality.fraudbusters.mg.connector.service.WithdrawalClientService;
import dev.vality.machinegun.eventsink.MachineEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static dev.vality.fraudbusters.mg.connector.mapper.impl.WithdrawalBeanUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {FistfulAccountToDomainAccountConverter.class,
        FistfulCashToDomainCashConverter.class,
        FistfulCurrencyToDomainCurrencyConverter.class,
        FistfulResourceToDomainResourceConverter.class,
        LogWithdrawalMapperDecorator.class,
        WithdrawalMapper.class})
public class WithdrawalMapperTest {

    public static final String SOURCE_ID = "SOURCE_ID";
    public static final Long EVENT_ID = 1L;
    public static final String WALLET_ID = "walletId";
    public static final String DESTINATION_ID = "destinationId";

    @MockBean
    DestinationClientService destinationClientService;

    @MockBean
    WithdrawalClientService withdrawalClientService;

    @MockBean
    WalletClientService walletClientService;

    @Autowired
    Mapper<TimestampedChange, MachineEvent, Withdrawal> logWithdrawalMapperDecorator;

    @Test
    public void accept() {
        final Status failed = Status.failed(new Failed());
        TimestampedChange timestampedChange = createStatusCahnge(failed);
        boolean accept = logWithdrawalMapperDecorator.accept(timestampedChange);
        assertTrue(accept);

        timestampedChange = createStatusCahnge(Status.pending(new Pending()));
        accept = logWithdrawalMapperDecorator.accept(timestampedChange);
        assertFalse(accept);
    }

    @Test
    public void map() {
        final MachineEvent event = new MachineEvent();
        event.setSourceId(SOURCE_ID);
        event.setEventId(EVENT_ID);

        final WithdrawalState withdrawalState = new WithdrawalState();
        withdrawalState.setBody(createCash());
        withdrawalState.setDestinationId(DESTINATION_ID);
        withdrawalState.setWalletId(WALLET_ID);
        when(withdrawalClientService.getWithdrawalInfoFromFistful(SOURCE_ID, EVENT_ID)).thenReturn(withdrawalState);
        when(walletClientService.getWalletInfoFromFistful(WALLET_ID)).thenReturn(createWallet());
        when(destinationClientService.getDestinationInfoFromFistful(DESTINATION_ID))
                .thenReturn(createDestinationState());

        TimestampedChange timestampedChange = createStatusCahnge(Status.failed(new Failed()));
        final Withdrawal map = logWithdrawalMapperDecorator.map(timestampedChange, event);

        assertEquals(RUB, map.getCost().getCurrency().symbolic_code);
        assertEquals(WithdrawalStatus.failed, map.getStatus());
        assertEquals(SOURCE_ID, map.getId());
        assertTrue(map.getDestinationResource().isSetBankCard());
        assertEquals(IDENTITY_ID, map.getAccount().getIdentity());
        assertEquals(WALLET_ACCOUNT_ID, map.getAccount().getId());
        assertEquals(RUB, map.getAccount().getCurrency().symbolic_code);
    }
}
