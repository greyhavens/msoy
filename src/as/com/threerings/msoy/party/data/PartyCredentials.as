//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Name;
import com.threerings.util.StringBuilder;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Used to authenticate a party session.
 */
public class PartyCredentials extends MsoyCredentials
{
    /** The party that the authenticating user wishes to join. */
    public var partyId :int;

    public function PartyCredentials (username :Name)
    {
        super(username);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        partyId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(partyId);
    }

    // from Credentials
    override protected function toStringBuf (buf :StringBuilder) :void
    {
        super.toStringBuf(buf);
        buf.append(", partyId=", partyId);
    }
}
}
