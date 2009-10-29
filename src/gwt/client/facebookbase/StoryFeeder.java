//
// $Id$

package client.facebookbase;

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
import com.threerings.msoy.facebook.gwt.FacebookService.StoryKey;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;

import client.facebookbase.FacebookUtil;
import client.shell.CShell;
import client.util.InfoCallback;
import client.util.JavaScriptUtil;

/**
 * Handles the bulk of the work for posting a story to the user's Facebook feed. Instances are
 * alive only as long as needed to complete a single post. A javascript bridge is utilized to
 * perform the actual interaction with Facebook javascript. The source for this bridge is in
 * rsrc/js/facebook.js.tmpl.
 * TODO: this currently uses the deprecated showFeedDialog, change to streamPublish
 */
public class StoryFeeder
{
    /**
     * Posts a story to the user's Facebook feed. First queries the server for the story fields
     * matching the requested code (e.g. "trophy" or "challenge").
     */
    public void publish ()
    {
        _fbsvc.getStoryFields(_key, new InfoCallback<StoryFields>() {
            @Override public void onSuccess (StoryFields result) {
                if (result != null) {
                    _fields = result;
                    doPublish();
                } // else oops
            }
        });
    }

    /**
     * Creates a new story feeder. Subclasses will normally pass in a literal code and a static
     * final array of public images.
     * @param key the story key to publish
     * @param publicImages 3 hard-wired image urls to use on local deployments - without them
     *        Facebook will not publish your story
     */
    protected StoryFeeder (StoryKey key, String[] publicImages)
    {
        _key = key;
        _publicImages = publicImages;
    }

    /**
     * Performs the publish. Populates wildcards and action url and invokes the native publish
     * method.
     */
    protected void doPublish ()
    {
        String vector = _fields.template.toEntryVector();
        String templateId = String.valueOf(_fields.template.bundleId);

        // we use this url in 3 places on the post
        // TODO: link different things to different places? more redirects in FacebookServlet?
        String actionURL = SharedNaviUtil.buildRequest(
            FacebookUtil.getCanvasUrl(_fields.canvasName),
            CookieNames.AFFILIATE, String.valueOf(CShell.getMemberId()),
            FBParam.VECTOR.name, vector,
            FBParam.TRACKING.name, _fields.trackingId);

        if (_key.game != null) {
            actionURL = SharedNaviUtil.buildRequest(actionURL, _key.game.getCanvasArgs());
        }

        actionURL = addMoreParameters(actionURL);

        FacebookUtil.FeedStoryImages images = new FacebookUtil.FeedStoryImages(_publicImages);
        for (String thumbnail : _fields.thumbnails) {
            images.add(thumbnail, actionURL);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("action_url", actionURL);
        data.put("vector", vector);
        data.put("images", images.toArray());
        data.put("fbuid", "" + String.valueOf(_fields.fbuid));
        addMoreWildcards(data);

        doPublish(templateId, JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Adds additional parameters to the clickthrough url for the feed post. The base version just
     * returns the url with no modifications. Subclasses may need to add their own specific
     * parameters.
     */
    protected String addMoreParameters (String url)
    {
        return url;
    }

    /**
     * Adds additional wildcards to the template data associative array. For convenience this is
     * done with a map. The base version does nothing. Subclasses may need to add their own
     * specific wildcards.
     */
    protected void addMoreWildcards (Map<String, Object> data)
    {
    }

    /**
     * Retrieves the optional identity associated with the post. In addition to the story key which
     * contains the application and the type of story and an optional game, this provides
     * additional scoping within the game of code. For example a trophy identity.
     */
    protected String getIdent ()
    {
        return null;
    }

    /**
     * Notifies the instance that the feed post is all finished. This occurs after the server has
     * been informed for tracking purposes.
     */
    protected void onComplete (boolean success)
    {
    }

    /**
     * Called by facebook.js when the feed dialog is closed. The post id is only used to test if
     * the post was successful or skipped.
     */
    protected void publishCallback (String postId)
    {
        if (postId != null && !postId.equals("null")) {
            _fbsvc.trackStoryPosted(
                _key, getIdent(), _fields.trackingId, new AsyncCallback<Void>() {
                @Override public void onFailure (Throwable caught) {
                    CShell.log("Failed to contact server for tracking story published", caught);
                    onComplete(false);
                }
                @Override public void onSuccess (Void result) {
                    onComplete(true);
                }
            });
        }
    }

    /**
     * Invokes the javascript bridge code (source in {code facebook.js.tmpl}) to popup the facebook
     * feed dialog using the given template id and wildcard data.
     */
    protected native void doPublish (String templateId, JavaScriptObject data) /*-{
        var object = this;
        $wnd.FB_PostStory(templateId, data, function (postid, exception, data) {
            object.@client.facebookbase.StoryFeeder::publishCallback(Ljava/lang/String;)(postid);
        });
    }-*/;

    /** The kind of story we are posting. */
    protected StoryKey _key;

    /** Images to use on local deployments (otherwise Facebook will not allow testing there). */
    protected String[] _publicImages;

    /** The fields to use when populating the wildcards. */
    protected StoryFields _fields;

    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
