//
// $Id$

package com.threerings.msoy.swiftly.data;


/** A compiler message. */
public interface CompilerOutput
{
    /** Message severity levels. */
    public enum Level {
        /** Message should be ignored. */
        IGNORE,
        /** Informational message. */
        INFO,
        /** Warning message. */
        WARNING,
        /** Error message. */
        ERROR,
        /** Message level could not be determined. */
        UNKNOWN
    }

    /**
     * Returns the line number referenced, or -1 if no line number was specified.
     */
    public int getLineNumber();

    /**
     * Returns the column referenced, or -1 if no column was specified.
     */
    public int getColumnNumber();

    /**
     * Returns the file name referenced, or null if no file was specified.
     */
    public String getFileName();

    /**
     * Returns the relative path of the file name referenced, or null if no file was specified.
     */
    public String getPath();

    /**
     * Returns true if this message has a path associated with it.
     */
    public boolean hasPath();

    /**
     * Returns the compiler's text message.
     */
    public String getMessage();

    /**
     * Returns the message's severity level.
     */
    public Level getLevel();
}
