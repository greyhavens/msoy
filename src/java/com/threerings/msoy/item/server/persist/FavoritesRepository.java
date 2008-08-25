//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.item.data.all.Item;

/**
 * Manages members' favorite item information.
 */
@Singleton @BlockingThread
public class FavoritesRepository extends DepotRepository
{
    @Inject public FavoritesRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads up to <code>count</code> recently favorited items for the specified member (of any
     * item type).
     */
    public List<FavoriteItemRecord> loadRecentFavorites (int memberId, int count)
        throws PersistenceException
    {
        return findAll(FavoriteItemRecord.class,
                       new Where(FavoriteItemRecord.MEMBER_ID_C, memberId),
                       OrderBy.descending(FavoriteItemRecord.NOTED_ON_C),
                       new Limit(0, count));
    }

    /**
     * Loads all favorites for the specified member of the specified item type. If the type is
     * {@link Item#NOT_A_TYPE}, all of this member's favorites of all types will be returned.
     *
     * TODO: paginate if people start favoriting things up a storm.
     */
    public List<FavoriteItemRecord> loadFavorites (int memberId, byte itemType)
        throws PersistenceException
    {
        Where where = (itemType == Item.NOT_A_TYPE) ?
            new Where(FavoriteItemRecord.MEMBER_ID_C, memberId) :
            new Where(FavoriteItemRecord.MEMBER_ID_C, memberId,
                      FavoriteItemRecord.ITEM_TYPE_C, itemType);
        return findAll(FavoriteItemRecord.class, where,
                       OrderBy.descending(FavoriteItemRecord.NOTED_ON_C));
    }

    /**
     * Loads a favorite item record for the specified member and catalog listing. Returns null if
     * no such favorite exists.
     */
    public FavoriteItemRecord loadFavorite (int memberId, byte itemType, int catalogId)
        throws PersistenceException
    {
        return load(FavoriteItemRecord.class,
                    FavoriteItemRecord.getKey(memberId, itemType, catalogId));
    }

    /**
     * Notes that the specified member favorited the specified item.
     */
    public void noteFavorite (int memberId, byte itemType, int catalogId)
        throws PersistenceException
    {
        FavoriteItemRecord record = new FavoriteItemRecord();
        record.memberId = memberId;
        record.itemType = itemType;
        record.catalogId = catalogId;
        record.notedOn = new Timestamp(System.currentTimeMillis());
        insert(record);
    }

    /**
     * Clears out a favorited item registration.
     */
    public void clearFavorite (int memberId, byte itemType, int catalogId)
        throws PersistenceException
    {
        delete(FavoriteItemRecord.class, FavoriteItemRecord.getKey(memberId, itemType, catalogId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FavoriteItemRecord.class);
    }
}
