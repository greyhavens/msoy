//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.*;

import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import com.threerings.msoy.item.data.all.Item;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link AvatarRecord} items.
 */
@Singleton
public class AvatarRepository extends ItemRepository<AvatarRecord>
{
    @Entity(name="AvatarTagRecord")
    public static class AvatarTagRecord extends TagRecord
    {
    }

    @Entity(name="AvatarTagHistoryRecord")
    public static class AvatarTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public AvatarRepository (PersistenceContext ctx)
    {
        super(ctx);

        registerMigration(new DataMigration("2009_02_13_fixUsedAvatars") {
            @Override public void invoke () throws DatabaseException {
                log.info("Free the avatars: starting...");
                try {
                    Set<Integer> ids = Sets.newHashSet();
                    List<Key<AvatarRecord>> origKeys = findAllKeys(AvatarRecord.class, true,
                        new Where(AvatarRecord.USED, Item.USED_AS_AVATAR));
                    for (Key<AvatarRecord> key : origKeys) {
                        ids.add((Integer) key.getValues()[0]);
                    }
                    origKeys = null; // aid gc
                    List<Key<AvatarCloneRecord>> cloneKeys = findAllKeys(AvatarCloneRecord.class,
                        true, new Where(AvatarCloneRecord.USED, Item.USED_AS_AVATAR));
                    for (Key<AvatarCloneRecord> key : cloneKeys) {
                        ids.add((Integer) key.getValues()[0]);
                    }
                    cloneKeys = null; // aid gc
                    log.info("Free the avatars: identified used", "count", ids.size());
                    _memberRepo.removeAllUsedAvatars(ids);
                    log.info("Free the avatars: filtered the really-used ones", "count", ids.size());
                    // break up the origs so we do no more than 5k at once
                    for (Iterable<Integer> subset : Iterables.partition(
                            Iterables.filter(ids, IS_ORIGINAL_ID), 5000, false)) {
                        List<Integer> subsetIds = Lists.newArrayList(subset);
                        log.info("Free the avatars: freeing originals", "count", subsetIds.size());
                        if (!subsetIds.isEmpty()) {
                            updatePartial(AvatarRecord.class,
                                new Where(new In(AvatarRecord.ITEM_ID, subsetIds)), null,
                                AvatarRecord.USED, Item.UNUSED);
                        }
                    }
                    // break up the origs so we do no more than 5k at once
                    for (Iterable<Integer> subset : Iterables.partition(
                            Iterables.filter(ids, IS_CLONE_ID), 5000, false)) {
                        List<Integer> subsetIds = Lists.newArrayList(subset);
                        log.info("Free the avatars: freeing clones", "count", subsetIds.size());
                        if (!subsetIds.isEmpty()) {
                            updatePartial(AvatarCloneRecord.class,
                                new Where(new In(AvatarCloneRecord.ITEM_ID, subsetIds)), null,
                                AvatarCloneRecord.USED, Item.UNUSED);
                        }
                    }
                    log.info("Free the avatars: done!");

                } catch (Exception e) {
                    log.info("Free the avatars: oh shit. Let's do it another time.", e);
                }
            }
        });
    }

    /**
     * Update the scale of the specified avatar.
     */
    public void updateScale (int avatarId, float newScale)
    {
        int result = updatePartial((avatarId > 0) ? getItemClass() : getCloneClass(),
            avatarId, (avatarId > 0) ? AvatarRecord.SCALE : AvatarCloneRecord.SCALE, newScale);
        if (0 == result) {
            log.warning("Unable to find avatar to update scale [avatarId=" + avatarId + "].");
        }
    }

    @Override
    protected Class<AvatarRecord> getItemClass ()
    {
        return AvatarRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(AvatarCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(AvatarCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(AvatarRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new AvatarTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new AvatarTagHistoryRecord();
    }

    // dependencies
    /* TEMP */ @Inject protected MemberRepository _memberRepo;
}
