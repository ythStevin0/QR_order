package com.qrorder.payment.domain;

import com.qrorder.ordering.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * QRIS Payment implementation (Polymorphic Child).
 */
@Entity
@Table(name = "payment_qris")
@DiscriminatorValue("QRIS")
public class QrisPayment extends Payment {

    @Column(name = "qr_string", columnDefinition = "TEXT")
    private String qrString;

    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @Column(length = 50)
    private String nmid;

    @Column(length = 50)
    private String rrn;

    protected QrisPayment() {
        super();
    }

    public QrisPayment(Order order, PaymentProvider provider, String reference, long amount, Instant expiresAt,
                       String qrString, String invoiceUrl, String nmid) {
        super(order, provider, reference, amount, expiresAt);
        this.qrString = qrString;
        this.invoiceUrl = invoiceUrl;
        this.nmid = nmid;
    }

    public String getQrString() { return qrString; }
    public void setQrString(String qrString) { this.qrString = qrString; }

    public String getInvoiceUrl() { return invoiceUrl; }
    public void setInvoiceUrl(String invoiceUrl) { this.invoiceUrl = invoiceUrl; }

    public String getNmid() { return nmid; }
    public void setNmid(String nmid) { this.nmid = nmid; }

    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }
}
