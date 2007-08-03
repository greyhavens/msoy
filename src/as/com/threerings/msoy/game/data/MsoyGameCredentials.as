//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Name;

import com.threerings.presents.net.Credentials;

/**
 * Used to authenticate with an MSOY Game server.
 */
public class MsoyGameCredentials extends Credentials
{
    /** A session token that identifies this user. */
    public var sessionToken :String;

    public function MsoyGameCredentials ()
    {
        super(new Name(""));
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
}
}
