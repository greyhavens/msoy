//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.data.ClientObject;

/** Client object purely for distinguishing windows in service methods. */
public class WindowClientObject extends ClientObject
{
    /** The bureau id of the owner of this window. */
    public var bureauId :String;

    /** @inheritDoc */
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        bureauId = ins.readField(String) as String;
    }
}

}
