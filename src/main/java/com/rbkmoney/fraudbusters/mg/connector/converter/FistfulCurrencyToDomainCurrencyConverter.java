package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.damsel.domain.CurrencyRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FistfulCurrencyToDomainCurrencyConverter
        implements Converter<dev.vality.fistful.base.CurrencyRef, CurrencyRef> {

    @Override
    public CurrencyRef convert(dev.vality.fistful.base.CurrencyRef currency) {
        return new CurrencyRef()
                .setSymbolicCode(currency.getSymbolicCode());
    }

}
