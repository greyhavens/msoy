//
// $Id$

package client.util;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;

import client.shell.CShell;
import client.util.MsoyUI;

/**
 * A data model that can be customized for components that obtain their data by calling a service
 * method to fetch a range of items.
 */
public abstract class ServiceBackedDataModel implements DataModel, AsyncCallback
{
    // from interface DataModel
    public int getItemCount ()
    {
        return _count;
    }

    // from interface DataModel
    public void removeItem (Object item)
    {
        // TOOD
    }

    // from interface DataModel
    public void doFetchRows (int start, int count, AsyncCallback callback)
    {
        _callback = callback;
        callFetchService(start, count, _count == -1);
    }

    // from interface AsyncCallback
    public void onSuccess (Object result)
    {
        if (_count == -1) {
            _count = getCount(result);
        }
        try {
            _callback.onSuccess(getRows(result));
        } finally {
            _callback = null;
        }
    }

    // from interface AsyncCallback
    public void onFailure (Throwable caught)
    {
        CShell.log(this + " failed", caught);
        MsoyUI.error(CShell.serverError(caught));
    }

    /** Calls the service to obtain data, should pass this as the callback. */
    protected abstract void callFetchService (int start, int count, boolean needCount);

    /** Returns the count from the service result. */
    protected abstract int getCount (Object result);

    /** Returns the list of row items from the service result. */
    protected abstract List getRows (Object result);

    /** The count of items in our model, filled in by the first call to {@link #doFetchRows}. */
    protected int _count = -1;

    /** A pending callback stored while we're making our service call. */
    protected AsyncCallback _callback;
}
