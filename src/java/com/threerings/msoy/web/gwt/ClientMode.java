//
// #Id$

package com.threerings.msoy.web.gwt;

import com.samskivert.util.ByteEnum;

public enum ClientMode
    implements ByteEnum
{
    UNSPECIFIED(0, ""),
    FB_GAMES(1, "g"),
    FB_ROOMS(2, "r");

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

    ClientMode (int value, String code) {
        _value = (byte)value;
        this.code = code;
    }

    protected byte _value;
}
