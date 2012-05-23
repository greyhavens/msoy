//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Funcs;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.server.persist.MemberRecord.Flag;
import com.threerings.msoy.server.persist.MemberRecord;

/**
 * Manages members' favorite item information.
 */
@Singleton @BlockingThread
public class FavoritesRepository extends DepotRepository
{
    @Entity @Computed(shadowOf=FavoriteItemRecord.class)
    public static class FavoritedItemResultRecord extends PersistentRecord
    {
        public MsoyItemType itemType;
        public int catalogId;
    }

    @Inject public FavoritesRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads up to <code>count</code> items recently favorited by subscribers. If the
     * type is {@link MsoyItemType#NOT_A_TYPE}, all types will be returned.
     */
    public List<FavoritedItemResultRecord> loadRecentFavorites (
        int offset, int rows, MsoyItemType type)
    {
        List<SQLExpression<?>> conditions = Lists.newArrayList();

        // only count upstanding players who have logged a few hours into Whirled
        conditions.add(MemberRecord.SESSION_MINUTES.greaterEq(8*60));
        conditions.add(MemberRecord.FLAGS.bitAnd(Flag.TROUBLEMAKER.getBit()).eq(0));

        // only look at favoriting done in the past N days
        conditions.add(FavoriteItemRecord.NOTED_ON.greaterThan(
            new Timestamp(System.currentTimeMillis() - RECENT_FAVORITE_CUTOFF)));

        if (type != MsoyItemType.NOT_A_TYPE) {
            // possibly only care about some item types
            conditions.add(FavoriteItemRecord.ITEM_TYPE.eq(type.toByte()));
        }

        return findAll(FavoritedItemResultRecord.class,
            new Join(FavoriteItemRecord.MEMBER_ID, MemberRecord.MEMBER_ID),
            new Where(Ops.and(conditions)),
            new GroupBy(FavoriteItemRecord.ITEM_TYPE, FavoriteItemRecord.CATALOG_ID),
            OrderBy.descending(Funcs.count(MemberRecord.MEMBER_ID)),
            new Limit(offset, rows));
    }

    /**
     * Loads up to <code>count</code> recently favorited items for the specified member (of any
     * item type).
     */
    public List<FavoritedItemResultRecord> loadRecentFavorites (int memberId, int count)
    {
        return findAll(FavoritedItemResultRecord.class,
                       new Where(FavoriteItemRecord.MEMBER_ID, memberId),
                       OrderBy.descending(FavoriteItemRecord.NOTED_ON),
                       new Limit(0, count));
    }

    /**
     * Loads all favorites for the specified member of the specified item type. If the type is
     * {@link MsoyItemType#NOT_A_TYPE}, all of this member's favorites of all types will be returned.
     *
     * TODO: paginate if people start favoriting things up a storm.
     */
    public List<FavoritedItemResultRecord> loadFavorites (int memberId, MsoyItemType itemType)
    {
        Where where = (itemType == MsoyItemType.NOT_A_TYPE) ?
            new Where(FavoriteItemRecord.MEMBER_ID, memberId) :
            new Where(FavoriteItemRecord.MEMBER_ID, memberId,
                      FavoriteItemRecord.ITEM_TYPE, itemType);
        return findAll(FavoritedItemResultRecord.class, where,
                       OrderBy.descending(FavoriteItemRecord.NOTED_ON));
    }

    /**
     * Loads a favorite item record for the specified member and catalog listing. Returns null if
     * no such favorite exists.
     */
    public FavoriteItemRecord loadFavorite (int memberId, MsoyItemType itemType, int catalogId)
    {
        return load(FavoriteItemRecord.class,
            FavoriteItemRecord.getKey(memberId, itemType, catalogId));
    }

    /**
     * Notes that the specified member favorited the specified item.
     *
     * @return true iff the item wasn't a favorite (but is now)
     */
    public boolean noteFavorite (int memberId, MsoyItemType itemType, int catalogId)
    {
        FavoriteItemRecord record = new FavoriteItemRecord();
        record.memberId = memberId;
        record.itemType = itemType;
        record.catalogId = catalogId;
        record.notedOn = new Timestamp(System.currentTimeMillis());
        return store(record);
    }

    /**
     * Clears out a favorited item registration.
     *
     * @return true iff the item was a favorite (and now isn't)
     */
    public boolean clearFavorite (int memberId, MsoyItemType itemType, int catalogId)
    {
        return delete(FavoriteItemRecord.getKey(memberId, itemType, catalogId)) > 0;
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(FavoriteItemRecord.class,
                  new Where(FavoriteItemRecord.MEMBER_ID.in(memberIds)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FavoriteItemRecord.class);
    }

    // look at the last 10 days for item favoriting
    protected static final long RECENT_FAVORITE_CUTOFF = 10 * 24*60*60*1000L;
}
