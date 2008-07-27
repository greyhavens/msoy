//
// $Id$

package com.threerings.msoy.game.gwt;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Extends {@link GameInfo} with information on a featured game.
 */
public class FeaturedGameInfo extends GameInfo
{
    /** The screenshot media. */
    public MediaDesc shotMedia;

    /** The name and id of the game's creator. */
    public MemberName creator;

    /** The minimum number of players allowed by the game. */
    public int minPlayers;

    /** The maximum number of players allowed by the game (Integer.MAX_VALUE for party games). */
    public int maxPlayers;

    /** The average game duration in seconds. */
    public int avgDuration;

    /**
     * Returns the media to be shown for this game's screenshot.
     */
    public MediaDesc getShotMedia ()
    {
        return (shotMedia != null) ? shotMedia : getThumbMedia();
    }
}
