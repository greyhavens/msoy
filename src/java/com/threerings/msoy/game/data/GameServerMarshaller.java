//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.stats.data.StatModifier;

/**
 * Provides the implementation of the {@link GameServerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GameServerMarshaller extends InvocationMarshaller
    implements GameServerService
{
    /** The method id used to dispatch {@link #awardCoins} requests. */
    public static final int AWARD_COINS = 1;

    // from interface GameServerService
    public void awardCoins (Client arg1, int arg2, int arg3, String arg4, int arg5, int arg6, UserAction arg7)
    {
        sendRequest(arg1, AWARD_COINS, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4, Integer.valueOf(arg5), Integer.valueOf(arg6), arg7
        });
    }

    /** The method id used to dispatch {@link #awardPrize} requests. */
    public static final int AWARD_PRIZE = 2;

    // from interface GameServerService
    public void awardPrize (Client arg1, int arg2, int arg3, String arg4, Prize arg5, InvocationService.ResultListener arg6)
    {
        InvocationMarshaller.ResultMarshaller listener6 = new InvocationMarshaller.ResultMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, AWARD_PRIZE, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4, arg5, listener6
        });
    }

    /** The method id used to dispatch {@link #clearGameHost} requests. */
    public static final int CLEAR_GAME_HOST = 3;

    // from interface GameServerService
    public void clearGameHost (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, CLEAR_GAME_HOST, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #leaveAVRGame} requests. */
    public static final int LEAVE_AVRGAME = 4;

    // from interface GameServerService
    public void leaveAVRGame (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_AVRGAME, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #reportCoinAward} requests. */
    public static final int REPORT_COIN_AWARD = 5;

    // from interface GameServerService
    public void reportCoinAward (Client arg1, int arg2, int arg3)
    {
        sendRequest(arg1, REPORT_COIN_AWARD, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #reportTrophyAward} requests. */
    public static final int REPORT_TROPHY_AWARD = 6;

    // from interface GameServerService
    public void reportTrophyAward (Client arg1, int arg2, String arg3, Trophy arg4)
    {
        sendRequest(arg1, REPORT_TROPHY_AWARD, new Object[] {
            Integer.valueOf(arg2), arg3, arg4
        });
    }

    /** The method id used to dispatch {@link #sayHello} requests. */
    public static final int SAY_HELLO = 7;

    // from interface GameServerService
    public void sayHello (Client arg1, int arg2)
    {
        sendRequest(arg1, SAY_HELLO, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #updatePlayer} requests. */
    public static final int UPDATE_PLAYER = 8;

    // from interface GameServerService
    public void updatePlayer (Client arg1, int arg2, GameSummary arg3)
    {
        sendRequest(arg1, UPDATE_PLAYER, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #updateStat} requests. */
    public static final int UPDATE_STAT = 9;

    // from interface GameServerService
    public void updateStat (Client arg1, int arg2, StatModifier<?> arg3)
    {
        sendRequest(arg1, UPDATE_STAT, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }
}
