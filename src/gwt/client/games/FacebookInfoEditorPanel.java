//
// $Id$

package client.games;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.game.gwt.FacebookInfo;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.StringUtil;

/**
 * Displays and allows editing of a game's Facebook info.
 */
public class FacebookInfoEditorPanel extends EditorTable
{
    public FacebookInfoEditorPanel (final FacebookInfo info)
    {
        addWidget(MsoyUI.createHTML(_msgs.fieIntro(), null), 2);

        addSpacer();

        _viewRow = addRow("", MsoyUI.createHTML("", null), null);
        updateAppLink(info);

        final TextBox key = MsoyUI.createTextBox(
            info.apiKey, FacebookInfo.KEY_LENGTH, FacebookInfo.KEY_LENGTH);
        addRow(_msgs.fieKey(), key, new Command() {
            public void execute () {
                info.apiKey = key.getText().trim();
            }
        });

        final TextBox secret = MsoyUI.createTextBox(
            info.appSecret, FacebookInfo.SECRET_LENGTH, FacebookInfo.SECRET_LENGTH);
        addRow(_msgs.fieSecret(), secret, new Command() {
            public void execute () {
                info.appSecret = secret.getText().trim();
            }
        });

        final TextBox canvasName = MsoyUI.createTextBox(
            info.canvasName, FacebookInfo.CANVAS_NAME_LENGTH, FacebookInfo.CANVAS_NAME_LENGTH);
        addRow(_msgs.fieCanvasName(), canvasName, new Command() {
            public void execute () {
                info.canvasName = canvasName.getText().trim();
            }
        });

        final CheckBox chromeless = new CheckBox(_msgs.fieChromelessText());
        chromeless.setValue(info.chromeless);
        addRow(_msgs.fieChromeless(), chromeless, new Command() {
            public void execute () {
                info.chromeless = chromeless.getValue();
            }
        });

        addSpacer();

        Button save = addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                _gamesvc.updateFacebookInfo(info, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.fieInfoUpdated());
                updateAppLink(info);
                return true;
            }
        };
    }

    protected void updateAppLink (FacebookInfo info)
    {
        setWidget(_viewRow, 1, MsoyUI.createHTML(_msgs.fieViewApp(info.apiKey), null));
        getRowFormatter().setVisible(_viewRow, !StringUtil.isBlank(info.apiKey));
    }

    protected int _viewRow;
}
