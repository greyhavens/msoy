//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.facebook.gwt.KontagentInfo;

import client.edutil.EditorTable;
import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Panel for editing an application's Kontagent integration data.
 */
public class KontagentInfoPanel extends EditorTable
{
    public KontagentInfoPanel (final AppInfo appInfo, final KontagentInfo kinfo)
    {
        addWidget(MsoyUI.createHTML(_msgs.kontagentTitle(appInfo.name), "Title"), 2);
        addSpacer();

        final TextBox key = MsoyUI.createTextBox(
            kinfo.apiKey, KontagentInfo.KEY_LENGTH, KontagentInfo.KEY_LENGTH);
        addRow(_msgs.kontagentKeyLabel(), key, new Command() {
            public void execute () {
                kinfo.apiKey = key.getText().trim();
            }
        });

        final TextBox secret = MsoyUI.createTextBox(
            kinfo.apiSecret, KontagentInfo.SECRET_LENGTH, KontagentInfo.SECRET_LENGTH);
        addRow(_msgs.kontagentSecretLabel(), secret, new Command() {
            public void execute () {
                kinfo.apiSecret = secret.getText().trim();
            }
        });

        addSpacer();

        Button save = addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _appsvc.updateKontagentInfo(appInfo.appId, kinfo, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.kontagentUpdated());
                return true;
            }
        };
    }

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
