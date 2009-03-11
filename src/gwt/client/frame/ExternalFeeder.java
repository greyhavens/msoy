//
// $Id$

package client.frame;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Frame;
import client.util.events.FlashEvents;
import client.util.events.TrophyEvent;

/**
 * Handles publishing events to external feeds like Facebook or OpenSocial.
 */
public class ExternalFeeder
{
    public ExternalFeeder (Frame frame)
    {
        _frame = frame;
        FlashEvents.addListener(new TrophyEvent.Listener() {
            public void trophyEarned (TrophyEvent event) {
                publishTrophyToFacebook(event);
            }
        });
    }

    protected void publishTrophyToFacebook (TrophyEvent event)
    {
        // unfortunately, there is no way to track what actually got published, or more importantly
        // how many views a news feed item actually gets... so just track that the user was
        // interested enough to try 
        _frame.reportClientAction(null, "2009-03 trophy publish request",
                                  "gameId=" + event.getGameId());
        publishTrophy(event.getGameId(), event.getGame(), event.getTrophy(),
                      event.getDescription(), event.getMediaURL(),
                      Pages.makeURL(Pages.GAMES, Args.compose("d", event.getGameId(), "t")));
    }

    protected native void publishTrophy (int gameId, String game, String trophy, String descrip,
                                         String mediaURL, String trophyURL)
    /*-{
        var ids = new Array();
        var data = {
            "game_id": gameId,
            "game": game,
            "trophy": trophy,
            "descrip": descrip,
            "images": [ {"src": mediaURL, "href": trophyURL} ] };
        $wnd.FB_PostTrophy(data, ids);
    }-*/;

    protected Frame _frame;
}
