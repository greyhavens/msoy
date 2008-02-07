//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information displayed in the arcade.
 */
public class ArcadeData
    implements IsSerializable
{
    /** Contains summary information on a particular game category. */
    public static class Category
        implements IsSerializable
    {
        /** The category code for this category. */
        public int category;

        /** The number of games in this category. */
        public int gameCount;

        /** The first game in this category. */
        public GameInfo game1;

        /** The second game in this category. */
        public GameInfo game2;
    }

    /** Info on the featured game. */
    public FeaturedGameInfo featuredGame;

    /** 
     * Information about each game category.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.ArcadeData.Category>
     */
    public List categories;

    /** 
     * Info for this player's favorite games or null if they're a guest or have none.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.GameInfo>
     */
    public List favorites;
}
