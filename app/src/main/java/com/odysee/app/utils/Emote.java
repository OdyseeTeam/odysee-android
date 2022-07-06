package com.odysee.app.utils;

import lombok.Getter;

public enum Emote {
    alien("alien", "Alien__multiplier__.png"),
    angry_1("angry_1", "angry__multiplier__.png"),
    angry_2("angry_2", "angry%202__multiplier__.png"),
    angry_3("angry_3", "angry%203__multiplier__.png"),
    angry_4("angry_4", "angry%204__multiplier__.png"),
    blind("blind", "blind__multiplier__.png"),
    block("block", "block__multiplier__.png"),
    bomb("bomb", "bomb__multiplier__.png"),
    brain_chip("brain_chip", "Brain%20chip__multiplier__.png"),
    confirm("confirm", "CONFIRM__multiplier__.png"),
    confused_1("confused_1", "confused__multiplier__-1.png"),
    confused_2("confused_2", "confused__multiplier__.png"),
    cooking_something_nice("cooking_something_nice", "cooking%20something%20nice__multiplier__.png"),
    cry_1("cry_1", "cry__multiplier__.png"),
    cry_2("cry_2", "cry%202__multiplier__.png"),
    cry_3("cry_3", "cry%203__multiplier__.png"),
    cry_4("cry_4", "cry%204__multiplier__.png"),
    cry_5("cry_5", "cry%205__multiplier__.png"),
    donut("donut", "donut__multiplier__.png"),
    eggplant_with_condom("eggplant_with_condom", "eggplant%20with%20condom__multiplier__.png"),
    eggplant("eggplant", "eggplant__multiplier__.png"),
    fire_up("fire_up", "fire%20up__multiplier__.png"),
    flat_earth("flat_earth", "Flat%20earth__multiplier__.png"),
    flying_saucer("flying_saucer", "Flying%20saucer__multiplier__.png"),
    heart_chopper("heart_chopper", "heart%20chopper__multiplier__.png"),
    hyper_troll("hyper_troll", "HyperTroll__multiplier__.png"),
    ice_cream("ice_cream", "ice%20cream__multiplier__.png"),
    idk("idk", "IDK__multiplier__.png"),
    illuminati_1("illuminati_1", "Illuminati__multiplier__-1.png"),
    illuminati_2("illuminati_2", "Illuminati__multiplier__.png"),
    kiss_1("kiss_1", "kiss__multiplier__.png"),
    kiss_2("kiss_2", "kiss%202__multiplier__.png"),
    laser_gun("laser_gun", "laser%20gun__multiplier__.png"),
    laughing_1("laughing_1", "Laughing__multiplier__.png"),
    laughing_2("laughing_2", "Laughing 2__multiplier__.png"),
    lollipop("lollipop", "Lollipop__multiplier__.png"),
    love_1("love_1", "Love__multiplier__.png"),
    love_2("love_2", "Love%202__multiplier__.png"),
    monster("monster", "Monster__multiplier__.png"),
    mushroom("mushroom", "mushroom__multiplier__.png"),
    nail_it("nail_it", "Nail%20It__multiplier__.png"),
    no("no", "NO__multiplier__.png"),
    ouch("ouch", "ouch__multiplier__.png"),
    peace("peace", "peace__multiplier__.png"),
    pizza("pizza", "pizza__multiplier__.png"),
    rabbit_hole("rabbit_hole", "rabbit%20hole__multiplier__.png"),
    rainbow_puke_1("rainbow_puke_1", "rainbow%20puke__multiplier__-1.png"),
    rainbow_puke_2("rainbow_puke_2", "rainbow%20puke__multiplier__.png"),
    rock("rock", "ROCK__multiplier__.png"),
    sad("sad", "sad__multiplier__.png"),
    salty("salty", "salty__multiplier__.png"),
    scary("scary", "scary__multiplier__.png"),
    sleep("sleep", "Sleep__multiplier__.png"),
    slime_down("slime_down", "slime%20down__multiplier__.png"),
    smelly_socks("smelly_socks", "smelly%20socks__multiplier__.png"),
    smile_1("smile_1", "smile__multiplier__.png"),
    smile_2("smile_2", "smile%202__multiplier__.png"),
    space_chad("space_chad", "space%20chad__multiplier__.png"),
    space_doge("space_doge", "doge__multiplier__.png"),
    space_green_wojak("space_green_wojak", "space%20wojak__multiplier__-1.png"),
    space_julian("space_julian", "Space%20Julian__multiplier__.png"),
    space_red_wojak("space_red_wojak", "space%20wojak__multiplier__.png"),
    space_resitas("space_resitas", "resitas__multiplier__.png"),
    space_tom("space_tom", "space%20Tom__multiplier__.png"),
    spock("spock", "SPOCK__multiplier__.png"),
    star("star", "Star__multiplier__.png"),
    sunny_day("sunny_day", "sunny%20day__multiplier__.png"),
    surprised("surprised", "surprised__multiplier__.png"),
    sweet("sweet", "sweet__multiplier__.png"),
    thinking_1("thinking_1", "thinking__multiplier__-1.png"),
    thinking_2("thinking_2", "thinking__multiplier__.png"),
    thumb_down("thumb_down", "thumb%20down__multiplier__.png"),
    thumb_up_1("thumb_up_1", "thumb%20up__multiplier__-1.png"),
    thumb_up_2("thumb_up_2", "thumb%20up__multiplier__.png"),
    tinfoil_hat("tinfoil_hat", "tin%20hat__multiplier__.png"),
    troll_king("troll_king", "Troll%20king__multiplier__.png"),
    ufo("ufo", "ufo__multiplier__.png"),
    waiting("waiting", "waiting__multiplier__.png"),
    what("what", "what___multiplier__.png"),
    woodoo_doll("woodoo_doll", "woodo%20doll__multiplier__.png");

    @Getter
    private final String name;
    @Getter
    private final String path;

    Emote(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getPath(String multiplier) {
        return path.replace("__multiplier__", multiplier);
    }

    public static boolean isEmote(String name) {
        Emote[] emotes = Emote.values();
        for (Emote emote : emotes) {
            if (emote.name.equals(name)) {
                return true;
            }
        }

        return false;
    }
}
