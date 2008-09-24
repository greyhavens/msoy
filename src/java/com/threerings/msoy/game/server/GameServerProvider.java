//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.stats.data.StatModifier;

/**
 * Defines the server-side of the {@link GameServerService}.
 */
public interface GameServerProvider extends InvocationProvider
{
    /**
     * Handles a {@link GameServerService#awardCoins} request.
     */
    void awardCoins (ClientObject caller, int arg1, UserAction arg2, int arg3);

    /**
     * Handles a {@link GameServerService#awardPrize} request.
     */
    void awardPrize (ClientObject caller, int arg1, int arg2, String arg3, Prize arg4, InvocationService.ResultListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GameServerService#clearGameHost} request.
     */
    void clearGameHost (ClientObject caller, int arg1, int arg2);

    /**
     * Handles a {@link GameServerService#leaveAVRGame} request.
     */
    void leaveAVRGame (ClientObject caller, int arg1);

    /**
     * Handles a {@link GameServerService#reportCoinAward} request.
     */
    void reportCoinAward (ClientObject caller, int arg1, int arg2);

    /**
     * Handles a {@link GameServerService#reportTrophyAward} request.
     */
    void reportTrophyAward (ClientObject caller, int arg1, String arg2, Trophy arg3);

    /**
     * Handles a {@link GameServerService#sayHello} request.
     */
    void sayHello (ClientObject caller, int arg1);

    /**
     * Handles a {@link GameServerService#updatePlayer} request.
     */
    void updatePlayer (ClientObject caller, int arg1, GameSummary arg2);

    /**
     * Handles a {@link GameServerService#updateStat} request.
     */
    void updateStat (ClientObject caller, int arg1, StatModifier<?> arg2);
}
