//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Furniture} items.
 */
public class FurnitureRepository extends ItemRepository<FurnitureRecord>
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
    protected Class<? extends CatalogRecord<FurnitureRecord>> getCatalogClass ()
    {
        return FurnitureCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<FurnitureRecord>> getCloneClass ()
    {
        return FurnitureCloneRecord.class;
    }

    @Override
    protected Class<? extends TagRecord<FurnitureRecord>> getTagClass ()
    {
        return FurnitureTagRecord.class;
    }

    @Override
    protected Class<? extends TagHistoryRecord<FurnitureRecord>> getTagHistoryClass ()
    {
        return FurnitureTagHistoryRecord.class;
    }
}
