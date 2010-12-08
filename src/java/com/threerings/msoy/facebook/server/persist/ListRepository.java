//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Join;

/**
 * Maintains persistent lists of string ids and cursors that refer to them.
 * TODO: this is not really specific to facebook, but currently not required elsewhere.
 */
@Singleton
public class ListRepository extends DepotRepository
{
    public static final String DEFAULT_CURSOR = "";

    /**
     * Creates a new list repository.
     */
    @Inject public ListRepository (PersistenceContext context)
    {
        super(context);
    }

    /**
     * Gets all the items in a list of the given id, using the cache or not as specified.
     */
    public List<String> getList (String listId, boolean useCache)
    {
        CacheStrategy strat = useCache ? CacheStrategy.BEST : CacheStrategy.NONE;
        return Lists.transform(from(ListItemRecord.class).cache(strat).
                               where(ListItemRecord.LIST_ID.eq(listId)).
                               ascending(ListItemRecord.INDEX).select(),
            new Function<ListItemRecord, String>() {
            public String apply (ListItemRecord rec) {
                return rec.id;
            }
        });
    }

    /**
     * Sets the content of the given list to the given ids, in order. The previous contents and
     * all cursors are lost.
     */
    public void setList (String listId, List<String> ids)
    {
        from(ListItemRecord.class).where(ListItemRecord.LIST_ID.eq(listId)).delete();
        from(ListCursorRecord.class).where(ListCursorRecord.LIST_ID.eq(listId)).delete();
        for (int ii = 0, ll = ids.size(); ii < ll; ++ii) {
            ListItemRecord item = new ListItemRecord();
            item.listId = listId;
            item.index = ii;
            item.id = ids.get(ii);
            insert(item);
        }
    }

    /**
     * Gets a single item in the given list at the given index, or null if there is no such item.
     */
    public String getItem (String listId, int index)
    {
        ListItemRecord item = load(ListItemRecord.getKey(listId, index));
        return item != null ? item.id : null;
    }

    /**
     * Gets the item from the given list that the given cursor is currently pointing to, or null if
     * there is no such cursor or list.
     */
    public String getCursorItem (String listId, String cursorId)
    {
        Join join = new Join(ListCursorRecord.class, Ops.and(
            ListCursorRecord.LIST_ID.eq(ListItemRecord.LIST_ID),
            ListCursorRecord.INDEX.eq(ListItemRecord.INDEX))).setType(Join.Type.INNER);

        ListItemRecord item = from(ListItemRecord.class).join(join).where(
            ListItemRecord.LIST_ID.eq(listId)).load();
        return (item == null) ? null : item.id;
    }

    /**
     * Gets the index the given cursor is currently at, or null if there is no such cursor or
     * list.
     */
    public Integer getCursorIndex (String listId, String cursorId)
    {
        ListCursorRecord cursor = load(ListCursorRecord.getKey(listId, cursorId));
        return cursor != null ? cursor.index : null;
    }

    /**
     * Gets a list of items from the given lists that the given cursor is currently pointing to for
     * each list. This is equivalent to calling {@link #getCursorItem(String, String)} for each
     * list but more efficient. The returned items are in the same order as the given lists and
     * will have the same number of entries.
     */
    public List<String> getCursorItems (List<String> listIds, String cursorId)
    {
        Join join = new Join(ListCursorRecord.class, Ops.and(
            ListCursorRecord.LIST_ID.eq(ListItemRecord.LIST_ID),
            ListCursorRecord.INDEX.eq(ListItemRecord.INDEX))).setType(Join.Type.INNER);

        Map<String, ListItemRecord> items = Maps.newHashMap();
        for (ListItemRecord item : from(ListItemRecord.class).join(join).where(
                 ListItemRecord.LIST_ID.in(listIds)).select()) {
            items.put(item.listId, item);
        }

        List<String> ids = Lists.newArrayList();
        for (String listId : listIds) {
            ListItemRecord item = items.get(listId);
            ids.add(item == null ? null : item.id);
        }
        return ids;
    }

    /**
     * Advances the given cursor on the given list and returns the new current item, or null if the
     * list is empty.
     */
    public String advanceCursor (String listId, String cursorId)
    {
        String first = getItem(listId, 0);
        if (first == null) {
            return null;
        }

        ListCursorRecord cursor = load(ListCursorRecord.getKey(listId, cursorId));
        if (cursor == null) {
            cursor = new ListCursorRecord();
            cursor.listId = listId;
            cursor.cursorId = cursorId;
            cursor.index = 0;
            insert(cursor);
            return first;
        }

        cursor.index++;
        String item = getItem(listId, cursor.index);
        if (item == null) {
            cursor.index = 0;
            item = first;
        }

        store(cursor);
        return item;
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ListCursorRecord.class);
        classes.add(ListItemRecord.class);
    }
}
