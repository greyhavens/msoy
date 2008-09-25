//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Name;
import com.threerings.util.StringBuilder;

import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Contains extra information used during authentication with the game server.
 */
public class MsoyCredentials extends UsernamePasswordCreds
{
    /** A string prepended to session tokens that represent guest sessions. */
    public static const GUEST_SESSION_PREFIX :String = "G";

    /** A session token that identifies a user without requiring username or password. */
    public var sessionToken :String;

    /** The machine identifier of the client, if one is known. */
    public var ident :String;

    /** Indicates whether this client is set up as a featured place view. */
    public var featuredPlaceView :Boolean;

    /**
     * Creates credentials with the specified username and password.
     * {@link #ident} should be set before logging in unless the client does
     * not know its machine identifier in which case it should be left null.
     */
    public function MsoyCredentials (username :Name, password :String)
    {
        super(username, password);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sessionToken = (ins.readField(String) as String);
        ident = (ins.readField(String) as String);
        featuredPlaceView = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(sessionToken);
        out.writeField(ident);
        out.writeBoolean(featuredPlaceView);
    }

    // documentation inherited
    override protected function toStringBuf (buf :StringBuilder) :void
    {
        super.toStringBuf(buf);
        buf.append(", ident=").append(ident);
        buf.append(", token=").append(sessionToken);
        buf.append(", featuredPlaceView=").append(featuredPlaceView);
    }
}
}
