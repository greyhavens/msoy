//
// $Id$

package com.threerings.msoy.web.gwt;

import com.samskivert.util.ByteEnum;

/**
 * Represents an external authentication source.
 */
public enum ExternalAuther
    implements ByteEnum
{
    FACEBOOK(1),
    OPEN_SOCIAL(2),
    OPEN_ID(3);

    // from ByteEnum
    public byte toByte () {
        return _code;
    }

    ExternalAuther (int code) {
        if (code < Byte.MIN_VALUE || code > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Code out of byte range: " + code);
        }
        _code = (byte)code;
    }

    protected transient byte _code;
}
