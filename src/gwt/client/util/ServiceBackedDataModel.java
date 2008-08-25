//
// $Id$

package client.util;

import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.data.all.GwtAuthCodes;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 * A data model that can be customized for components that obtain their data by calling a service
 * method to fetch a range of items.  Type T is from DataModel, and must be the type that is
 * return in a list from DataModel.doFetchRows.  Type R is for the AsyncCallback, and can be
 * anything - it is passed into getCount() and getRows() from the service call.
 */
public abstract class ServiceBackedDataModel<T, R> implements DataModel<T>
{
    /**
     * Prepends an item to an already loaded model. The model must have at least been asked to
     * display its first page (and hence have it's total count).
     */
    public void prependItem (T item)
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
    public void appendItem (T item)
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
        _pageItems = Collections.emptyList();
    }

    // from interface DataModel
    public int getItemCount ()
    {
        return Math.max(_count, _pageOffset + _pageItems.size());
    }

    // from interface DataModel
    public void removeItem (T item)
    {
        _pageItems.remove(item);
        _count--;
    }

    // from interface DataModel
    public void doFetchRows (int start, int count, final AsyncCallback<List<T>> callback)
    {
        if (_pageOffset == start && _pageCount == count) {
            callback.onSuccess(_pageItems);
        } else {
            callFetchService(
                _pageOffset = start, _pageCount = count, _count < 0, new AsyncCallback<R>() {
                public void onSuccess (R result) {
                    ServiceBackedDataModel.this.onSuccess(result, callback);
                }
                public void onFailure (Throwable cause) {
                    ServiceBackedDataModel.this.onFailure(cause, callback);
                }
            });
        }
    }

    protected void onSuccess (R result, AsyncCallback<List<T>> callback)
    {
        if (_count < 0) {
            _count = getCount(result);
        }
        _pageItems = getRows(result);
        callback.onSuccess(_pageItems);
    }

    protected void onFailure (Throwable caught, AsyncCallback<List<T>> callback)
    {
        if (GwtAuthCodes.SESSION_EXPIRED.equals(caught.getMessage())) {
            MsoyUI.showPasswordExpired(CShell.serverError(caught));
        } else {
            MsoyUI.error(CShell.serverError(caught));
        }
    }

    /** Calls the service to obtain data, should pass this as the callback. */
    protected abstract void callFetchService (
        int start, int count, boolean needCount, AsyncCallback<R> callback);

    /** Returns the count from the service result. */
    protected abstract int getCount (R result);

    /** Returns the list of row items from the service result. */
    protected abstract List<T> getRows (R result);

    /** The count of items in our model, filled in by the first call to {@link #doFetchRows}. */
    protected int _count = -1;

    /** The offset of the page we're currently displaying. */
    protected int _pageOffset;

    /** The requested count for the page we're currently displaying. */
    protected int _pageCount = -1;

    /** The items we got back for the page we're currently displaying. */
    protected List<T> _pageItems = Collections.emptyList();
}
