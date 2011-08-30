//
// $Id$

package client.apps;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.util.InfoCallback;
import client.util.Link;

/**
 * Panel for viewing a list of all defined applications.
 */
public class AppListPanel extends SmartTable
{
    /**
     * Creates a new application listing panel.
     */
    public AppListPanel ()
    {
        super("appsList", 5, 5);
        _appsvc.getApps(new InfoCallback<List<AppInfo>> () {
            @Override public void onSuccess (List<AppInfo> result) {
                setApps(result);
            }
        });
    }

    protected void setApps (List<AppInfo> apps)
    {
        int row = 0;
        for (AppInfo app : apps) {
            setWidget(row++, 0, Link.create(app.name, Pages.APPS, "e", app.appId));
        }
        setWidget(row++, 0, Link.create(_msgs.addNewApp(), Pages.APPS, "c"));
    }

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
