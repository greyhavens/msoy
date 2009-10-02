//
// $Id$

package client.frame;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import client.facebookbase.FacebookUtil;
import client.shell.CShell;
import client.util.JavaScriptUtil;
import client.util.NoopAsyncCallback;

/**
 * Pops up a level-up feed story publisher.
 * TODO: look into sharing code with FBChallengeFeeder and ExternalFeeder.
 */
public class LevelUpFeeder
{
    /**
     * Creates a new feeder to publish a levelup story for the given level and story fields.
     */
    public LevelUpFeeder (int level, StoryFields fields)
    {
        _level = level;
        _fields = fields;
    }

    /**
     * Pops up a level-up feed story confirmation using the values given in the constructor.
     */
    public void publish ()
    {
        String vector = _fields.template.toEntryVector();
        String templateId = String.valueOf(_fields.template.bundleId);

        // action link goes to the main canvas page
        String actionURL = SharedNaviUtil.buildRequest(
            FacebookUtil.getCanvasUrl(_fields.canvasName),
            CookieNames.AFFILIATE, String.valueOf(CShell.getMemberId()),
            ArgNames.FBParam.VECTOR.name, vector,
            ArgNames.FBParam.TRACKING.name, _fields.trackingId);

        FacebookUtil.FeedStoryImages images = new FacebookUtil.FeedStoryImages(PUB_IMAGES);
        for (String thumbnail : _fields.thumbnails) {
            images.add(thumbnail, actionURL);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("action_url", actionURL);
        data.put("images", images.toArray());
        data.put("level", "" + _level);
        data.put("fbuid", "" + String.valueOf(_fields.fbuid));

        publishLevelUp(templateId, JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Callback after the feed form is submitted or cancelled.
     */
    protected void onCompletion (String postId)
    {
        if (postId != null) {
            _fbsvc.levelUpPublished(
                CShell.getAppId(), _fields.trackingId, new NoopAsyncCallback());
        }
    }

    protected native void publishLevelUp (String templateId, JavaScriptObject data) /*-{
        var object = this;
        $wnd.FB_PostLevelUp(templateId, data, function (postid, exception, data) {
            object.@client.frame.LevelUpFeeder::onCompletion(Ljava/lang/String;)(postid);
        });
    }-*/;

    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "action_url": "http://apps.whirled.com/whirled/",
          "level": "19",
          "fbuid" : "loggedinuser",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://apps.whirled.com/whirled/"}]}
    */

    protected int _level;
    protected StoryFields _fields;

    public static final String PUB_IMAGES[] = {
        FacebookUtil.PUB_ROOT + "5e96a1dd2244f0679834e4d5e6ba886bffabd9ff.png",
        FacebookUtil.PUB_ROOT + "d540aa4605b1b778f826262209723359d81fa06e.png",
        FacebookUtil.PUB_ROOT + "7bb4a311f008782516ab77c994ac232357f99547.png" };

    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
