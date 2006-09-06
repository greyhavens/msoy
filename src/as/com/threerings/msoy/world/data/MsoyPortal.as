package com.threerings.msoy.world.data {

import com.threerings.io.Streamable;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.MediaData;

/**
 * Contains additional info about portals necessary for the msoy world.
 */
public class MsoyPortal extends Portal
{
    /** The media used to represent the portal. */
    public var media :MediaData;

    public var scaleX :Number = 1;

    public var scaleY :Number = 1;

    /**
     * @return true if the other portal is identical.
     */
    public function equivalent (that :MsoyPortal) :Boolean
    {
        return (this.portalId == that.portalId) &&
            (this.targetSceneId == that.targetSceneId) &&
            (this.targetPortalId == that.targetPortalId) &&
            this.loc.equals(that.loc) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY)
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(media);
        out.writeFloat(scaleX);
        out.writeFloat(scaleY);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        media = (ins.readObject() as MediaData);
        scaleX = ins.readFloat();
        scaleY = ins.readFloat();
    }

    // documentation inherited
    override public function clone () :Object
    {
        var p :MsoyPortal = (super.clone() as MsoyPortal);
        p.media = media;
        p.scaleX = scaleX;
        p.scaleY = scaleY;
        return p;
    }
}
}
