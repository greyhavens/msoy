//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Name;
import com.threerings.util.StringBuilder;

import com.threerings.presents.net.Credentials;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Contains information needed to authenticate with the MSOY server.
 */
public class MsoyCredentials extends Credentials
{
    /** A session token that identifies a user without requiring username or password. */
    public var sessionToken :String;

    /** The unique tracking id for this client, if one is assigned */
    public var visitorId :String;

    /** The affiliate id provided to the client via Flash parameters or 0. */
    public var affiliateId :int;

    /** The vector by which this client entered, or null. */
    public var vector :String;

    /**
     * Creates credentials with the specified username.
     */
    public function MsoyCredentials (username :Name)
    {
        _username = username;
    }

    public function getUsername () :Name
    {
        return _username;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(sessionToken);
        out.writeField(visitorId);
        out.writeInt(affiliateId);
        out.writeField(vector);
        out.writeObject(_username);
    }

    public function toString () :String
    {
        var buf :StringBuilder = new StringBuilder("[");
        toStringBuf(buf);
        return buf.append("]").toString();
    }

    protected function toStringBuf (buf :StringBuilder) :void
    {
        buf.append("username=").append(_username);
        buf.append(", token=").append(sessionToken);
        buf.append(", visitorId=").append(visitorId);
        buf.append(", affiliateId=").append(affiliateId);
        buf.append(", vector=").append(vector);
    }

    protected var _username :Name;
}
}
