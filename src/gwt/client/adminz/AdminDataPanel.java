//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;

import client.ui.MsoyUI;
import client.util.PageCallback;
import client.util.ServiceUtil;

/**
 * A panel that displays some data after loading.
 */
public abstract class AdminDataPanel<T> extends FlowPanel
{
    protected AdminDataPanel (String styleName)
    {
        setStyleName(styleName);
        add(MsoyUI.createNowLoading());
    }

    protected abstract void init (T data);

    protected void addNoDataMessage (String message)
    {
        add(MsoyUI.createLabel(message, "infoLabel"));
    }

    protected PageCallback<T> createCallback ()
    {
        return new PageCallback<T>(this) {
            public void onSuccess (T data) {
                clear();
                init(data);
            }
        };
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
