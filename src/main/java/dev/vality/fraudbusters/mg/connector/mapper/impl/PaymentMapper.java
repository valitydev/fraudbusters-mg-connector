package dev.vality.fraudbusters.mg.connector.mapper.impl;

import dev.vality.damsel.domain.Payer;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.fraudbusters.PayerType;
import dev.vality.damsel.fraudbusters.Payment;
import dev.vality.damsel.fraudbusters.PaymentStatus;
import dev.vality.damsel.payment_processing.*;
import dev.vality.fraudbusters.mg.connector.constant.InvoiceEventType;
import dev.vality.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import dev.vality.fraudbusters.mg.connector.mapper.Mapper;
import dev.vality.fraudbusters.mg.connector.mapper.initializer.InfoInitializer;
import dev.vality.fraudbusters.mg.connector.service.HgClientService;
import dev.vality.geck.common.util.TBaseUtil;
import dev.vality.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMapper implements Mapper<InvoiceChange, MachineEvent, Payment> {

    private final HgClientService hgClientService;
    private final InfoInitializer<InvoicePaymentStatusChanged> generalInfoInitiator;

    @Override
    public boolean accept(InvoiceChange change) {
        return InvoiceEventType.INVOICE_PAYMENT_STATUS_CHANGED.getFilter().match(change)
                && (change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus()
                .isSetFailed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus()
                .isSetProcessed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus()
                .isSetCaptured())
                || InvoiceEventType.INVOICE_PAYMENT_STARTED.getFilter().match(change)
                && change.getInvoicePaymentChange().getPayload().getInvoicePaymentStarted().getPayment().getStatus()
                .isSetPending();
    }

    @Override
    public Payment map(InvoiceChange change, MachineEvent event) {
        Payment payment = null;
        if (InvoiceEventType.INVOICE_PAYMENT_STARTED.getFilter().match(change)) {
            String paymentId = change.getInvoicePaymentChange().getId();
            InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
            InvoicePaymentChangePayload payload = invoicePaymentChange.getPayload();
            InvoicePaymentStarted invoicePaymentStarted = payload.getInvoicePaymentStarted();

            InvoicePaymentWrapper invoicePaymentWrapper = invokeHgGetInvoiceInfo(change, event, paymentId);

            var invoice = invoicePaymentWrapper.getInvoice();
            var invoicePayment = invoicePaymentWrapper.getInvoicePayment();

            Payer payer = invoicePayment.getPayment().getPayer();
            PaymentTool paymentTool = generalInfoInitiator.initPaymentTool(payer);
            payment = initPaymentByStartedEvent(event, invoicePaymentStarted, invoice, invoicePayment, payer,
                    paymentTool);
        } else {
            String paymentId = change.getInvoicePaymentChange().getId();
            InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
            InvoicePaymentChangePayload payload = invoicePaymentChange.getPayload();
            InvoicePaymentStatusChanged invoicePaymentStatusChanged = payload.getInvoicePaymentStatusChanged();

            InvoicePaymentWrapper invoicePaymentWrapper = invokeHgGetInvoiceInfo(change, event, paymentId);

            var invoice = invoicePaymentWrapper.getInvoice();
            var invoicePayment = invoicePaymentWrapper.getInvoicePayment();

            Payer payer = invoicePayment.getPayment().getPayer();
            PaymentTool paymentTool = generalInfoInitiator.initPaymentTool(payer);
            payment = initPaymentByChangeStatusEvent(event, invoicePaymentStatusChanged, invoice, invoicePayment, payer,
                    paymentTool);
        }
        log.debug("Map payment: {}", payment);
        return payment;
    }

    private Payment initPaymentByChangeStatusEvent(MachineEvent event,
                                                   InvoicePaymentStatusChanged invoicePaymentStatusChanged,
                                                   dev.vality.damsel.domain.Invoice invoice,
                                                   InvoicePayment invoicePayment,
                                                   Payer payer,
                                                   PaymentTool paymentTool) {
        var payment = invoicePayment.getPayment();
        return new Payment()
                .setStatus(TBaseUtil.unionFieldToEnum(invoicePaymentStatusChanged.getStatus(), PaymentStatus.class))
                .setCost(payment.getCost())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(paymentTool)
                .setId(String.join(DELIMITER, invoice.getId(), payment.getId()))
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment))
                .setPayerType(TBaseUtil.unionFieldToEnum(payer, PayerType.class))
                .setMobile(isMobile(paymentTool))
                .setRecurrent(isRecurrent(payer))
                .setError(generalInfoInitiator.initError(invoicePaymentStatusChanged));
    }

    private Payment initPaymentByStartedEvent(MachineEvent event,
                                              InvoicePaymentStarted invoicePaymentStatusChanged,
                                              dev.vality.damsel.domain.Invoice invoice,
                                              InvoicePayment invoicePayment,
                                              Payer payer,
                                              PaymentTool paymentTool) {
        var payment = invoicePayment.getPayment();
        return new Payment()
                .setStatus(TBaseUtil.unionFieldToEnum(invoicePaymentStatusChanged.getPayment().getStatus(),
                        PaymentStatus.class))
                .setCost(payment.getCost())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(paymentTool)
                .setId(String.join(DELIMITER, invoice.getId(), payment.getId()))
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment))
                .setPayerType(TBaseUtil.unionFieldToEnum(payer, PayerType.class))
                .setMobile(isMobile(paymentTool))
                .setRecurrent(isRecurrent(payer));
    }

    @Override
    public InvoiceEventType getChangeType() {
        return InvoiceEventType.INVOICE_PAYMENT_STATUS_CHANGED;
    }

    private InvoicePaymentWrapper invokeHgGetInvoiceInfo(InvoiceChange change, MachineEvent event, String paymentId) {
        try {
            return hgClientService.getInvoiceInfo(event.getSourceId(), findPayment(), paymentId, event.getEventId());
        } catch (Exception e) {
            log.warn("Problem when get invoice info for event: {} change: {} status: {}", event,
                    change, change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus());
            throw e;
        }
    }

    private BiFunction<String, Invoice, Optional<InvoicePayment>> findPayment() {
        return (id, invoiceInfo) -> invoiceInfo.getPayments().stream()
                .filter(payment -> payment.isSetPayment() && payment.getPayment().getId().equals(id))
                .findFirst();
    }

    public boolean isRecurrent(Payer payer) {
        return payer.isSetRecurrent() || payer.isSetCustomer();
    }

    public boolean isMobile(PaymentTool paymentTool) {
        return paymentTool.isSetBankCard() && paymentTool.getBankCard().getPaymentToken() != null;
    }

}
