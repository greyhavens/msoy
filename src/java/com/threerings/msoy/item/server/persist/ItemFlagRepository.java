//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;

import com.threerings.msoy.server.persist.CountRecord;

// TODO: doc

@Singleton
public class ItemFlagRepository extends DepotRepository
{
    @Inject public ItemFlagRepository (PersistenceContext ctx)
    {
        super(ctx);

        // rename the "flag" column
        _ctx.registerMigration(ItemFlagRecord.class,
            new SchemaMigration.Rename(3, "flag", ItemFlagRecord.KIND));
    }

    public int countItemFlags ()
    {
        return load(CountRecord.class, new FromOverride(ItemFlagRecord.class)).count;
    }

    public List<ItemFlagRecord> loadFlags (int start, int count)
    {
        return findAll(ItemFlagRecord.class, new Limit(start, count),
            OrderBy.ascending(ItemFlagRecord.TIMESTAMP));
    }

    public void addFlag (ItemFlagRecord itemFlag)
    {
        insert(itemFlag);
    }

    public void removeItemFlags (byte itemType, int itemId)
    {
        deleteAll(ItemFlagRecord.class, new Where(
            ItemFlagRecord.ITEM_TYPE, itemType, ItemFlagRecord.ITEM_ID, itemId));
    }

    @Override
    protected void getManagedRecords (
        Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ItemFlagRecord.class);
    }
}
