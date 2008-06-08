//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.item.data.all.ItemIdent;

import static com.threerings.msoy.Log.log;

@Singleton @BlockingThread
public class ItemListRepository extends DepotRepository
{
    @Inject public ItemListRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP: test and create tables
        try {
            loadInfos(0);
            loadList(0);
        } catch (PersistenceException pe) {
            log.warning("Unable to configure list tables", pe);
        }
    }

    public void createList (ItemListInfoRecord record)
        throws PersistenceException
    {
        // 
    }

    public void deleteList (int listId)
        throws PersistenceException
    {
        // delete both tables
        delete(ItemListInfoRecord.class, listId);
        delete(ItemIdentListRecord.class, listId);
    }

    /**
     * Load all the ItemListInfos for the specified member. Does not add in "standard" lists.
     */
    public List<ItemListInfoRecord> loadInfos (int memberId)
        throws PersistenceException
    {
        //List<ItemListInfoRecord> list = Collections.emptyList();
        List<ItemListInfoRecord> list = findAll(ItemListInfoRecord.class,
            new Where(ItemListInfoRecord.MEMBER_ID_C, memberId));
        return list;
    }

    public ItemListInfoRecord loadInfo (int listId)
        throws PersistenceException
    {
        return load(ItemListInfoRecord.class, listId);
    }

    /**
     * Load the specified item list info.
     */
    public ItemIdent[] loadList (int listId)
        throws PersistenceException
    {
        List<ItemIdentListRecord> list = findAll(ItemIdentListRecord.class,
            new Where(ItemIdentListRecord.LIST_ID_C, listId),
            OrderBy.ascending(ItemIdentListRecord.SEQUENCE_C));

        int size = list.size();
        ItemIdent[] idents = new ItemIdent[size];
        for (int ii = 0; ii < size; ii++) {
            idents[ii] = list.get(ii).toItemIdent();
        }

        return idents;
    }

    public void saveList (int listId, ItemIdent[] items)
        throws PersistenceException
    {
        // TODO: depot method to overwrite all rows with the same primary key with a new set
        // of rows
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ItemIdentListRecord.class);
        classes.add(ItemListInfoRecord.class);
    }
}
