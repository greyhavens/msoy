package com.threerings.msoy.badge.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Long;
import com.threerings.msoy.client.DeploymentConfig;

/**
 * Contains information on a badge earned by the player.
 */
public class EarnedBadge extends Badge
{
    /** When this badge was earned. */
    public var whenEarned :Long;

    // from Badge
    override public function get imageUrl () :String
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR +
            uint(badgeCode).toString(16) + "_" + level + "f" + BADGE_IMAGE_TYPE;
    }

    // from Object
    override public function toString () :String
    {
        return "EarnedBadge [code=" + badgeCode + ", level=" + level + ", whenEarned=" +
            whenEarned + ", coinValue=" + coinValue + "]";
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        whenEarned = (ins.readField(Long) as Long);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(whenEarned);
    }
}

}
