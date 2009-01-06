//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;

import com.threerings.msoy.game.server.persist.GameDetailRecord;

/**
 * Contains the gobs of game metadata that we load when a lobby or an AVRG is resolved and
 * pass along to games once they are created.
 */
public class GameContent
{
    public Game game;

    public GameDetailRecord detail;

    public List<LevelPack> lpacks = Lists.newArrayList();

    public List<ItemPack> ipacks = Lists.newArrayList();

    public List<TrophySource> tsources = Lists.newArrayList();

    public List<Prize> prizes = Lists.newArrayList();
}
