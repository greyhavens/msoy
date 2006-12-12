//
// $Id$

package com.threerings.msoy.web.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Represent a single place in the top list of populated places.
 */
public abstract class PopularPlace extends SimpleStreamableObject
{
    public static class PopularScenePlace extends PopularPlace
    {
        /** The id of the scene we represent. */
        public int sceneId;

        public PopularScenePlace ()
        {
        }
        
        public PopularScenePlace (String name, int population, int sceneId)
        {
            super(name, population);
            this.sceneId = sceneId;
        }
    }

    public static class PopularGamePlace extends PopularPlace
    {
        /** The id of the game of the lobby we represent. */
        public int gameId;

        public PopularGamePlace ()
        {
        }
        
        public PopularGamePlace (String name, int population, int gameId)
        {
            super(name, population);
            this.gameId = gameId;
        }
        
    }

    /** A human-readable name for this place. */
    public String name;
    /** The number of people who are in this place. */
    public int population;

    public PopularPlace ()
    {
    }

    public PopularPlace (String name, int population)
    {
        this.name = name;
        this.population = population;
    }
}