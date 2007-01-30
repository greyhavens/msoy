//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link AvatarRecord} items.
 */
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

    public AvatarRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<AvatarRecord> getItemClass () {
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
