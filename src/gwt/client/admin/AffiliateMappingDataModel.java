//
// $Id$

package client.admin;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.AffiliateMapping;

import client.util.PagedServiceDataModel;
import client.util.ServiceUtil;

public class AffiliateMappingDataModel extends PagedServiceDataModel<AffiliateMapping>
{
    public AffiliateMappingDataModel (AdminServiceAsync adminSvc)
    {
        _adminSvc = adminSvc;
    }

    @Override
    protected void callFetchService (
        int start, int count, boolean needCount,
        AsyncCallback<PagedResult<AffiliateMapping>> callback)
    {
        _adminSvc.getAffiliateMappings(start, count, needCount, callback);
    }

    protected AdminServiceAsync _adminSvc;
}
