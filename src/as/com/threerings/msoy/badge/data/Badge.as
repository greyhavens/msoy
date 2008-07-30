package com.threerings.msoy.badge.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.client.DeploymentConfig;

public class Badge extends SimpleStreamableObject
    implements DSet_Entry
{
    /** The code that uniquely identifies this badge type. */
    public var badgeCode :int;

    public function getKey () :Object
    {
        return badgeCode;
    }

    /**
     * Returns the public access image URL for this badge.
     */
    public function imageUrl () :String
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + badgeCode.toString(16) +
            BADGE_IMAGE_TYPE;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        badgeCode = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(badgeCode);
    }

    protected static const BADGE_IMAGE_DIR :String = "badge/";
    protected static const BADGE_IMAGE_TYPE :String = ".png";
}
}
