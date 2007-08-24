//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * Provides a special media descriptor for snapshot media, which follow a different
 * naming convention than other uploaded media.
 */
public class SnapshotMediaDesc extends MediaDesc
{
    /** SceneId of this snapshot. */
    public var sceneId :int;

    /** Generates a snapshot media path for the given scene id. */
    public static function getMediaPath (sceneId :int, mimeType :int) :String
    {
        return DeploymentConfig.mediaURL + sceneToName(sceneId) + mimeTypeToSuffix(mimeType);
    }

    /** Generates a snapshot media filename root, sans path or extension. */
    public static function sceneToName (sceneId :int) :String
    {
        return "s" + String(sceneId);
    }

    /**
     * Constructor
     */
    public function SnapshotMediaDesc (mimeType :int = 0, sceneId :int = 0)
    {
        super(null, mimeType);
        this.sceneId = sceneId;
    }

    // from MediaDesc
    override public String getMediaPath ()
    {
        return getMediaPath(sceneId, mimeType);
    }

    // from Hashable 
    override public function hashCode () :int
    {
        return sceneId;
    }

    // from Hashable 
    override public function equals (other :Object) :Boolean
    {
        if (other is SnapshotMediaDesc) {
            var that :SnapshotMediaDesc = (other as SnapshotMediaDesc);
            return (this.mimeType == that.mimeType) && (this.sceneId == that.sceneId);
        }
        return false;
    }

    // from Object
    override public function toString () :String
    {
        return sceneToName(sceneId) + mimeTypeToSuffix(mimeType);
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sceneId = ins.readInt();
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(sceneId);
    }
}
}
