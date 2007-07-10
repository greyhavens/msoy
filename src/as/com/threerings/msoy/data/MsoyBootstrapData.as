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
    // nada, at the moment

    public function MsoyBootstrapData ()
    {
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
    }
}
}
