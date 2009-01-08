//
// $Id$

package client.frame;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.util.events.FlashEvents;
import client.util.events.TrophyEvent;

/**
 * Handles publishing events to external feeds like Facebook or OpenSocial.
 */
public class ExternalFeeder
{
    public ExternalFeeder ()
    {
        FlashEvents.addListener(new TrophyEvent.Listener() {
            public void trophyEarned (TrophyEvent event) {
                publishTrophyToFacebook(event);
            }
        });
    }

    protected void publishTrophyToFacebook (TrophyEvent event)
    {
        if (1 != 0) {
            // this fucking sucks and it doesn't fucking work anyway because Facebook's goddamned
            // iframe pops up and displays blank contents, probably because it's popping up over
            // Flash, and it steals focus away from the game and there's no way to position the
            // iframe and it's generally just a disaster on all fronts, someone just kill me now
            return;
        }
        if (!DeploymentConfig.devDeployment) {
            return; // disabled for now in production
        }
        publishTrophy(event.getGameId(), event.getGame(), event.getTrophy(), event.getDescription(),
                      event.getMediaURL(), DeploymentConfig.serverURL +
                      Pages.makeLink(Pages.GAMES, Args.compose("d", event.getGameId(), "t")));
    }

    protected native void publishTrophy (int gameId, String game, String trophy, String descrip,
                                         String mediaURL, String trophyURL)
    /*-{
        var templateId = 41625542934; // login to facebook to get this magic number!
        var ids = new Array();
        var data = new Object();
        data.game_id = gameId;
        data.game = game;
        data.trophy = trophy;
        data.descrip = descrip;
        data.images = [ {'src':mediaURL, 'href':trophyURL} ]
        $wnd.FB_PostStory(templateId, data, ids, "");
    }-*/;
}
