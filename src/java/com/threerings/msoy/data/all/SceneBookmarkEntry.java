//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.presents.dobj.DSet;

/**
 * Does something extraordinary.
 */
public class SceneBookmarkEntry
    implements Comparable<SceneBookmarkEntry>, DSet.Entry, IsSerializable
{
    /** The scene id being represented. */
    public int sceneId;

    /** The name of the scene. */
    public String sceneName;

    /** The time of the last visit. */
    public double lastVisit;

    /** Suitable for deserialization. */
    public SceneBookmarkEntry ()
    {
    }

    public SceneBookmarkEntry (int sceneId, String sceneName, long visit)
    {
        this.sceneId = sceneId;
        this.sceneName = sceneName;
        lastVisit = visit;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return new Integer(sceneId);
    }

    // from Comparable
    public int compareTo (SceneBookmarkEntry that)
    {
        return this.sceneId - that.sceneId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof SceneBookmarkEntry) &&
            ((SceneBookmarkEntry) other).sceneId == this.sceneId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return sceneId;
    }

    @Override // from Object
    public String toString ()
    {
        return sceneName;
    }
}
