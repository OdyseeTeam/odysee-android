package com.odysee.app.utils;

import lombok.Getter;

public enum Currency {
    EUR("EUR", "€", false),
    GBP("GBP", "£", false),
    USD("USD", "$", false);

    Currency(String code, String symbol, boolean suffix) {
        this.code = code;
        this.symbol = symbol;
        this.suffix = suffix;
    }

    @Getter
    private String code;
    @Getter
    private String symbol;
    @Getter
    private boolean suffix;

    public static boolean isCurrency(String code) {
        Currency[] currencies = Currency.values();
        for (Currency currency : currencies) {
            if (currency.code.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
