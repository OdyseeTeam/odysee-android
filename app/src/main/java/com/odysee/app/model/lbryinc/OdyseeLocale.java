package com.odysee.app.model.lbryinc;

import lombok.Data;

@Data
public class OdyseeLocale {
    private String continent;
    private String country;
    private boolean euMember;

    public OdyseeLocale(String country, String continent, boolean euMember) {
        this.continent = continent;
        this.country = country;
        this.euMember = euMember;
    }
}
