//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.SQLException;

import java.util.Collections;
import java.util.List;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.msoy.item.data.all.ItemIdent;

public class ItemListRepository extends DepotRepository
{
    public ItemListRepository (ConnectionProvider provider)
    {
        super(new PersistenceContext("itemlistdb", provider));
    }

    public void createItemList (ItemListInfoRecord record)
        throws PersistenceException
    {
        // 
    }

    /**
     * Load all the ItemListInfos for the specified member. Does not add in "standard" lists.
     */
    public List<ItemListInfoRecord> loadItemListInfos (int memberId)
        throws PersistenceException
    {
        List<ItemListInfoRecord> list = Collections.emptyList();
//        List<ItemListInfoRecord> list = findAll(ItemListInfoRecord.class,
//            new Where(ItemListInfoRecord.MEMBER_ID_C, memberId));
        return list;
    }

    public ItemListInfoRecord loadItemListInfo (int listId)
        throws PersistenceException
    {
        return load(ItemListInfoRecord.class, listId);
    }

    /**
     * Load the specified item list info.
     */
    public ItemIdent[] loadItemList (int listId)
        throws PersistenceException
    {
        ItemListRecord record = load(ItemListRecord.class, listId);
        return record.toItemList();
    }

    public void saveItemList (int listId, ItemIdent[] items)
        throws PersistenceException
    {
        store(new ItemListRecord(listId, items));
    }
}
