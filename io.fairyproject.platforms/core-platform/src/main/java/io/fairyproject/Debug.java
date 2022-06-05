package io.fairyproject;

import io.fairyproject.container.ContainerContext;
import io.fairyproject.util.SimpleTiming;
import io.fairyproject.util.Stacktrace;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@UtilityClass
public class Debug {

    // Is fairy framework currently running under unit testing?
    public boolean UNIT_TEST = false;

    // the boolean to determine if you are in Fairy Project IDE
    public boolean IN_FAIRY_IDE = Boolean.getBoolean("fairy.project-ide");

    public static boolean SHOW_LOGS = Boolean.getBoolean("fairy.showlog");

    // the boolean to determine if you are in the project IDE that develops based on Fairy
    public boolean IN_DEV_IDE = false;

    // the runnable to let you setup break point in IDE
    public Runnable BREAKPOINT;

    /**
     * Logging
     */
    public static final Logger LOGGER = LogManager.getLogger(ContainerContext.class);
    public static void log(String msg, Object... replacement) {
        if (SHOW_LOGS) {
            LOGGER.info(String.format(msg, replacement));
        }
    }
    public static void warn(String msg, Object... replacement) {
        if (SHOW_LOGS) {
            LOGGER.warn(String.format(msg, replacement));
        }
    }
    public static void fatal(String msg, Throwable e, Object... replacement) {
        if (SHOW_LOGS) {
            LOGGER.fatal(String.format(msg, replacement), e);
        }
    }

    public static SimpleTiming logTiming(String msg) {
        return SimpleTiming.create(time -> log("Ended %s - took %d ms", msg, time));
    }

    public void doPause() {
        if (!Debug.isInIde()) {
            return;
        }

        System.err.println("Pausing...");

        if (BREAKPOINT == null) {
            System.err.println("You don't seems to have breakpoint setup!");
        } else {
            BREAKPOINT.run();
        }
    }

    public void logExceptionAndPause(Throwable throwable) {
        Stacktrace.print(throwable);
        Debug.doPause();
    }

    public boolean isInIde() {
        return IN_FAIRY_IDE || IN_DEV_IDE;
    }

}
