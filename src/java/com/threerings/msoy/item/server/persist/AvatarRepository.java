//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link AvatarRecord} items.
 */
public class AvatarRepository extends ItemRepository<
    AvatarRecord,
    AvatarCloneRecord,
    AvatarCatalogRecord,
    AvatarTagRecord,
    AvatarTagHistoryRecord,
    AvatarRatingRecord>
{
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
    protected Class<AvatarTagRecord> getTagClass ()
    {
        return AvatarTagRecord.class;
    }

    @Override
    protected Class<AvatarTagHistoryRecord> getTagHistoryClass ()
    {
        return AvatarTagHistoryRecord.class;
    }

    @Override
    protected Class<AvatarRatingRecord> getRatingClass ()
    {
        return AvatarRatingRecord.class;
    }
}
