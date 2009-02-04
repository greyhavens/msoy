//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.samskivert.jdbc.RepositoryUnit;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.GameData;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.ItemPackRecord;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRecord;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;

import com.threerings.msoy.game.server.persist.TrophyRepository;

public abstract class ContentOwnershipUnit extends RepositoryUnit
{
    public ContentOwnershipUnit (int gameId, int suiteId, int memberId) {
        super("contentOwnershipResolution");

        _gameId = gameId;
        _suiteId = suiteId;
        _memberId = memberId;
    }

    @Override
    public void invokePersist() throws Exception
    {
        _content = Lists.newArrayList();
        Iterable<LevelPackRecord> lrecords;
        Iterable<ItemPackRecord> irecords;
        if (Game.isDevelopmentVersion(_gameId)) {
            // only the game creator will appear to "own" premium level packs (or any item
            // packs since all item packs are premium); however a crafty creator could
            // create extra item or premium level packs and give them to a tester and the
            // tester will then also appear to own said premium content
            lrecords = getLpackRepo().loadOriginalItems(_memberId, _suiteId);
            irecords = getIpackRepo().loadOriginalItems(_memberId, _suiteId);
            // filter out non-premium level packs (which will generally show up in the
            // creator's inventory) since those normally wouldn't be owned
            lrecords = Iterables.filter(lrecords, new Predicate<LevelPackRecord>() {
                public boolean apply (LevelPackRecord record) {
                    return record.premium;
                }
            });
        } else {
            lrecords = getLpackRepo().loadClonedItems(_memberId, _suiteId);
            irecords = getIpackRepo().loadClonedItems(_memberId, _suiteId);
        }
        Function<SubItemRecord, GameContentOwnership> createOwnership =
            new Function<SubItemRecord, GameContentOwnership>() {
                public GameContentOwnership apply (SubItemRecord rec) {
                    return new GameContentOwnership(_gameId,
                        rec instanceof LevelPackRecord ? GameData.LEVEL_DATA : GameData.ITEM_DATA,
                        rec.ident);
                }
            };
        Iterables.addAll(_content, Iterables.transform(lrecords, createOwnership));
        Iterables.addAll(_content, Iterables.transform(irecords, createOwnership));
        List<String> trophies = getTrophyRepo().loadTrophyOwnership(_gameId, _memberId);
        Iterables.addAll(_content,
            Iterables.transform(trophies, new Function<String, GameContentOwnership>() {
                public GameContentOwnership apply (String trophy) {
                    return new GameContentOwnership(_gameId, GameData.TROPHY_DATA, trophy);
                }
            }));
    }

    protected abstract ItemPackRepository getIpackRepo ();
    protected abstract LevelPackRepository getLpackRepo ();
    protected abstract TrophyRepository getTrophyRepo ();

    protected int _gameId;
    protected int _suiteId;
    protected int _memberId;
    protected List<GameContentOwnership> _content;
}
