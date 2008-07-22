//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link AvatarRecord} items.
 */
@Singleton
public class AvatarRepository extends ItemRepository<
    AvatarRecord,
    AvatarCloneRecord,
    AvatarCatalogRecord,
    AvatarRatingRecord>
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
    }

    /**
     * Update the scale of the specified avatar.
     */
    public void updateScale (int avatarId, float newScale)
        throws PersistenceException
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
    protected Class<AvatarCatalogRecord> getCatalogClass ()
    {
        return AvatarCatalogRecord.class;
    }

    @Override
    protected Class<AvatarCloneRecord> getCloneClass ()
    {
        return AvatarCloneRecord.class;
    }

    @Override
    protected Class<AvatarRatingRecord> getRatingClass ()
    {
        return AvatarRatingRecord.class;
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
