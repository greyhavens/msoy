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
    /** A string prepended to session tokens that represent guest sessions. */
    public static const GUEST_SESSION_PREFIX :String = "G";

    /** A session token that identifies a user without requiring username or password. */
    public var sessionToken :String;

    /**
     * Creates credentials with the specified username.
     */
    public function MsoyCredentials (username :Name)
    {
        super(username);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sessionToken = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(sessionToken);
    }

    // documentation inherited
    override protected function toStringBuf (buf :StringBuilder) :void
    {
        super.toStringBuf(buf);
        buf.append(", token=").append(sessionToken);
    }
}
}
