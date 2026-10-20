package com.qrorder.payment.domain;

import com.qrorder.ordering.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * EDC Card Payment (Debit / Credit) handled at POS (Polymorphic Child).
 */
@Entity
@Table(name = "payment_edc")
@DiscriminatorValue("EDC")
public class EdcCardPayment extends Payment {

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "approval_code", nullable = false, length = 50)
    private String approvalCode;

    @Column(name = "masked_card_number", nullable = false, length = 20)
    private String maskedCardNumber;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType; // DEBIT or CREDIT

    protected EdcCardPayment() {
        super();
    }

    public EdcCardPayment(Order order, PaymentProvider provider, String reference, long amount,
                          String bankName, String approvalCode, String maskedCardNumber, String cardType) {
        super(order, provider, reference, amount, Instant.now().plusSeconds(300));
        this.bankName = bankName;
        this.approvalCode = approvalCode;
        this.maskedCardNumber = maskedCardNumber;
        this.cardType = cardType;
    }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getApprovalCode() { return approvalCode; }
    public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }

    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
}
