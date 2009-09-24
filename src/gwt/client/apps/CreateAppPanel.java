//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.edutil.EditorTable;
import client.util.ClickCallback;
import client.util.Link;

/**
 * Panel for entering the required fields for an application and using them to create one.
 */
public class CreateAppPanel extends EditorTable
{
    /**
     * Creates a new panel for creating an application.
     */
    public CreateAppPanel ()
    {
        addStyleName("createApp");
        AppUtil.addNameBox(this, _info);

        Button save = addSaveRow();
        new ClickCallback<Integer>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _appsvc.createApp(_info.name, this);
                return true;
            }
            protected boolean gotResult (Integer appId) {
                Link.go(Pages.APPS, "e", appId);
                return true;
            }
        };
    }

    protected AppInfo _info = new AppInfo();

    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
