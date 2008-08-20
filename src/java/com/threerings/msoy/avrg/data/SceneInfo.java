//
// $Id$

package com.threerings.msoy.avrg.data;

import com.google.common.collect.Comparators;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Helps an AVRG keep track of which world server is currently hosting a given scene.
 */
public class SceneInfo extends SimpleStreamableObject
    implements DSet.Entry, Comparable<SceneInfo>
{
    /** The id of the scene occupied by one or more players in our AVRG. */
    public int sceneId;

    /** The hostname of the world server hosting this scene. */
    public String hostname;

    /** The port on which to connect to said world server. */
    public int port;

    public SceneInfo ()
    {
    }

    public SceneInfo (int sceneId, String hostname, int port)
    {
        this.sceneId = sceneId;
        this.hostname = hostname;
        this.port = port;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return sceneId;
    }

    // from interface Comparable<SceneInfo>
    public int compareTo (SceneInfo other)
    {
        return Comparators.compare(sceneId, other.sceneId);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other == null || !other.getClass().equals(getClass())) {
            return false;
        }
        SceneInfo info = (SceneInfo) other;
        return sceneId == info.sceneId && hostname.equals(info.hostname) && port == info.port;
    }

    @Override // from Object
    public int hashCode ()
    {
        return sceneId;
    }
}
