//
// $Id$

package com.threerings.msoy.swiftly.client.model;

/**
 * Uniquely identifies RPC calls in the models.
 */
public class RequestId
{
    public final int id;

    public RequestId (int id)
    {
        this.id = id;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (!(other instanceof RequestId)) {
            return false;
        }

        return id == ((RequestId)other).id;
    }

    @Override // from Object
    public int hashCode ()
    {
        return id;
    }
}
