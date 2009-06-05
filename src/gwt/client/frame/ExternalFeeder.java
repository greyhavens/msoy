//
// $Id$

package client.frame;

import java.util.Date;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ABTestCard;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;

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
        // select trophy text based on a pseudo test (not a real A/B test on the server); append
        // the test group to the entry vector so the results can be viewed in the entry vector
        // table
        String[] templateConfig = DeploymentConfig.facebookTrophyTemplateConfig.split(",");
        int testGroup = new ABTestCard("trophy pseudo test", new Date(),
            templateConfig.length / 2, false).getGroup(CShell.frame.getVisitorInfo());
        String vector = "v.fbtrophy" + templateConfig[(testGroup - 1) / 2];
        String templateId = templateConfig[(testGroup - 1) / 2 + 1];

        // Swap in some arbitrary public URLs here to satisfy Facebook's overly aggressive URL
        // validation... however, the other links in the template appear to fail as well. But since
        // they are in the template there isn't much we can do about it except make them link to
        // whirled.com. But then the template creation UI is very difficult to use... I guess we
        // will just have to get in line with Facebook and never test anything in development. If
        // we need a release to fix trophy publishing, so be it.
        // TODO: remove hackery if and when Facebook realize the value of testing
        String mediaURL = event.getMediaURL();
        String trophyURL = Pages.GAMES.makeURL("vec", vector, event.getGameId(), "d", "t");
        if (DeploymentConfig.devDeployment) {
            mediaURL = "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png";
            trophyURL = "http://www.whirled.com/go/games-d_827_t";
        }
        String actionUrl =
            DeploymentConfig.facebookCanvasUrl + "?game=" + event.getGameId() + "&vec=" + vector;

        publishTrophy(actionUrl, templateId, event.getGameId(), event.getGame(),
            event.getGameDescription(), event.getTrophy(), event.getDescription(), mediaURL,
            trophyURL, vector);
    }

    protected native void publishTrophy (String actionUrl, String templateId, int gameId,
                                         String game, String gameDesc, String trophy,
                                         String descrip, String mediaURL, String trophyURL,
                                         String vector)
    /*-{
        var data = {
            "game_id": gameId,
            "game": game,
            "game_desc": gameDesc,
            "trophy": trophy,
            "descrip": descrip,
            "vector": vector,
            "action_url": actionUrl,
            "images": [ {"src": mediaURL, "href": actionUrl} ] };
        $wnd.FB_PostTrophy(templateId, data);
    }-*/;

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game_id" : 827,
          "game" : "Corpse Craft",
          "game_desc" :
              "Build an army of corpses to destroy your foes in this puzzle-action hybrid.",
          "trophy" : "Freshman",
          "descrip" : "Complete Chapter 3 of \"The Incident.\"",
          "vector" : "v.none",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */
}
