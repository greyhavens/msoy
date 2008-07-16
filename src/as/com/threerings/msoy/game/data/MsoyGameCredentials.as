//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Name;
import com.threerings.util.StringBuilder;

import com.threerings.presents.net.Credentials;

import com.threerings.msoy.data.all.ReferralInfo;

/**
 * Used to authenticate with an MSOY Game server.
 */
public class MsoyGameCredentials extends Credentials
{
    /** A session token that identifies this user. */
    public var sessionToken :String;

    /** Referral info data. */
    public var referral :ReferralInfo;

    public function MsoyGameCredentials (name :Name = null)
    {
        super(name);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sessionToken = (ins.readField(String) as String);
        referral = ins.readObject() as ReferralInfo;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(sessionToken);
        out.writeObject(referral);
    }

    // from Credentials
    override protected function toStringBuf (buf :StringBuilder) :void
    {
        super.toStringBuf(buf);
        buf.append(", token=", sessionToken);
        buf.append(", referral=").append(referral);
    }
}
}
