//
// $Id$

package client.facebook;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;

import client.facebookbase.FacebookUtil;
import client.shell.CShell;
import client.util.JavaScriptUtil;
import client.util.Link;

/**
 * Pops up a game challenge feed story publisher.
 */
public class FBChallengeFeeder
{
    /**
     * Creates a new feeder to publish a challenge for the given game and story fields.
     */
    public FBChallengeFeeder (FacebookGame game, StoryFields fields)
    {
        _game = game;
        _fields = fields;
    }

    /**
     * Pops up a challenge feed story confirmation using the values given in the constructor.
     */
    public void publish ()
    {
        String vector = _fields.template.toEntryVector();
        String templateId = String.valueOf(_fields.template.bundleId);

        // action link goes to either the Whirled game detail or the Mochi embed
        String actionURL = SharedNaviUtil.buildRequest(
            FacebookUtil.getCanvasUrl(_fields.canvasName), _game.getCanvasArgs());
        actionURL = SharedNaviUtil.buildRequest(actionURL,
            CookieNames.AFFILIATE, String.valueOf(CShell.getMemberId()),
            ArgNames.FBParam.VECTOR.name, vector,
            ArgNames.FBParam.TRACKING.name, _fields.trackingId);

        FacebookUtil.FeedStoryImages images = new FacebookUtil.FeedStoryImages(PUB_IMAGES);
        for (String thumbnail : _fields.thumbnails) {
            images.add(thumbnail, actionURL);
        }

        // TODO: A/B test use of target ids
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("game", _fields.name);
        data.put("game_desc", _fields.description);
        data.put("action_url", actionURL);
        data.put("images", images.toArray());
        data.put("fbuid", "" + String.valueOf(_fields.fbuid));

        publishChallenge(templateId, JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Callback after the feed form is submitted or cancelled.
     */
    protected void onCompletion (String postId)
    {
        if (postId != null) {
            _fbsvc.challengePublished(
                CShell.getAppId(), _game, _fields.trackingId, new AsyncCallback<Void>() {
                @Override public void onFailure (Throwable caught) {
                    goBackToGame();
                }
                @Override public void onSuccess (Void result) {
                    goBackToGame();
                }
            });
        }
    }

    protected void goBackToGame ()
    {
        Link.go(_game.getPlayPage(), _game.getPlayArgs());
    }

    protected native void publishChallenge (String templateId, JavaScriptObject data) /*-{
        var object = this;
        $wnd.FB_PostChallenge(templateId, data, function (postid, exception, data) {
            object.@client.facebook.FBChallengeFeeder::onCompletion(Ljava/lang/String;)(postid);
        });
    }-*/;

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game" : "Corpse Craft",
          "fbuid" : "loggedinuser",
          "game_desc" :
              "Build an army of corpses to destroy your foes in this puzzle-action hybrid.",
          "action_url": "http://www.whirled.com/go/games-d_827",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */

    protected FacebookGame _game;
    protected StoryFields _fields;

    public static final String PUB_IMAGES[] = {
        FacebookUtil.PUB_ROOT + "db21cc504a1f735f96eb051275c1dd9d394924d2.png",
        FacebookUtil.PUB_ROOT + "e59ac92a6c497f573fcee451dedb59ce03cadf68.png",
        FacebookUtil.PUB_ROOT + "af525e4e1b026a46beeaf98ff824aa65e30d800a.png" };

    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
