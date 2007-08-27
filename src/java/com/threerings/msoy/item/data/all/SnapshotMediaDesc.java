//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.web.client.DeploymentConfig;

/**
 * Provides a special media descriptor for snapshot media, which follow a different
 * naming convention than other uploaded media.
 */
public class SnapshotMediaDesc extends MediaDesc
{
    /** This is where snapshots are stored relative to the media directory. */
    public static final String SNAPSHOT_DIRECTORY = "snapshot";
    
    /** SceneId of this snapshot. */
    public int sceneId;

    /** Generates a snapshot media path for the given scene id. */
    public static String getMediaPath (int sceneId, byte mimeType, boolean proxy)
    {
        String prefix = proxy ? DeploymentConfig.PROXY_PREFIX : DeploymentConfig.mediaURL;
        return prefix + SNAPSHOT_DIRECTORY + "/" +
            sceneToName(sceneId) + mimeTypeToSuffix(mimeType);
    }

    /** Generates a snapshot media filename root, sans path or extension. */
    public static String sceneToName (int sceneId)
    {
        return Integer.toString(sceneId);
    }
    
    /** Used for deserialization. */
    public SnapshotMediaDesc ()
    {
    }

    /**
     * Creates a configured media descriptor.
     */
    public SnapshotMediaDesc (byte mimeType, int sceneId)
    {
        super((byte[])null, mimeType, NOT_CONSTRAINED);
        this.sceneId = sceneId;
    }

    // @Override // from MediaDesc
    public String getMediaPath ()
    {
        return getMediaPath(sceneId, mimeType, false);
    }

    // @Override // from MediaDesc
    public String getProxyMediaPath ()
    {
        return getMediaPath(sceneId, mimeType, true);
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof SnapshotMediaDesc) {
            SnapshotMediaDesc that = (SnapshotMediaDesc) other;
            return this.mimeType == that.mimeType &&
                this.constraint == that.constraint &&
                this.sceneId == that.sceneId;
        }
        return false;
    }

    // @Override // from Object
    public int hashCode ()
    {
        return sceneId;
    }

    // @Override // from Object
    public String toString ()
    {
        return sceneToName(sceneId) + mimeTypeToSuffix(mimeType);
    }
}
