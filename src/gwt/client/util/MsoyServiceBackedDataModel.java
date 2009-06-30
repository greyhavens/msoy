//
// $Id$

package client.util;

import com.threerings.gwt.util.ServiceBackedDataModel;

import client.ui.MsoyUI;

/**
 * A data model that can be customized for components that obtain their data by calling a service
 * method to fetch a range of items.  Type T is from DataModel, and must be the type that is
 * return in a list from DataModel.doFetchRows.  Type R is for the AsyncCallback, and can be
 * anything - it is passed into getCount() and getRows() from the service call.
 */
public abstract class MsoyServiceBackedDataModel<T, R> extends ServiceBackedDataModel<T, R>
{
    @Override // from ServiceBackedDataModel
    protected void reportFailure (Throwable caught)
    {
        MsoyUI.reportServiceFailure(caught);
    }
}
