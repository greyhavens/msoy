//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.item.server.persist.GameDetailRecord;

/**
 * Contains the gobs of game metadata that we load when a lobby or an AVRG is resolved and
 * pass along to games once they are created.
 */
public class GameContent
{
    public Game game;

    public GameDetailRecord detail;

    public ArrayList<LevelPack> lpacks = new ArrayList<LevelPack>();

    public ArrayList<ItemPack> ipacks = new ArrayList<ItemPack>();

    public ArrayList<TrophySource> tsources = new ArrayList<TrophySource>();

    public ArrayList<Prize> prizes = new ArrayList<Prize>();
}
