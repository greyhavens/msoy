//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

public class GameSummary extends SimpleStreamableObject
    implements Cloneable
{
    /** The game item id */
    public int gameId;

    /** The thumbnail of the game - used as a game icon */
    public MediaDesc thumbMedia;

    /** The name of the game - used as a tooltip */
    public String name;

    public MediaDesc getThumbMedia ()
    {
        return thumbMedia != null ? thumbMedia : Item.getDefaultThumbnailMediaFor(Item.GAME);
    }

    // documentation inherited
    @Override
    public boolean equals (Object other)
    {
        if (other instanceof GameSummary) {
            GameSummary data = (GameSummary) other;
            return data.gameId == this.gameId;
        }
        return false;
    }

    // documentation inherited
    @Override
    public Object clone ()
    {
        try {
            GameSummary data = (GameSummary) super.clone();

            return data;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // not going to happen
        }
    }
}
