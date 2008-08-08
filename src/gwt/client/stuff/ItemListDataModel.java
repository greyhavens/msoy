package client.stuff;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService.ItemListResult;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Provides a data model for a PagedGrid backed by a persistent item list. The item list is accessed
 * via the StuffService.
 *
 * @author mjensen
 */
public class ItemListDataModel extends ServiceBackedDataModel<Item, ItemListResult>
{
    public ItemListDataModel ()
    {
        // by default, show all item types
        this(Item.NOT_A_TYPE);
    }

    public ItemListDataModel (byte itemType)
    {
        _query = new ItemListQuery();
        _query.itemType = itemType;
    }

    public void setListId (int listId)
    {
        // we can't display a list unless/until the list id is set
        _initialized = listId >= 0;

        _query.listId = listId;

        // make sure that the model reloads next time a fetch is called
        reset();
    }

    public void setItemType (byte itemType)
    {
        _query.itemType = itemType;

        // make sure that the model reloads next time a fetch is called
        reset();
    }

    /**
     * Sets whether the list should be sorted in descending order.
     */
    public void setDescending (boolean descending)
    {
        _query.descending = descending;
    }

    /**
     * Sends an item list query to the service using this as a callback to collect the results.
     *
     * @see client.util.ServiceBackedDataModel#callFetchService(int, int, boolean)
     */
    @Override
    protected void callFetchService (int start, int count, boolean needsCount)
    {
        if (!_initialized) {
            return;
        }
        _query.offset = start;
        _query.count = count;
        _query.needsCount = needsCount;
        _stuffsvc.loadItemList(_query, this);
    }

    /**
     * Returns the total number of possible items returned (asynchronously) by the last service
     * fetch.
     *
     * @see client.util.ServiceBackedDataModel#getCount(java.lang.Object)
     */
    @Override
    protected int getCount (ItemListResult result)
    {
        return result.totalCount;
    }

    /**
     * Returns the list of item results from the last service fetch.
     *
     * @see client.util.ServiceBackedDataModel#getRows(java.lang.Object)
     */
    @Override
    protected List<Item> getRows (ItemListResult result)
    {
        return result.items;
    }

    protected ItemListQuery _query;

    protected boolean _initialized;

    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync) ServiceUtil.bind(GWT
        .create(StuffService.class), StuffService.ENTRY_POINT);
}
