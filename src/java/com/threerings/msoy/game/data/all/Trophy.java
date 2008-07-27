//
// $Id$

package com.threerings.msoy.game.data.all;

import com.threerings.io.SimpleStreamableObject;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains information on a trophy held by a player.
 */
public class Trophy extends SimpleStreamableObject
    implements IsSerializable
{
    /** The game for which this trophy was awarded. */
    public int gameId;

    /** The name of the trophy. */
    public String name;

    /** The description of how to earn this trophy (not always available). */
    public String description;

    /** The media for the trophy image. */
    public MediaDesc trophyMedia;

    /** When this trophy was earned (not always available). */
    public Long whenEarned;
}
