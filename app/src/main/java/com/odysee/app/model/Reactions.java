package com.odysee.app.model;

import lombok.Data;
import lombok.Getter;

@Data
public class Reactions {
    private int othersLikes;
    private int othersDislikes;
    private boolean liked;
    private boolean disliked;

    public Reactions() { }
    public Reactions(int likes, int dislikes) {
        this(likes, dislikes, false, false);
    }
    public Reactions(int likes, int dislikes, boolean liked, boolean disliked) {
        if (liked && disliked)
            throw new IllegalArgumentException("Claim cannot be both liked and disliked at the same time");

        this.othersLikes = likes;
        this.othersDislikes = dislikes;
        this.liked = liked;
        this.disliked = disliked;
    }

    /**
     * Convenience function for common construction case.
     */
    public static Reactions newInstanceWithNoLikesOrDislikes() {
        return new Reactions(0, 0, false, false);
    }
}
