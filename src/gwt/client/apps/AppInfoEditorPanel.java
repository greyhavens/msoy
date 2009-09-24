//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.apps.gwt.AppService.AppData;

import client.edutil.EditorTable;
import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Panel for editing an application's info. Intended for use as a tab within the overall
 * application editor.
 */
public class AppInfoEditorPanel extends EditorTable
{
    /**
     * Creates a new application info editor for the info in the given data.
     */
    public AppInfoEditorPanel (AppData data)
    {
        final AppInfo info = data.info;
        AppUtil.addNameBox(this, info);

        Button save = addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _appsvc.updateAppInfo(info, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.editAppInfoUpdated());
                return true;
            }
        };

    }

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
