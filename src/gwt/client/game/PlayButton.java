//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a play button that starts playing the game in question.
 */
public class PlayButton
{
    public static PushButton createSmall (int gameId)
    {
        return create(gameId, null);
    }

    public static PushButton createMedium (int gameId)
    {
        return create(gameId, "playButtonMedium");
    }

    public static PushButton createLarge (int gameId)
    {
        return create(gameId, "playButtonLarge");
    }

    public static PushButton createCustom (int gameId, String style)
    {
        return create(gameId, style);
    }

    protected static PushButton create (int gameId, String style)
    {
        PushButton play;
        if (style == null) {
            play = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.playPlay(), null);
        } else {
            play = new PushButton();
            play.setStyleName(style);
        }
        play.addClickHandler(Link.createHandler(Pages.WORLD, "game", "p", gameId));
        return play;
    }

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
}
