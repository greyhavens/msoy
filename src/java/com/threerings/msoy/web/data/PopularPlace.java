//
// $Id$

package com.threerings.msoy.web.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Represent a single place in the top list of populated places.
 */
public abstract class PopularPlace extends SimpleStreamableObject
{
    public static abstract class PopularScenePlace extends PopularPlace
    {
        /** The id of the primary scene we represent. */
        public int sceneId;
        /** The population in that particular scene. */
        public int scenePop;
    }

    public static class PopularMemberPlace extends PopularScenePlace
    {
        /** The id of the member's scene we represent. */
        public int memberId;
    }

    public static class PopularGamePlace extends PopularPlace
    {
        /** The id of the game of the lobby we represent. */
        public int gameId;
    }

    public static class PopularGroupPlace extends PopularScenePlace
    {
        /** The id of the group's scene we represent. */
        public int groupId;
    }

    /** A human-readable name for this place. */
    public String name;
    /** The number of people who are in this place. */
    public int population;
}