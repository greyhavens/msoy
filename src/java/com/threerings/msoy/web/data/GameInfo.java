//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains summary information for a game being displayed on the arcade page.
 */
public class GameInfo
    implements IsSerializable
{
    /** The unique identifier for this game. */
    public int gameId;

    /** The game's human readable name. */
    public String name;

    /** The genre code for this game. */
    public int genre;

    /** This game's thumbnail icon. */
    public MediaDesc thumbMedia;

    /** This game's description. */
    public String description;

    /** The number of players currently playing this game. */
    public int playersOnline;
}
