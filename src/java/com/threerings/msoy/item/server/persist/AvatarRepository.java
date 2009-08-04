//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

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
}
