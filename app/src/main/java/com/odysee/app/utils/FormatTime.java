package com.odysee.app.utils;

import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.Decade;

import java.util.Date;

/**
 * Modified from NewPipe <a href="https://github.com/TeamNewPipe/NewPipe/blob/dev/app/src/main/java/org/schabi/newpipe/util/Localization.java">Localization.java</a>
 */
public class FormatTime {
    public static PrettyTime prettyTime;

    private static void initPrettyTime() {
        prettyTime = new PrettyTime();
        // Do not use decades as Odysee doesn't either.
        prettyTime.removeUnit(Decade.class);
    }

    public static String fromEpochMillis(final long epochMillis) {
        if (prettyTime == null) {
            initPrettyTime();
        }
        return prettyTime.format(new Date(epochMillis));
    }
}
