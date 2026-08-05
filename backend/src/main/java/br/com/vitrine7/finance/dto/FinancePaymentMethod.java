package br.com.vitrine7.finance.dto;

public enum FinancePaymentMethod {
    CASH("CASH"),
    PIX("PIX"),
    CREDIT("CREDIT_CARD"),
    DEBIT("DEBIT_CARD");

    private final String databaseValue;

    FinancePaymentMethod(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
