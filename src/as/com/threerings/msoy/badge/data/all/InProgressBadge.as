package com.threerings.msoy.badge.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.msoy.client.DeploymentConfig;

/**
 * Contains information on a badge that a player is working towards.
 */
public class InProgressBadge extends Badge
{
    /** The progress that has been made on this badge. */
    public var progress :Number;

    // from Badge
    override public function get imageUrl () :String
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR +
            uint(badgeCode).toString(16) + "_" +
            (level > 0 ? (level -1) + "f" : level + "e") + BADGE_IMAGE_TYPE;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        progress = ins.readFloat();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeFloat(progress);
    }
}

}
