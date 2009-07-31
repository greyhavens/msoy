//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.server.GameUtil;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.GameMetricsRecord;

/**
 * Contains the gobs of game metadata that we load when a lobby or an AVRG is resolved and pass
 * along to games once they are created.
 */
public class GameContent
{
    public int gameId;
    public boolean isApproved;
    public GameInfoRecord game;
    public FacebookInfo facebook;
    public GameMetricsRecord metrics;
    public GameCode code;
    public List<LevelPack> lpacks = Lists.newArrayList();
    public List<ItemPack> ipacks = Lists.newArrayList();
    public List<TrophySource> tsources = Lists.newArrayList();
    public List<Prize> prizes = Lists.newArrayList();

    public boolean isDevelopmentVersion ()
    {
        return GameUtil.isDevelopmentVersion(gameId);
    }

    public GameSummary toGameSummary ()
    {
        return new GameSummary(
            gameId, game.name, game.description, game.isAVRG, game.getThumbMedia(), game.creatorId);
    }
}
