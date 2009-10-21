//
// $Id: $

package com.threerings.msoy.group.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the definition of a Theme.
 */
public class Theme extends SimpleStreamableObject
    implements DSet_Entry
{
    /** The group id of this theme. */
    public var groupId :int;

    /** The media of the theme's Whirled logo replacement image. */
    public var logo :MediaDesc;

    /** Whether or not we start playing this group's associated AVRG upon room entry. */
    public var playOnEnter :Boolean;

    public function Theme ()
    {
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return groupId;
    }

    // from Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        groupId = ins.readInt();
        logo = MediaDesc(ins.readObject());
        playOnEnter = ins.readBoolean();
    }
}

}
