//
// $Id$

package client.apps;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextArea;

import com.threerings.gwt.util.StringUtil;
import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;

import client.edutil.EditorTable;
import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Panel for editing the daily notifications that are cycled through for the main games portal
 * app.
 */
public class FacebookDailyNotificationsPanel extends EditorTable
{
    public FacebookDailyNotificationsPanel (final AppInfo info, List<String> dailyNotifications)
    {
        addStyleName("dailyNotifications");

        addWidget(MsoyUI.createHTML(_msgs.fbDailyNotifsTitle(info.name), "Title"), 2);

        final TextArea ids = MsoyUI.createTextArea(
            StringUtil.join(dailyNotifications, "\n"), 32, 5);
        addRow(_msgs.fbDailyNotifsIdsLabel(), ids, new Command() {
            @Override public void execute () {
                _ids = new ArrayList<String>();
                for (String id : ids.getText().split("( |\t|\r|\n)+")) {
                    _ids.add(id);
                }
            }
        });

        Button save = addSaveRow();
        new ClickCallback<Void>(save) {
            @Override public boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _appsvc.setDailyNotifications(info.appId, _ids, this);
                return true;
            }
            @Override public boolean gotResult (Void result) {
                MsoyUI.info(_msgs.fbDailyNotifsUpdated());
                return true;
            }
        };
    }

    protected List<String> _ids;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
