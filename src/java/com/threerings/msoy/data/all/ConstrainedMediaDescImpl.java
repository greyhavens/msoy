//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Contains information about a piece of media.
 */
public abstract class ConstrainedMediaDescImpl extends MediaDescImpl
    implements ConstrainedMediaDesc, IsSerializable, Streamable
{
    /** Used for deserialization. */
    public ConstrainedMediaDescImpl ()
    {
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public ConstrainedMediaDescImpl (byte mimeType, byte constraint)
    {
        super(mimeType);
        setConstraint(constraint);
    }

    @Override public String getProxyMediaPath ()
    {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override public byte getConstraint ()
    {
        return _constraint;
    }

    @Override
    public void setConstraint (byte constraint)
    {
        _constraint = constraint;
    }

    @Override // from MediaDescBase
    public int hashCode ()
    {
        return (43 * super.hashCode()) + getConstraint();
    }

    @Override // from MediaDescBase
    public boolean equals (Object other)
    {
        return super.equals(other) && (other instanceof ConstrainedMediaDescImpl) &&
            (this.getConstraint() == ((ConstrainedMediaDesc) other).getConstraint());
    }

    protected byte _constraint;
}
