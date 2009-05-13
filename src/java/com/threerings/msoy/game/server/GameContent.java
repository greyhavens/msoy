//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;

import com.threerings.msoy.game.gwt.GameCode;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.GameMetricsRecord;

/**
 * Contains the gobs of game metadata that we load when a lobby or an AVRG is resolved and pass
 * along to games once they are created.
 */
public class GameContent
{
    public boolean isDevelopmentVersion;

    public int suiteId;

    public GameInfoRecord game;

    public GameMetricsRecord metrics;

    public GameCode code;

    public List<LevelPack> lpacks = Lists.newArrayList();

    public List<ItemPack> ipacks = Lists.newArrayList();

    public List<TrophySource> tsources = Lists.newArrayList();

    public List<Prize> prizes = Lists.newArrayList();
}
