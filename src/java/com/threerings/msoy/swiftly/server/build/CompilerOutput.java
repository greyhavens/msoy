//
// $Id$

package com.threerings.msoy.swiftly.server.build;

/** A compiler message. */
public interface CompilerOutput {
    /** Message severity levels. */
    public enum Level {
        /** Informational message. */
        INFO,
        /** Warning message. */
        WARNING,
        /** Error message. */
        ERROR,
        /** Mesage level could not be determined. */
        UNKNOWN
    }

    /**
     * Returns the line number referenced, or -1 if no line number was specified.
     */
    public int getLineNumber();

    /**
     * Returns the file name referenced, or null if no file was specified.
     */
    public String getFileName();

    /**
     * Returns the compiler's text message.
     */
    public String getMessage();

    /**
     * Returns the message's severity level.
     */
    public Level getLevel();
}
