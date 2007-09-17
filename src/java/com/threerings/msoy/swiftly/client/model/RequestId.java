//
// $Id$

package com.threerings.msoy.swiftly.client.model;

/**
 * Uniquely identifies RPC calls in the models.
 */
public class RequestId
{
    public RequestId (int id)
    {
        _id = id;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (!(other instanceof RequestId)) {
            return false;
        }

        return _id == ((RequestId)other)._id;
    }

    @Override // from Object
    public int hashCode ()
    {
        return _id;
    }

    private final int _id;
}
