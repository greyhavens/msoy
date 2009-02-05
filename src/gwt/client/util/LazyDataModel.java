//
// $Id$

package client.util;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.SimpleDataModel;

/**
 * Provides access to data that is loaded in a single RPC query, but lazily, the first time data is
 * reuested. Note: this class extends {@link SimpleDataModel} and exposes the same functionality it
 * offers, but adds the implicit requirement that no data model manipulations be done before the
 * data is caused to be loaded.
 */
public abstract class LazyDataModel<T> extends SimpleDataModel<T>
{
    public LazyDataModel ()
    {
        super(null);
    }

    @Override // from SimpleDataModel
    public void doFetchRows (
        final int start, final int count, final AsyncCallback<List<T>> callback)
    {
        if (_items == null) {
            fetchData(new AsyncCallback<List<T>>() {
                public void onSuccess (List<T> data) {
                    _items = data;
                    LazyDataModel.super.doFetchRows(start, count, callback);
                }
                public void onFailure (Throwable cause) {
                    callback.onFailure(cause);
                }
            });
        } else {
            super.doFetchRows(start, count, callback);
        }
    }

    /**
     * Implementations should override this method, load their data and pass success and failure
     * through to the supplied callback.
     */
    protected abstract void fetchData (AsyncCallback<List<T>> callback);
}
