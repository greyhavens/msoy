//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on a trophy held by a player.
 */
public class Trophy extends SimpleStreamableObject
{
    /** The name of the trophy. */
    public String name;

    /** The media for the trophy image. */
    public MediaDesc trophyMedia;
}
