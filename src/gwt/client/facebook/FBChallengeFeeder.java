//
// $Id$

package client.facebook;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;

import client.facebookbase.FacebookUtil;
import client.util.JavaScriptUtil;
import client.util.Link;

/**
 * Pops up a game challenge feed story publisher.
 */
public class FBChallengeFeeder
{
    /**
     * Creates a new feeder to publish a challenge for the given game.
     */
    public FBChallengeFeeder (FacebookGame game)
    {
        _game = game;
    }

    /**
     * Pops up a challenge feed story confirmation using the given game and story fields.
     */
    public void publish (StoryFields result)
    {
        String vector = result.template.toEntryVector("challenge");
        String templateId = String.valueOf(result.template.bundleId);

        // action link goes to either the Whirled game detail or the Mochi embed
        String actionURL = SharedNaviUtil.buildRequest(
            FacebookUtil.APP_CANVAS, _game.getCanvasArgs());
        actionURL = SharedNaviUtil.buildRequest(actionURL, ArgNames.FBParam.VECTOR.name, vector);

        FacebookUtil.FeedStoryImages images = new FacebookUtil.FeedStoryImages();
        images.add(result.thumbnailURL, actionURL, ACCESSIBLE_GAME_IMAGE);

        // TODO: A/B test use of target ids
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("game", result.name);
        data.put("game_desc", result.description);
        data.put("action_url", actionURL);
        data.put("images", images.toArray());

        publishChallenge(templateId, JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Callback after the feed form is submitted or cancelled.
     */
    protected void onCompletion ()
    {
        // go back to playing the game
        Link.go(_game.getPlayPage(), _game.getPlayArgs());
    }

    protected native void publishChallenge (String templateId, JavaScriptObject data) /*-{
        var object = this;
        $wnd.FB_PostChallenge(templateId, data, function () {
            object.@client.facebook.FBChallengeFeeder::onCompletion()();
        });
    }-*/;

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game" : "Corpse Craft",
          "game_desc" :
              "Build an army of corpses to destroy your foes in this puzzle-action hybrid.",
          "action_url": "http://www.whirled.com/go/games-d_827",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */

    protected FacebookGame _game;

    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
    protected static final String ACCESSIBLE_GAME_IMAGE =
        FacebookUtil.PUB_ROOT + "708ca91490155abc18f99a74e8bba5129b5033f6.png"; // CC game thumb
}
