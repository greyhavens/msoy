//
// $Id$

package client.games;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.game.gwt.FacebookInfo;

import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Displays and allows editing of a game's Facebook info.
 */
public class FacebookInfoEditorPanel extends BaseEditorPanel
{
    public FacebookInfoEditorPanel (final FacebookInfo info)
    {
        addWidget(MsoyUI.createHTML(_msgs.fieIntro(), null), 2, null);

        addSpacer();

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
                return true;
            }
        };
    }
}
