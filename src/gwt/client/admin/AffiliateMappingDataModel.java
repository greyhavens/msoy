//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.AffiliateMapping;

import client.util.PagedServiceDataModel;
import client.util.ServiceUtil;

public class AffiliateMappingDataModel extends PagedServiceDataModel<AffiliateMapping>
{
    public AffiliateMappingDataModel ()
    {
    }

    @Override
    protected void callFetchService (
        int start, int count, boolean needCount,
        AsyncCallback<PagedResult<AffiliateMapping>> callback)
    {
        _adminsvc.getAffiliateMappings(start, count, needCount, callback);
    }

    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
