//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.parlor.turn.server.TurnGameManagerDelegate;

/**
 * A special turn delegate for flash games.
 */
public class FlashGameTurnDelegate extends TurnGameManagerDelegate
{
    public FlashGameTurnDelegate (FlashGameManager mgr)
    {
        super(mgr);
    }

    /**
     * A form of endTurn where you can specify the next turn holder index.
     */
    public void endTurn (int playerIndex)
    {
        _nextPlayerIndex = playerIndex;
        endTurn();
    }

    @Override
    protected void setNextTurnHolder ()
    {
        // if the user-supplied value seems to make sense, use it!
        if ((_nextPlayerIndex >= 0) &&
                (_nextPlayerIndex < _turnGame.getPlayers().length)) {
            _turnIdx = _nextPlayerIndex;

        } else {
            // otherwise, do the default behavior
            super.setNextTurnHolder();
        }

        // always clear out the override
        _nextPlayerIndex = -1;
    }

    /** An override next turn holder, or -1. */
    protected int _nextPlayerIndex = -1;
}
