//
// $Id$

package com.threerings.msoy.data {

import com.threerings.presents.net.BootstrapData;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Msoy bootstrap data.
 */
public class MsoyBootstrapData extends BootstrapData
{
    /** Oid of the chat room. */
    public var chatOid :int;

    public function MsoyBootstrapData ()
    {
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        chatOid = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(chatOid);
    }
}
}
