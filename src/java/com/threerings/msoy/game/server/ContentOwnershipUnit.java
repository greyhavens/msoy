//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.CountHashMap;

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
        Iterables.addAll(_content, summarize(GameData.LEVEL_DATA, lrecords));
        Iterables.addAll(_content, summarize(GameData.ITEM_DATA, irecords));
        List<String> trophies = getTrophyRepo().loadTrophyOwnership(_gameId, _memberId);
        Iterables.addAll(_content,
            Iterables.transform(trophies, new Function<String, GameContentOwnership>() {
                public GameContentOwnership apply (String trophy) {
                    return new GameContentOwnership(_gameId, GameData.TROPHY_DATA, trophy);
                }
            }));
    }

    protected Iterable<GameContentOwnership> summarize (
        final byte type, Iterable<? extends SubItemRecord> records)
    {
        CountHashMap<String> counts = new CountHashMap<String>();
        for (SubItemRecord rec : records) {
            counts.incrementCount(rec.ident, 1);
        }
        return Iterables.transform(counts.countEntrySet(),
            new Function<CountHashMap.Entry<String>, GameContentOwnership>() {
            public GameContentOwnership apply (CountHashMap.Entry<String> entry) {
                return new GameContentOwnership(_gameId, type, entry.getKey(), entry.getCount());
            }
        });
    }

    protected abstract ItemPackRepository getIpackRepo ();
    protected abstract LevelPackRepository getLpackRepo ();
    protected abstract TrophyRepository getTrophyRepo ();

    protected int _gameId;
    protected int _suiteId;
    protected int _memberId;
    protected List<GameContentOwnership> _content;
}
