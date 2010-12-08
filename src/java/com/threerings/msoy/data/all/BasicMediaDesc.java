//
// $Id: $

package com.threerings.msoy.data.all;

/**
 * A somewhat more concrete {@link MediaDesc}.
 */
public abstract class BasicMediaDesc extends MediaDescImpl
{
    protected BasicMediaDesc ()
    {
    }

    protected BasicMediaDesc (byte mimeType, byte constraint)
    {
        _mimeType = mimeType;
        _constraint = constraint;
    }

    public void setConstraint (byte constraint)
    {
        _constraint = constraint;
    }

    @Override public byte getMimeType ()
    {
        return _mimeType;
    }

    @Override public byte getConstraint ()
    {
        return _constraint;
    }

    protected byte _mimeType;

    protected byte _constraint;
}
