//
// #Id$

package com.threerings.msoy.web.gwt;

import com.samskivert.util.ByteEnum;

public enum ClientMode
    implements ByteEnum
{
    UNSPECIFIED(0, ""),
    FB_GAMES(1, "g"),
    FB_ROOMS(2, "r"),
    WHIRLED_DJ(3, "dj");

    public static ClientMode fromCode (String code)
    {
        for (ClientMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return UNSPECIFIED;
    }

    /** The code to pass down to the client in its token. */
    public String code;

    @Override // from ByteEnum
    public byte toByte ()
    {
        return _value;
    }

    /**
     * Detects if this client mode is within a facebook iframe.
     */
    public boolean isFacebook ()
    {
        return this == FB_GAMES || this == FB_ROOMS;
    }

    /**
     * Detects if this client mode is the facebook games portal.
     */
    public boolean isFacebookGames ()
    {
        return this == FB_GAMES;
    }

    /**
     * Detects if this client mode is the facebook rooms portal.
     */
    public boolean isFacebookRooms ()
    {
        return this == FB_ROOMS;
    }

    /**
     * Detects if this client mode should only show one thing at a time. That is, either content
     * or client, not both.
     */
    public boolean isMonoscreen ()
    {
        return this == FB_GAMES;
    }

    /**
     * Whether we should hide room editting features and various other things.
     */
    public boolean isMinimal ()
    {
        return this == WHIRLED_DJ;
    }

    ClientMode (int value, String code) {
        _value = (byte)value;
        this.code = code;
    }

    protected byte _value;
}
