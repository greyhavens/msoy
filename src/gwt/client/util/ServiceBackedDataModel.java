//
// $Id$

package client.util;

import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.data.all.GwtAuthCodes;

import client.shell.CShell;
import client.util.MsoyUI;

/**
 * A data model that can be customized for components that obtain their data by calling a service
 * method to fetch a range of items.
 */
public abstract class ServiceBackedDataModel implements DataModel, AsyncCallback
{
    /**
     * Prepends an item to an already loaded model. The model must have at least been asked to
     * display its first page (and hence have it's total count).
     */
    public void prependItem (Object item)
    {
        // if we're on the first page, this new item will show up, so add it
        if (_pageOffset == 0) {
            _pageItems.add(0, item);
        }
        _count++;
    }

    /**
     * Appends an item to an already loaded model. The model must have at least been asked to
     * display its first page (and hence have it's total count).
     */
    public void appendItem (Object item)
    {
        // if we're on the last page and there are fewer than a full page of items, this new item
        // will show up on this page so add it
        if (_pageItems.size() < _pageCount) {
            _pageItems.add(item);
        }
        _count++;
    }

    /**
     * Clears out any cached data and resets the model to total blankness.
     */
    public void reset ()
    {
        _count = -1;
        _pageOffset = 0;
        _pageCount = -1;
        _pageItems = Collections.EMPTY_LIST;
    }

    // from interface DataModel
    public int getItemCount ()
    {
        return Math.max(_count, _pageOffset + _pageItems.size());
    }

    // from interface DataModel
    public void removeItem (Object item)
    {
        _pageItems.remove(item);
        _count--;
    }

    // from interface DataModel
    public void doFetchRows (int start, int count, AsyncCallback callback)
    {
        if (_pageOffset == start && _pageCount == count) {
            callback.onSuccess(_pageItems);
        } else {
            _callback = callback;
            callFetchService(_pageOffset = start, _pageCount = count, _count < 0);
        }
    }

    // from interface AsyncCallback
    public void onSuccess (Object result)
    {
        if (_count < 0) {
            _count = getCount(result);
        }
        try {
            _pageItems = getRows(result);
            _callback.onSuccess(_pageItems);
        } finally {
            _callback = null;
        }
    }

    // from interface AsyncCallback
    public void onFailure (Throwable caught)
    {
        CShell.log(this + " failed", caught);

        if (GwtAuthCodes.SESSION_EXPIRED.equals(caught.getMessage())) {
            MsoyUI.showPasswordExpired(CShell.serverError(caught));
        } else {
            MsoyUI.error(CShell.serverError(caught));
        }
    }

    /** Calls the service to obtain data, should pass this as the callback. */
    protected abstract void callFetchService (int start, int count, boolean needCount);

    /** Returns the count from the service result. */
    protected abstract int getCount (Object result);

    /** Returns the list of row items from the service result. */
    protected abstract List getRows (Object result);

    /** The count of items in our model, filled in by the first call to {@link #doFetchRows}. */
    protected int _count = -1;

    /** The offset of the page we're currently displaying. */
    protected int _pageOffset;

    /** The requested count for the page we're currently displaying. */
    protected int _pageCount = -1;

    /** The items we got back for the page we're currently displaying. */
    protected List _pageItems = Collections.EMPTY_LIST;

    /** A pending callback stored while we're making our service call. */
    protected AsyncCallback _callback;
}
