//
// $Id$

package client.frame;

import java.util.Map;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryKey;

import client.facebookbase.FacebookUtil;
import client.facebookbase.StoryFeeder;

import client.shell.CShell;
import client.util.events.FlashEvents;
import client.util.events.TrophyEvent;

/**
 * Handles publishing trophy stories to the facebook feed when a trophy event is received.
 */
public class TrophyFeeder extends StoryFeeder
{
    /**
     * Initializes a listener for trophy events to automatically invoke the publish dialog. Note
     * the feed popup is only shown if the event was manually generated rather than whenever a
     * trophy is earned. This latter behavior would result in slow facebook popups interrupting the
     * user's game.
     */
    public static void listen ()
    {
        FlashEvents.addListener(new TrophyEvent.Listener() {
            public void trophyEarned (TrophyEvent event) {
                // only do the manual requests since otherwise the popup will interrupt the game 
                if (event.isManualPublish()) {
                    new TrophyFeeder(event).publish();
                }
            }
        });
    }

    /**
     * Creates a new feeder for the given trophy event.
     */
    protected TrophyFeeder (TrophyEvent event)
    {
        super(new StoryKey(CShell.getAppId(), FacebookService.TROPHY,
            new FacebookGame(event.getGameId())), PUB_IMAGES);
        _event = event;
    }

    @Override // from StoryFeeder
    protected String getIdent ()
    {
        return _event.getTrophyIdent();
    }

    @Override // from StoryFeeder
    protected void addMoreWildcards (Map<String, Object> data)
    {
        // add in the game specific stuff we got from the event
        data.put("game_id", _event.getGameId());
        data.put("game", _event.getGame());
        data.put("game_desc", _event.getGameDescription());
        data.put("trophy", _event.getTrophy());
        data.put("descrip", _event.getDescription());
    }

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game_id" : 827,
          "game" : "Corpse Craft",
          "game_desc" :
              "Build an army of corpses to destroy your foes in this puzzle-action hybrid.",
          "trophy" : "Freshman",
          "vector" : "v.none",
          "action_url": "http://www.whirled.com/go/games-d_827",
          "fbuid" : "loggedinuser",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */

    protected TrophyEvent _event;

    protected static final String PUB_IMAGES[] = {
        FacebookUtil.PUB_ROOT + "88f92c972a7e088dfb71787a37ea3bbb3fed27ce.png",
        FacebookUtil.PUB_ROOT + "c4fa25346e8ba5773fdd4d0b263b322405fe3eef.png",
        FacebookUtil.PUB_ROOT + "02d2576b5ea2ef0c07f96712344678b6354fa475.png" };
}
