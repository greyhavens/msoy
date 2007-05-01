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
        return sceneId;
    }

    // from Comparable
    public int compareTo (Object other)
    {
        SceneBookmarkEntry that = (SceneBookmarkEntry) other;
        return this.sceneId - that.sceneId;
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
