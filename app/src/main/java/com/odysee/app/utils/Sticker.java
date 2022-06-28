package com.odysee.app.utils;

import lombok.Getter;

public enum Sticker {
    cat_border("cat_border", "CAT/PNG/cat_with_border.png", 0),
    fail_border("fail_border", "FAIL/PNG/fail_with_border.png", 0),
    hype_border("hype_border", "HYPE/PNG/hype_with_border.png", 0),
    pants_1_with_frame("pants_1_with_frame", "PANTS/PNG/PANTS_1_with_frame.png", 0),
    piss("piss","PISS/PNG/piss_with_frame.png", 0),
    pregnant_man_blonde_white_border("pregnant_man_blonde_white_border", "pregnant%20man/png/Pregnant%20man_white%20border_blondie.png", 0),
    pregnant_woman_brown_hair_white_border("pregnant_woman_brown_hair_white_border", "pregnant%20woman/png/Pregnant%20woman_white_border_brown%20hair.png", 0),
    rocket_spaceman_with_border("rocket_spaceman_with_border", "ROCKET%20SPACEMAN/PNG/rocket-spaceman_with-border.png", 0),
    salty("salty", "SALTY/PNG/salty.png", 0),
    sick_2_with_border("sick_2_with_border", "SICK/PNG/sick2_with_border.png", 0),
    sick_1_with_borderdark_with_frame("sick_1_with_borderdark_with_frame", "SICK/PNG/with%20borderdark%20with%20frame.png", 0),
    slime_with_frame("slime_with_frame", "SLIME/PNG/slime_with_frame.png", 0),
    fire_with_frame("fire_with_frame", "MISC/PNG/fire.png", 0),
    sphagetti_bath_with_frame("sphagetti_bath_with_frame", "SPHAGETTI%20BATH/PNG/sphagetti%20bath_with_frame.png", 0),
    thug_life_with_border("thug_life_with_border", "THUG%20LIFE/PNG/thug_life_with_border_clean.png", 0),
    whuut_with_frame("whuut_with_frame", "WHUUT/PNG/whuut_with-frame.png", 0),
    egirl("egirl", "EGIRL/PNG/e-girl.png", 0),
    bull_ride("bull_ride", "BULL/PNG/bull-ride.png", 0),
    trap("trap", "TRAP/PNG/trap.png", 0),
    eliminated("eliminated", "ELIMINATED/PNG/eliminated.png", 0),
    trash("trash", "TRASH/PNG/trash.png", 0),
    ban("ban", "BAN/PNG/ban.png", 0),
    kanye_west("kanye_west", "MISC/PNG/kanye_west.png", 0),
    che_guevara("che_guevara", "MISC/PNG/che_guevara.png", 0),
    bill_cosby("bill_cosby", "MISC/PNG/bill_cosby.png", 0),
    kurt_cobain("kurt_cobain", "MISC/PNG/kurt_cobain.png", 0),
    bill_clinton("bill_clinton", "MISC/PNG/bill_clinton.png", 0),
    chris_chan("chris_chan", "MISC/PNG/chris_chan.png", 0),
    taylor_swift("taylor_swift", "MISC/PNG/taylor_swift.png", 0),
    epstein_island("epstein_island", "MISC/PNG/epstein_island.png", 0),
    donald_trump("donald_trump", "MISC/PNG/donald_trump.png", 0),
    egg_carton("egg_carton", "MISC/PNG/egg_carton.png", 0),
    mount_rushmore("mount_rushmore", "MISC/PNG/mount_rushmore.png", 0),
    money_printer("money_printer", "MISC/PNG/money_printer.png", 0),
    comet_tip("comet_tip", "TIPS/png/$%20comet%20tip%20with%20border.png", 0),
    big_lbc_tip("big_lbc_tip", "TIPS/png/big_LBC_TIPV.png", 0),
    big_tip("big_tip", "TIPS/png/with%20borderbig$tip.png", 0),
    bite_tip("bite_tip", "TIPS/png/bite_$tip_with%20border.png", 0),
    bite_tip_closeup("bite_tip_closeup", "TIPS/png/bite_$tip_closeup.png", 0),
    fortune_chest_lbc("fortune_chest_lbc", "TIPS/png/with%20borderfortunechest_LBC_tip.png", 0),
    fortune_chest("fortune_chest", "TIPS/png/with%20borderfortunechest$_tip.png", 0),
    large_lbc_tip("large_lbc_tip", "TIPS/png/with%20borderlarge_LBC_tip%20.png", 0),
    large_tip("large_tip", "TIPS/png/with%20borderlarge$tip.png", 0),
    bite_lbc_closeup("bite_lbc_closeup", "TIPS/png/LBC%20bite.png", 0),
    lbc_comet_tip("lbc_comet_tip", "TIPS/png/LBC%20comet%20tip%20with%20border.png", 0),
    medium_lbc_tip("medium_lbc_tip", "TIPS/png/with%20bordermedium_LBC_tip%20%20%20%20%20%20%20%20%20%20.png", 0),
    medium_tip("medium_tip", "TIPS/png/with%20bordermedium$_%20tip.png", 0),
    silver_odysee_coin("silver_odysee_coin", "TIPS/png/with%20bordersilver_odysee_coinv.png", 0),
    small_lbc_tip("small_lbc_tip", "TIPS/png/with%20bordersmall_LBC_tip%20.png", 0),
    small_tip("small_tip", "TIPS/png/with%20bordersmall$_tip.png", 0),
    tip_hand_flip("tip_hand_flip", "TIPS/png/tip_hand_flip_$%20_with_border.png", 0),
    tip_hand_flip_coin("tip_hand_flip_coin", "TIPS/png/tip_hand_flip_coin_with_border.png", 0),
    tip_hand_flip_lbc("tip_hand_flip_lbc", "TIPS/png/tip_hand_flip_lbc_with_border.png", 0),
    doge("doge","MISC/PNG/doge.png", 0),
    twitch("twitch", "MISC/PNG/twitch.png", 0);

    @Getter
    private final String name;
    @Getter
    private final String path;
    @Getter
    private final double price;

    Sticker(String name, String path, double price) {
        this.name = name;
        this.path = path;
        this.price = price;
    }

    public static boolean isSticker(String name) {
        Sticker[] stickers = Sticker.values();
        for (Sticker sticker : stickers) {
            if (sticker.name.equals(name)) {
                return true;
            }
        }

        return false;
    }
}
