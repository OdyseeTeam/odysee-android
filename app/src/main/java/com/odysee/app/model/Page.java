package com.odysee.app.model;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

public class Page {
    @Getter
    private final List<Claim> claims;
    @Getter
    private final boolean isLastPage;

    public Page(List<Claim> claims, boolean isLastPage) {
        this.claims = claims;
        this.isLastPage = isLastPage;
    }
}
