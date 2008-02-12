//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;

/**
 * Does something extraordinary.
 */
public class PlayPanel extends SmartTable
{
    public PlayPanel (final int gameId, int minPlayers, int maxPlayers)
    {
        super("playPanel", 0, 0);

        int row = 0;
        setText(row++, 0, CGame.msgs.gdpPlay(), 1, "Title");

        // if the game supports single-player play, it gets a "Quick Single" button
        if (minPlayers == 1 && maxPlayers != Integer.MAX_VALUE) {
            addPlayButton(row, 0, "SinglePlay", CGame.msgs.gdpJustMe(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "s", ""+gameId));
                }
            });
            row += 2;
        }

        // if the game supports multiplayer play, it gets "Quick Multi" buttons
        if (maxPlayers > 1) {
            addPlayButton(row, 0, "FriendPlay", CGame.msgs.gdpMyFriends(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, Args.compose("game", "f", ""+gameId));
                }
            });
            row += 2;

//             addPlayButton(pbbox, 3, 0, "AnyonePlay", CGame.msgs.gdpAnyone(), new ClickListener() {
//                 public void onClick (Widget sender) {
//                     Application.go(Page.WORLD, Args.compose("game", "m", ""+gameId));
//                 }
//             });

//             addPlayButton(pbbox, 3, 1, "CustomPlay", CGame.msgs.gdpCustom(), new ClickListener() {
//                 public void onClick (Widget sender) {
//                     Application.go(Page.WORLD, Args.compose("game", "l", ""+gameId));
//                 }
//             });
        }
    }

    protected void addPlayButton (
        int row, int column, String styleName, String tip, ClickListener onClick)
    {
        Button play = new Button("", onClick);
        play.setStyleName("Button");
        play.addStyleName(styleName);
        setWidget(row, column, play);
        setText(row+1, column, tip, 1, "Label");
    }
}
