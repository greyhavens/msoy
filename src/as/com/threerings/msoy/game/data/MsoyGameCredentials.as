//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Name;
import com.threerings.util.StringBuilder;

import com.threerings.presents.net.Credentials;

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Used to authenticate with an MSOY Game server.
 */
public class MsoyGameCredentials extends Credentials
{
    /** A session token that identifies this user. */
    public var sessionToken :String;

    /** The unique tracking id for this client, if one is assigned */
    public var visitorId :String;

    public function MsoyGameCredentials (name :Name = null)
    {
        super(name);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sessionToken = (ins.readField(String) as String);
        visitorId = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(sessionToken);
        out.writeField(visitorId);
    }

    // from Credentials
    override protected function toStringBuf (buf :StringBuilder) :void
    {
        super.toStringBuf(buf);
        buf.append(", token=", sessionToken);
        buf.append(", visitorId=", visitorId);
    }
}
}
