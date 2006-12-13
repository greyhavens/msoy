//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Furniture} items.
 */
public class FurnitureRepository extends ItemRepository<
    FurnitureRecord,
    FurnitureCloneRecord,
    FurnitureCatalogRecord,
    FurnitureTagRecord,
    FurnitureTagHistoryRecord,
    FurnitureRatingRecord>
{
    public FurnitureRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<FurnitureRecord> getItemClass () {
        return FurnitureRecord.class;
    }
    
    @Override
    protected Class<FurnitureCatalogRecord> getCatalogClass ()
    {
        return FurnitureCatalogRecord.class;
    }

    @Override
    protected Class<FurnitureCloneRecord> getCloneClass ()
    {
        return FurnitureCloneRecord.class;
    }

    @Override
    protected Class<FurnitureTagRecord> getTagClass ()
    {
        return FurnitureTagRecord.class;
    }

    @Override
    protected Class<FurnitureTagHistoryRecord> getTagHistoryClass ()
    {
        return FurnitureTagHistoryRecord.class;
    }
    
    @Override
    protected Class<FurnitureRatingRecord> getRatingClass ()
    {
        return FurnitureRatingRecord.class;
    }
}
