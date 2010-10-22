//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.orth.data.MediaDesc;

public interface MsoyGameConfig
{
    /** Returns the id of this game. */
    function getGameId () :int;

    /** Returns the name of this game. */
    function getName () :String;

    /** Returns the description of this game. */
    function getTextDescription () :String;

    /** Returns the thumbnail for this game. */
    function getThumbnail () :MediaDesc;

    /** Returns the splash screen for this game. */
    function getSplash () :MediaDesc;
}
}
