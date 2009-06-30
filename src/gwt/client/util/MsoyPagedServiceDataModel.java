//
// $Id$

package client.util;

import com.threerings.gwt.util.PagedResult;
import com.threerings.gwt.util.PagedServiceDataModel;

import client.ui.MsoyUI;

public abstract class MsoyPagedServiceDataModel<T, R extends PagedResult<T>>
    extends PagedServiceDataModel<T, R>
{
    @Override // from ServiceBackedDataModel
    protected void reportFailure (Throwable caught)
    {
        MsoyUI.reportServiceFailure(caught);
    }
}
