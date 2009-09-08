//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.game.gwt.FacebookInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.StringUtil;

/**
 * Displays and allows editing of a game's Facebook info.
 */
public class FacebookInfoEditorPanel extends FlowPanel
{
    public FacebookInfoEditorPanel (final FacebookInfo info)
    {
        add(_mainFields = new EditorTable());
        _mainFields.addWidget(MsoyUI.createHTML(_msgs.fieIntro(), null), 2);

        _mainFields.addSpacer();

        _viewRow = _mainFields.addRow("", MsoyUI.createHTML("", null), null);
        updateAppLink(info);

        final TextBox key = MsoyUI.createTextBox(
            info.apiKey, FacebookInfo.KEY_LENGTH, FacebookInfo.KEY_LENGTH);
        _mainFields.addRow(_msgs.fieKey(), key, new Command() {
            public void execute () {
                info.apiKey = key.getText().trim();
            }
        });

        final TextBox secret = MsoyUI.createTextBox(
            info.appSecret, FacebookInfo.SECRET_LENGTH, FacebookInfo.SECRET_LENGTH);
        _mainFields.addRow(_msgs.fieSecret(), secret, new Command() {
            public void execute () {
                info.appSecret = secret.getText().trim();
            }
        });

        final TextBox canvasName = MsoyUI.createTextBox(
            info.canvasName, FacebookInfo.CANVAS_NAME_LENGTH, FacebookInfo.CANVAS_NAME_LENGTH);
        _mainFields.addRow(_msgs.fieCanvasName(), canvasName, new Command() {
            public void execute () {
                info.canvasName = canvasName.getText().trim();
            }
        });

        final CheckBox chromeless = new CheckBox(_msgs.fieChromelessText());
        chromeless.setValue(info.chromeless);
        _mainFields.addRow(_msgs.fieChromeless(), chromeless, new Command() {
            public void execute () {
                info.chromeless = chromeless.getValue();
            }
        });

        _mainFields.addSpacer();

        Button save = _mainFields.addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!_mainFields.bindChanges()) {
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
        _mainFields.setWidget(_viewRow, 1, MsoyUI.createHTML(_msgs.fieViewApp(info.apiKey), null));
        _mainFields.getRowFormatter().setVisible(_viewRow, !StringUtil.isBlank(info.apiKey));
    }

    protected EditorTable _mainFields;
    protected int _viewRow;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
