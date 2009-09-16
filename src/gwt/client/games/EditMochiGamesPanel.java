//
// $Id$

package client.games;

import client.ui.MsoyUI;
import client.util.ClickCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

public class EditMochiGamesPanel extends FlowPanel
{
    public EditMochiGamesPanel ()
    {
        setStyleName("editMochiGames");

        // Mochi game adding
        final TextBox mochiTag = new TextBox();
        final Button addMochi = new Button("add mochi game");
        new ClickCallback<Void>(addMochi) {
            @Override protected boolean callService () {
                _gamesvc.addMochiGame(mochiTag.getText().trim(), this);
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                MsoyUI.info("Game added");
                mochiTag.setText("");
                return true;
            }
        };
        add(mochiTag);
        add(addMochi);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
