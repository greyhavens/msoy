//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.room.server.MsoySceneRegistry;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link AvatarRecord} items.
 */
@Singleton
public class AvatarRepository extends ItemRepository<AvatarRecord>
{
    @Entity(name="AvatarMogMarkRecord")
    public static class AvatarMogMarkRecord extends MogMarkRecord
    {
    }

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
    }

    /**
     * Finds all (original and cloned) avatars owned by the specified player in the given theme.
     * Note: this method explicitly and deliberately bypasses the cache, as its sole client
     * {@link MsoySceneRegistry} needs to be able to rely on an accurate enumeration of themed
     * avatars for the gifting to work correctly.
     */
    public List<AvatarRecord> getThemeAvatars (int ownerId, int themeId)
    {
        if (ownerId == 0 || themeId == 0) {
            throw new IllegalArgumentException("Expecting non-zero arguments");
        }

        QueryClause join = new Join(getItemColumn(ItemRecord.ITEM_ID),
            new ColumnExp<Object>(getMogMarkClass(), MogMarkRecord.ITEM_ID.name));

        // locate all matching original items
        List<AvatarRecord> results = findAll(
            getItemClass(), CacheStrategy.NONE, join, new Where(Ops.and(
                new ColumnExp<Object>(getMogMarkClass(), MogMarkRecord.GROUP_ID.name).eq(themeId),
                getItemColumn(ItemRecord.OWNER_ID).eq(ownerId))));

        // add in the clones
        results.addAll(resolveClones(findAll(getCloneClass(), CacheStrategy.NONE,
            new Join(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID),
                     getItemColumn(ItemRecord.ITEM_ID)),
            join,
            new Where(Ops.and(
                new ColumnExp<Object>(getMogMarkClass(), MogMarkRecord.GROUP_ID.name).eq(themeId),
                getCloneColumn(CloneRecord.OWNER_ID).eq(ownerId))))));

        return results;
    }

    /**
     * Update the scale of the specified avatar.
     */
    public void updateScale (int avatarId, float newScale)
    {
        int result = (avatarId > 0) ?
            updatePartial(getItemKey(avatarId), AvatarRecord.SCALE, newScale) :
            updatePartial(getCloneKey(avatarId), AvatarCloneRecord.SCALE, newScale);
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
    protected MogMarkRecord createMogMarkRecord ()
    {
        return new AvatarMogMarkRecord();
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

    @Override
    protected ColumnExp<byte[]> getPrimaryMediaColumn ()
    {
        return getItemColumn(AvatarRecord.AVATAR_MEDIA_HASH);
    }
}
