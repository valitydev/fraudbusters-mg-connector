package dev.vality.fraudbusters.mg.connector.service;


import dev.vality.fistful.base.EventRange;
import dev.vality.fistful.wallet.ManagementSrv;
import dev.vality.fistful.wallet.WalletState;
import dev.vality.fraudbusters.mg.connector.exception.PaymentInfoNotFoundException;
import dev.vality.fraudbusters.mg.connector.exception.PaymentInfoRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletClientService {

    private final ManagementSrv.Iface walletClient;

    public WalletState getWalletInfoFromFistful(String eventId) {
        try {
            final WalletState walletState = walletClient.get(eventId, new EventRange());
            if (walletState == null) {
                throw new PaymentInfoNotFoundException("Not found invoice info in hg!");
            }
            return walletState;
        } catch (TException e) {
            log.error("Error when getWalletInfoFromFistful eventId: {} e: ", eventId, e);
            throw new PaymentInfoRequestException(e);
        }
    }
}
