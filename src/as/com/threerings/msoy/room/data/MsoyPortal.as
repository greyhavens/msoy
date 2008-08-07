//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.whirled.spot.data.Portal;

/**
 * In Whirled, portals include the location in the destination scene at which to arrive rather than
 * requiring that portals be bound to another portal in the target room.
 */
public class MsoyPortal extends Portal
{
    /** The location at which to arrive in the target scene. May be null in which case the body is
     * placed at the scene's default entrance. */
    public var dest :MsoyLocation;

    /**
     * Constructs a portal from the supplied furni data record.
     */
    public function MsoyPortal (furni :FurniData = null)
    {
        if (furni == null) {
            return;
        }

        var vals :Array = furni.splitActionData();
        portalId = furni.id;
        loc = furni.loc;
        targetSceneId = int(vals[0]);
        targetPortalId = -1;

        // parse our destination location if we have one
        if (vals.length > 5) {
            dest = new MsoyLocation();
            dest.x = Number(vals[1]);
            dest.y = Number(vals[2]);
            dest.z = Number(vals[3]);
            dest.orient = int(vals[4]);
        }
    }

    // documentation inherited from interface Cloneable
    override public function clone () :Object
    {
        var p :MsoyPortal = (super.clone() as MsoyPortal);
        p.dest = dest;
        return p;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        dest = (ins.readObject() as MsoyLocation);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(dest);
    }
}
}
