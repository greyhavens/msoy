//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectOutputStream;

import com.threerings.util.Joiner;
import com.threerings.util.Name;

/**
 * Used to authenticate a world session.
 */
public class WorldCredentials extends MsoyCredentials
{
    /** The machine identifier of the client, if one is known. */
    public var ident :String;

    /** Indicates whether this client is set up as a featured place view. */
    public var featuredPlaceView :Boolean;

    /**
     * Creates credentials with the specified username and password. The other public fields should
     * be set before logging in.
     */
    public function WorldCredentials (username :Name, password :String)
    {
        super(username);
        _password = password;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(ident);
        out.writeBoolean(featuredPlaceView);
        out.writeField(_password);
    }

    // documentation inherited
    override protected function toStringJoiner (j :Joiner) :void
    {
        super.toStringJoiner(j);
        j.add("password", _password);
    }

    /** Our encrypted password, if one was provided. */
    protected var _password :String;
}
}
