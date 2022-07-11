package com.odysee.app.model.lbryinc;

import lombok.Data;

@Data
public class CustomBlockRule {
    private ContentType type;
    private Scope scope;
    private String reason;
    private String trigger;
    private String id;
    private String message;

    public enum ContentType {
        livestreams,
        videos
    }
    public enum Scope {
        continent,
        country,
        special
    }

    @Data
    public static class CustomBlockStatus {
        private boolean blocked;
        private String message;
    }
}
