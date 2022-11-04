package com.odysee.app.model.lbryinc;

import lombok.Data;

@Data
public class OdyseeLocale {
    private String continent;
    private String country;
    private boolean euMember;
    private boolean googleLimited;

    public OdyseeLocale(String country, String continent, boolean euMember, boolean googleLimited) {
        this.continent = continent;
        this.country = country;
        this.euMember = euMember;
        this.googleLimited = googleLimited;
    }
}
