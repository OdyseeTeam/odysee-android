package com.odysee.app.model;

import lombok.Data;

@Data
public class Fee {
    private String amount;
    private String currency;
    private String address;
}
