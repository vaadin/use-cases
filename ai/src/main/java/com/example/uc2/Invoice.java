package com.example.uc2;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * The invoice data the accounts-payable form edits.
 */
public class Invoice {

    private @Nullable String vendor;
    private @Nullable String invoiceNumber;
    private @Nullable LocalDate invoiceDate;
    private @Nullable LocalDate dueDate;
    private @Nullable String currency;
    private @Nullable BigDecimal netAmount;
    private @Nullable BigDecimal vatAmount;
    private @Nullable BigDecimal totalAmount;
    private @Nullable String iban;

    /**
     * Whether net amount plus VAT equals the total. Missing amounts are left to
     * the required-field checks.
     */
    public boolean amountsAddUp() {
        if (netAmount == null || vatAmount == null || totalAmount == null) {
            return true;
        }
        return netAmount.add(vatAmount).compareTo(totalAmount) == 0;
    }

    public @Nullable String getVendor() {
        return vendor;
    }

    public void setVendor(@Nullable String vendor) {
        this.vendor = vendor;
    }

    public @Nullable String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(@Nullable String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public @Nullable LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(@Nullable LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public @Nullable LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(@Nullable LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public @Nullable String getCurrency() {
        return currency;
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public @Nullable BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(@Nullable BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public @Nullable BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(@Nullable BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public @Nullable BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(@Nullable BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public @Nullable String getIban() {
        return iban;
    }

    public void setIban(@Nullable String iban) {
        this.iban = iban;
    }
}
