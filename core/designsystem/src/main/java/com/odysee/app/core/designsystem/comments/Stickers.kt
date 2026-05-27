package com.odysee.app.core.designsystem.comments

data class StickerDef(val name: String, val url: String, val priceLbc: Int? = null)

private fun cdn(path: String) = "https://static.odycdn.com/stickers/$path"

private fun s(name: String, path: String, price: Int? = null) =
    StickerDef(name = ":$name:", url = cdn(path), priceLbc = price)

val FREE_GLOBAL_STICKERS = listOf(
    s("FIRE", "MISC/PNG/fire.png"),
    s("SLIME", "SLIME/PNG/slime_with_frame.png"),
    s("PISS", "PISS/PNG/piss_with_frame.png"),
    s("THUMBS_UP", "MISC/PNG/thumbs_up.png"),
    s("BRAVO", "MISC/PNG/bravo.png"),
    s("WOW", "MISC/PNG/wow.png"),
    s("GRR", "MISC/PNG/grr.png"),
    s("ACTUALLY", "MISC/PNG/actually.png"),
    s("INTERESTING", "MISC/PNG/interesting.png"),
    s("CAT", "CAT/PNG/cat_with_border.png"),
    s("FAIL", "FAIL/PNG/fail_with_border.png"),
    s("HYPE", "HYPE/PNG/hype_with_border.png"),
    s("PANTS_1", "PANTS/PNG/PANTS_1_with_frame.png"),
    s("DOGE", "MISC/PNG/doge.png"),
    s("EGG_CARTON", "MISC/PNG/egg_carton.png"),
    s("WAITING", "MISC/PNG/waiting.png"),
    s("BULL_RIDE", "BULL/PNG/bull-ride.png"),
    s("ELIMINATED", "ELIMINATED/PNG/eliminated.png"),
    s("BAN", "BAN/PNG/ban.png"),
    s("MONEY_PRINTER", "MISC/PNG/money_printer.png"),
    s("MOUNT_RUSHMORE", "MISC/PNG/mount_rushmore.png"),
    s("KANYE_WEST", "MISC/PNG/kanye_west.png"),
    s("TAYLOR_SWIFT", "MISC/PNG/taylor_swift.png"),
    s("DONALD_TRUMP", "MISC/PNG/donald_trump.png"),
    s("BILL_CLINTON", "MISC/PNG/bill_clinton.png"),
    s("EPSTEIN_ISLAND", "MISC/PNG/epstein_island.png"),
    s("KURT_COBAIN", "MISC/PNG/kurt_cobain.png"),
    s("BILL_COSBY", "MISC/PNG/bill_cosby.png"),
    s("CHE_GUEVARA", "MISC/PNG/che_guevara.png"),
    s("PREGNANT_MAN_BLONDE", "pregnant%20man/png/Pregnant%20man_white%20border_blondie.png"),
    s("ROCKET_SPACEMAN", "ROCKET%20SPACEMAN/PNG/rocket-spaceman_with-border.png"),
    s("SALTY", "SALTY/PNG/salty.png"),
    s("SICK_FLAME", "SICK/PNG/sick2_with_border.png"),
    s("SICK_SKULL", "SICK/PNG/with%20borderdark%20with%20frame.png"),
    s("SPHAGETTI_BATH", "SPHAGETTI%20BATH/PNG/sphagetti%20bath_with_frame.png"),
    s("THUG_LIFE", "THUG%20LIFE/PNG/thug_life_with_border_clean.png"),
    s("TRAP", "TRAP/PNG/trap.png"),
    s("TRASH", "TRASH/PNG/trash.png"),
    s("WHUUT", "WHUUT/PNG/whuut_with-frame.png"),
)

