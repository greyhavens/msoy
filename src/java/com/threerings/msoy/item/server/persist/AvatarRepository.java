//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link AvatarRecord} items.
 */
public class AvatarRepository extends ItemRepository<AvatarRecord>
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
    protected Class<? extends CatalogRecord<AvatarRecord>> getCatalogClass ()
    {
        return AvatarCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<AvatarRecord>> getCloneClass ()
    {
        return AvatarCloneRecord.class;
    }

    @Override
    protected Class<? extends TagRecord<AvatarRecord>> getTagClass ()
    {
        return AvatarTagRecord.class;
    }

    @Override
    protected Class<? extends TagHistoryRecord<AvatarRecord>> getTagHistoryClass ()
    {
        return AvatarTagHistoryRecord.class;
    }

    @Override
    protected Class<? extends RatingRecord<AvatarRecord>> getRatingClass ()
    {
        return AvatarRatingRecord.class;
    }
}
