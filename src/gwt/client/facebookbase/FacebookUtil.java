//
// $Id$

package client.facebookbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.gwt.FacebookService;

import client.util.JavaScriptUtil;

/**
 * Provides some static methods and classes for doing facebook things to multiple modules.
 */
public class FacebookUtil
{
    /** The base url of public whirled media (basically determines if we're running on dev). */
    public static final String PUB_ROOT = "http://media.whirled.com/";

    /** Whether or not the media in this deployment is publicly accessible. This is important
     * because Facebook will not publish feed stories with firewalled media. */
    public static final boolean IS_MEDIA_ACCESSIBLE =
        !DeploymentConfig.devDeployment || DeploymentConfig.mediaURL.equals(PUB_ROOT);

    /**
     * Get the url of an application's profile, given its id.
     */
    public static String getProfileUrl (long fbAppId)
    {
        return "http://www.facebook.com/apps/application.php?id=" + fbAppId;
    }

    /**
     * Get the url of an application, given its canvas name.
     */
    public static String getCanvasUrl (String canvasName)
    {
        return "http://apps.facebook.com/" + canvasName + "/";
    }

    /**
     * Creates an array of feed story images, in javascript form for passing into Facebook.
     * @param urls the src attributes of the images, usually on the media server
     * @param actionUrl the href attribute of the images, currently all images go to the same url
     * @param pubImages images to use if running on a local test server; if the images passed to
     *        facebook are not public, the feed story will not post
     * @param setType whether to also set the "type" attribute to "image" for each item        
     */
    public static JavaScriptObject makeImages (
        List<String> urls, String actionUrl, String[] pubImages, boolean setType)
    {
        ArrayList<Object> images = new ArrayList<Object>();
        for (String url : urls) {
            if (!IS_MEDIA_ACCESSIBLE && pubImages != null) {
                url = pubImages[images.size() % pubImages.length];
            }
            Map<String, Object> image = new HashMap<String, Object>();
            if (setType) {
                image.put("type", "image");
            }
            image.put("src", url);
            image.put("href", actionUrl);
            images.add(JavaScriptUtil.createDictionaryFromMap(image));
        }
        return JavaScriptUtil.createArray(images);
    }

    /**
     * Creates an array of one feed story link, in javascript form for passing into Facebook.
     * @param text the text of the link
     * @param actionUrl the href of the link
     */
    public static JavaScriptObject makeLinks (String text, String actionUrl)
    {
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("text", text);
        link.put("href", actionUrl);
        ArrayList<Object> links = new ArrayList<Object>();
        links.add(JavaScriptUtil.createDictionaryFromMap(link));
        return JavaScriptUtil.createArray(links);
    }

    public static String getPossessivePronoun (FacebookService.Gender gender, boolean capitalize)
    {
        switch (gender) {
        case FEMALE: return _msgs.xlate("possessiveHer");
        case MALE: return _msgs.xlate("possessiveHis");
        case NEUTRAL: return _msgs.xlate("possessiveNeutral");
        }
        return "";
    }

    protected static final FacebookBaseLookup _msgs = GWT.create(FacebookBaseLookup.class);
}
