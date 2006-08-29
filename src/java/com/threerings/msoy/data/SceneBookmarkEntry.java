//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.dobj.DSet;

/**
 * Does something extraordinary.
 */
public class SceneBookmarkEntry
    implements Comparable, DSet.Entry
{
    /** The scene id being represented. */
    public int sceneId;

    /** The name of the scene. */
    public String sceneName;

    /** The time of the last visit. */
    public double lastVisit;

    /** An ordering id that is used in relation to the other bookmarks. */
    public short orderingId;

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
    public Comparable getKey ()
    {
        return orderingId;
    }

    // from Comparable
    public int compareTo (Object other)
    {
        SceneBookmarkEntry that = (SceneBookmarkEntry) other;
        return this.orderingId - that.orderingId;
    }

    @Override
    public boolean equals (Object other)
    {
        return (other instanceof SceneBookmarkEntry) &&
            ((SceneBookmarkEntry) other).sceneId == this.sceneId;
    }

    @Override
    public int hashCode ()
    {
        return sceneId;
    }

    @Override
    public String toString ()
    {
        return sceneName;
    }
}
