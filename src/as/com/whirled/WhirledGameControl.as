//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc.  Please do not redistribute.

package com.whirled {

import flash.display.DisplayObject;

import com.threerings.ezgame.EZGameControl;

/**
 * Adds whirled-specific controls to EZGameControl
 */
public class WhirledGameControl extends EZGameControl
{
    public function WhirledGameControl (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Get the amount of flow that's <i>currently</i> available to award to the occupant who has
     * instantiated this game control.  In other words, your game is only responsible for granting
     * flow to the occupantId returned by getMyId(). You should grant flow based on the performance
     * in the game, but can also grant flow to non-players.
     */
    public function getAvailableFlow () :int
    {
        return int(callEZCode("getAvailableFlow_v1"));
    }

    /**
     * Award flow to this occupant. See getAvailableFlow(). NOTE: awards must be completed before
     * the game is ended.
     */
    public function awardFlow (amount :int) :void
    {
        callEZCode("awardFlow_v1", amount);
    }

    /**
     * Enables or disables chat. When chat is disabled, it is not visible which is useful for games
     * in which the chat overlay obstructs the view during play.
     */
    public function setChatEnabled (enabled :Boolean) :void
    {
        callEZCode("setChatEnabled_v1", enabled);
    }
}
}
