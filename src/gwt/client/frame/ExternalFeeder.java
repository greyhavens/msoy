//
// $Id$

package client.frame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.DeploymentConfig;

import com.threerings.msoy.web.gwt.FacebookTemplateCard;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

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
                publishTrophyToFacebook(event);
            }
        });
    }

    protected void publishTrophyToFacebook (final TrophyEvent event)
    {
        _membersvc.getFacebookTemplate("trophy", new InfoCallback<FacebookTemplateCard>() {
            @Override public void onSuccess (FacebookTemplateCard result) {
                if (result != null) {
                    CShell.log("Got template, publishing", "bundle", result.bundleId,
                        "variant", result.variant);
                    publishTrophyToFacebook(event, result);
                } // else oops
            }
        });
    }

    protected void publishTrophyToFacebook (TrophyEvent event, FacebookTemplateCard template)
    {
        String vector = template.toEntryVector("trophy");
        String templateId = String.valueOf(template.bundleId);

        // we use it in 3 places, but they all just go to the game detail screen on facebook
        // TODO: link different things to different places? more redirects in FacebookServlet?
        String actionURL =
            DeploymentConfig.facebookCanvasUrl + "?game=" + event.getGameId() + "&vec=" + vector;

        List<Object> images = new ArrayList<Object>();
        if (event.getGameMediaURL() != null) {
            images.add(createImage(event.getGameMediaURL(), actionURL));
        }
        images.add(createImage(event.getMediaURL(), actionURL));

        setPublicImages(images);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("game_id", event.getGameId());
        data.put("game", event.getGame());
        data.put("game_desc", event.getGameDescription());
        data.put("trophy", event.getTrophy());
        data.put("descrip", event.getDescription());
        data.put("action_url", actionURL);
        data.put("vector", vector);
        data.put("images", JavaScriptUtil.createArray(images));

        publishTrophy(templateId, event.getGameId(), event.getTrophyIdent(),
            JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Creates a JSNI dictionary referring to the given image source and href.
     */
    protected JavaScriptObject createImage (String src, String href)
    {
        Map<String, Object> image = new HashMap<String, Object>();
        image.put("src", src);
        image.put("href", href);
        return JavaScriptUtil.createDictionaryFromMap(image);
    }

    /**
     * Swaps in some images with arbitrary public URLs here if this is a dev deployment that does
     * not use media.whirled.com.
     */
    protected void setPublicImages (List<Object> images)
    {
        String pubRoot = "http://media.whirled.com/";
        if (!DeploymentConfig.devDeployment || DeploymentConfig.mediaURL.equals(pubRoot)) {
            return;
        }

        String pubImages[] = {
            pubRoot + "708ca91490155abc18f99a74e8bba5129b5033f6.png",  // CC game thumb
            pubRoot + "240aa9267fa6dc8422588e6818862301fd658e6f.png"}; // CC Freshman trophy

        if (images.size() == 2) {
            // game and trophy images
            setImageSrc(images, 0, pubImages[0]);
            setImageSrc(images, 1, pubImages[1]);

        } else if (images.size() == 1) {
            // trophy only
            setImageSrc(images, 0, pubImages[1]);
        }        
    }

    /**
     * Swap in some images with arbitrary public URLs here to satisfy Facebook's validation.
     */
    protected void setImageSrc (List<Object> images, int idx, String url)
    {
        JavaScriptObject jsobj = (JavaScriptObject)images.get(idx);
        JavaScriptUtil.setDictionaryEntry(jsobj, "src", url);
    }

    /**
     * Called by facebook.js when the trophy feed publish dialog is closed. There is no guarantee
     * that the user actually chose to do it.
     */
    protected void trophyPublished (int gameId, String trophyIdent)
    {
        _membersvc.trophyPublishedToFacebook(gameId, trophyIdent, new AsyncCallback<Void>() {
            @Override public void onFailure (Throwable caught) {
                CShell.log("Failed to contact server for trophy published", caught);
            }
            @Override public void onSuccess (Void result) {
            }
        });
    }

    protected native void publishTrophy (
        String templateId, int gameId, String ident, JavaScriptObject data) /*-{
        var trophyPublished = this.@client.frame.ExternalFeeder::trophyPublished(ILjava/lang/String;);
        $wnd.FB_PostTrophy(templateId, data, function () {
            trophyPublished(gameId, ident);
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
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */

    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
