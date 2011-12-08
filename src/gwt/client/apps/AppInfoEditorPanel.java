//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.NumberTextBox;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService.AppData;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.web.gwt.ClientMode;

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

        final ListBox clientModes = new ListBox();
        clientModes.addItem(_msgs.editAppInfoClientModeUnspecified(),
            ClientMode.UNSPECIFIED.toString());
        clientModes.addItem(_msgs.editAppInfoClientModeGames(), ClientMode.FB_GAMES.toString());
        clientModes.addItem(_msgs.editAppInfoClientModeRooms(), ClientMode.FB_ROOMS.toString());
        clientModes.addItem(_msgs.editAppInfoClientModeDj(), ClientMode.WHIRLED_DJ.toString());
        addRow(_msgs.editAppInfoClientMode(), clientModes, new Command() {
            @Override public void execute () {
                String mode = clientModes.getValue(clientModes.getSelectedIndex());
                info.clientMode = ClientMode.valueOf(mode);
            }
        });
        clientModes.setSelectedIndex(info.clientMode.ordinal());

        final NumberTextBox groupId = NumberTextBox.newIntBox().withValue(info.groupId);
        addRow(_msgs.editAppInfoGroupId(), groupId, new Command() {
            public void execute () {
                info.groupId = groupId.getNumber().intValue();
            }
        });

        final TextBox domain = MsoyUI.createTextBox(info.domain);
        addRow(_msgs.editAppInfoDomain(), domain, new Command() {
            public void execute () {
                info.domain = domain.getValue();
            }
        });

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