val PAID_GLOBAL_STICKERS = listOf(
    s("TIP_HAND_FLIP", "TIPS/png/tip_hand_flip_\$%20_with_border.png", 1),
    s("TIP_HAND_FLIP_COIN", "TIPS/png/tip_hand_flip_coin_with_border.png", 1),
    s("TIP_HAND_FLIP_LBC", "TIPS/png/tip_hand_flip_lbc_with_border.png", 1),
    s("COMET_TIP", "TIPS/png/\$%20comet%20tip%20with%20border.png", 5),
    s("SILVER_ODYSEE_COIN", "TIPS/png/with%20bordersilver_odysee_coinv.png", 5),
    s("LBC_COMET_TIP", "TIPS/png/LBC%20comet%20tip%20with%20border.png", 25),
    s("SMALL_TIP", "TIPS/png/with%20bordersmall\$_tip.png", 25),
    s("SMALL_LBC_TIP", "TIPS/png/with%20bordersmall_LBC_tip%20.png", 25),
    s("BITE_TIP", "TIPS/png/bite_\$tip_with%20border.png", 50),
    s("BITE_TIP_CLOSEUP", "TIPS/png/bite_\$tip_closeup.png", 50),
    s("BITE_LBC_CLOSEUP", "TIPS/png/LBC%20bite.png", 50),
    s("MEDIUM_TIP", "TIPS/png/with%20bordermedium\$_%20tip.png", 50),
    s("MEDIUM_LBC_TIP", "TIPS/png/with%20bordermedium_LBC_tip%20%20%20%20%20%20%20%20%20%20.png", 50),
    s("LARGE_TIP", "TIPS/png/with%20borderlarge\$tip.png", 100),
    s("LARGE_LBC_TIP", "TIPS/png/with%20borderlarge_LBC_tip%20.png", 100),
    s("BIG_TIP", "TIPS/png/with%20borderbig\$tip.png", 150),
    s("BIG_LBC_TIP", "TIPS/png/big_LBC_TIPV.png", 150),
    s("FORTUNE_CHEST", "TIPS/png/with%20borderfortunechest\$_tip.png", 200),
    s("FORTUNE_CHEST_LBC", "TIPS/png/with%20borderfortunechest_LBC_tip.png", 200),
)

private val STICKERS_BY_NAME = (FREE_GLOBAL_STICKERS + PAID_GLOBAL_STICKERS).associateBy { it.name }

fun StickerDef.toCommentToken(): String = "<stkr>$name<stkr>"

private val STICKER_TOKEN = Regex("<stkr>([^<]+)<stkr>")
private val EMOTE_TOKEN_RE = Regex(":[a-zA-Z0-9_]+:")
private val STICKERS_BY_NAME_LOWER = STICKERS_BY_NAME.mapKeys { it.key.lowercase() }

data class CommentSegment(val text: String?, val sticker: StickerDef?, val emote: EmoteDef? = null)

fun parseCommentSegments(body: String): List<CommentSegment> {
    val hasStkr = body.contains("<stkr>")
    val hasEmote = body.contains(":")
    if (!hasStkr && !hasEmote) return listOf(CommentSegment(text = body, sticker = null))

    val result = mutableListOf<CommentSegment>()
    var cursor = 0
    val stickerMatches = if (hasStkr) STICKER_TOKEN.findAll(body).toList() else emptyList()
    for (match in stickerMatches) {
        if (match.range.first > cursor) {
            val seg = body.substring(cursor, match.range.first)
            result.addAll(splitTextByEmotes(seg))
        }
        val rawName = match.groupValues[1]
        val def = STICKERS_BY_NAME[rawName]
            ?: STICKERS_BY_NAME_LOWER[rawName.lowercase()]
        if (def != null) {
            result.add(CommentSegment(text = null, sticker = def))
        }
        cursor = match.range.last + 1
    }
    if (cursor < body.length) {
        result.addAll(splitTextByEmotes(body.substring(cursor)))
    }
    return result
}

private fun splitTextByEmotes(text: String): List<CommentSegment> {
    if (!text.contains(":")) return listOf(CommentSegment(text = text, sticker = null))
    val out = mutableListOf<CommentSegment>()
    var cursor = 0
    for (m in EMOTE_TOKEN_RE.findAll(text)) {
        val def = emoteByName(m.value) ?: continue
        if (m.range.first > cursor) {
            out.add(CommentSegment(text = text.substring(cursor, m.range.first), sticker = null))
        }
        out.add(CommentSegment(text = null, sticker = null, emote = def))
        cursor = m.range.last + 1
    }
    if (cursor < text.length) out.add(CommentSegment(text = text.substring(cursor), sticker = null))
    return out.ifEmpty { listOf(CommentSegment(text = text, sticker = null)) }
}
