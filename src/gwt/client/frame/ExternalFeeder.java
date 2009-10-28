//
// $Id$

package client.frame;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.gwt.ArgNames.FBParam;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;

import client.facebookbase.FacebookUtil;
import client.shell.CShell;
import client.util.InfoCallback;
import client.util.JavaScriptUtil;
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
                // only do the manual requests since otherwise the popup will interrupt the game 
                if (event.isManualPublish()) {
                    publishTrophyToFacebook(event);
                }
            }
        });
    }

    protected void publishTrophyToFacebook (final TrophyEvent event)
    {
        _fbsvc.getTrophyStoryFields(
            CShell.getAppId(), event.getGameId(), new InfoCallback<StoryFields>() {
            @Override public void onSuccess (StoryFields result) {
                if (result != null) {
                    publishTrophyToFacebook(event, result);
                } // else oops
            }
        });
    }

    protected void publishTrophyToFacebook (TrophyEvent event, StoryFields fields)
    {
        String vector = fields.template.toEntryVector();
        String templateId = String.valueOf(fields.template.bundleId);

        // we use this url in 3 places on the post
        // TODO: link different things to different places? more redirects in FacebookServlet?
        String actionURL = SharedNaviUtil.buildRequest(
            FacebookUtil.getCanvasUrl(fields.canvasName),
            CookieNames.AFFILIATE, String.valueOf(CShell.getMemberId()),
            FBParam.GAME.name, String.valueOf(event.getGameId()), FBParam.VECTOR.name, vector,
            FBParam.TRACKING.name, fields.trackingId);

        FacebookUtil.FeedStoryImages images = new FacebookUtil.FeedStoryImages(PUB_IMAGES);
        for (String thumbnail : fields.thumbnails) {
            images.add(thumbnail, actionURL);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("game_id", event.getGameId());
        data.put("game", event.getGame());
        data.put("game_desc", event.getGameDescription());
        data.put("trophy", event.getTrophy());
        data.put("descrip", event.getDescription());
        data.put("action_url", actionURL);
        data.put("vector", vector);
        data.put("images", images.toArray());
        data.put("fbuid", "" + String.valueOf(fields.fbuid));

        publishTrophy(templateId, event.getGameId(), event.getTrophyIdent(), fields.trackingId,
            JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Called by facebook.js when the trophy feed publish dialog is closed. There is no guarantee
     * that the user actually chose to do it.
     */
    protected void trophyPublished (
        int gameId, String trophyIdent, String trackingId, String postId)
    {
        if (postId != null && !postId.equals("null")) {
            _fbsvc.trophyPublished(
                CShell.getAppId(), gameId, trophyIdent, trackingId, new AsyncCallback<Void>() {
                @Override public void onFailure (Throwable caught) {
                    CShell.log("Failed to contact server for trophy published", caught);
                }
                @Override public void onSuccess (Void result) {
                }
            });
        }
    }

    protected native void publishTrophy (
        String templateId, int gameId, String ident, String trackingId, JavaScriptObject data) /*-{
        var object = this;
        $wnd.FB_PostTrophy(templateId, data, function (postid, exception, data) {
            object.@client.frame.ExternalFeeder::trophyPublished(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)(gameId, ident, trackingId, postid);
        });
    }-*/;

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

    public static final String PUB_IMAGES[] = {
        FacebookUtil.PUB_ROOT + "88f92c972a7e088dfb71787a37ea3bbb3fed27ce.png",
        FacebookUtil.PUB_ROOT + "c4fa25346e8ba5773fdd4d0b263b322405fe3eef.png",
        FacebookUtil.PUB_ROOT + "02d2576b5ea2ef0c07f96712344678b6354fa475.png" };

    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
