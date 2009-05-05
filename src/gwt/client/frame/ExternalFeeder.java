//
// $Id$

package client.frame;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ABTestUtil;
import com.threerings.msoy.web.gwt.Args;
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
        // unfortunately, there is no way to track what actually got published, or more importantly
        // how many views a news feed item actually gets... so just track that the user was
        // interested enough to try 
        CShell.frame.reportClientAction(null, "2009-03 trophy publish request",
                                  "gameId=" + event.getGameId());

        // select trophy text based on a pseudo test (not a real A/B test on the server); append
        // the test group to the entry vector so the results can be viewed in the entry vector
        // table
        String[] templateConfig = DeploymentConfig.facebookTrophyTemplateConfig.split(",");
        int testGroup = ABTestUtil.getGroup(CShell.frame.getVisitorInfo().id, "trophy pseudo test",
            templateConfig.length / 2);
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
        String trophyURL = Pages.makeURL(Pages.GAMES, Args.compose(
            "vec", vector, event.getGameId(), "d", "t"));
        if (DeploymentConfig.facebookURLValidationHack) {
            mediaURL = "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png";
            trophyURL = "http://www.whirled.com/go/games-d_827_t";
        }

        publishTrophy(templateId, event.getGameId(), event.getGame(), event.getTrophy(),
            event.getDescription(), mediaURL, trophyURL, vector);
    }

    protected native void publishTrophy (String templateId, int gameId, String game, String trophy,
                                         String descrip, String mediaURL, String trophyURL,
                                         String vector)
    /*-{
        var ids = new Array();
        var data = {
            "game_id": gameId,
            "game": game,
            "trophy": trophy,
            "descrip": descrip,
            "vector": vector,
            "images": [ {"src": mediaURL, "href": trophyURL} ] };
        $wnd.FB_PostTrophy(data, templateId, ids);
    }-*/;

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game_id" : 827,
          "game" : "Corpse Craft",
          "trophy" : "Freshman",
          "descrip" : "Complete Chapter 3 of \"The Incident.\"",
          "vector" : "v.none",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */
}
