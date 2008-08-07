//
// $Id$

package com.threerings.msoy.room.data;

public class EffectData extends FurniData
{
    /** The parameter should not be adjusted. */
    public static final byte MODE_NONE = 0;

    /** The parameter should be i18n translated. */
    public static final byte MODE_XLATE = 1;

    /** The layer upon which the effect should reside. @see RoomCodes. */
    public byte roomLayer;

    /**
     * Set the parameter.
     */
    public void setParameter (byte mode, String param)
    {
        actionType = mode;
        actionData = param;
    }
}
