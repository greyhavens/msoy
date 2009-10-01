//
// $Id$

package client.facebookbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.msoy.data.all.DeploymentConfig;

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

    /** Game thumbnails shown on dev deployments. */
    public static final String ACCESSIBLE_GAME_IMAGES[] = {
        PUB_ROOT + "708ca91490155abc18f99a74e8bba5129b5033f6.png", // CC
        PUB_ROOT + "be846a2fe35910c14007710f41d9b8976f57cec0.png", // brawler
        PUB_ROOT + "68b7869c610d2d17c9d5530f100af08e9fcf7de8.png", // qbeez
        PUB_ROOT + "b2fe48847324f1c1fc4a219bf8fc425261afe3e5.png" }; // ghosthunters

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
     * Manages the creation of an array of feed story images.
     */
    public static class FeedStoryImages
    {
        public FeedStoryImages ()
        {
            this(ACCESSIBLE_GAME_IMAGES);
        }

        public FeedStoryImages (String[] accessibleImages)
        {
            _accessibleImages = accessibleImages;
        }

        /**
         * Adds a new image to the array.
         * @param altPublicImageUrl for convenience, this url (if not null) is swapped in for the
         * real URL if the deployment does not have {@link #IS_MEDIA_ACCESSIBLE}.
         */
        public void add (String imageUrl, String actionUrl)
        {
            if (!IS_MEDIA_ACCESSIBLE && _accessibleImages != null) {
                imageUrl = _accessibleImages[_images.size() % _accessibleImages.length];
            }
            Map<String, Object> image = new HashMap<String, Object>();
            image.put("src", imageUrl);
            image.put("href", actionUrl);
            _images.add(JavaScriptUtil.createDictionaryFromMap(image));
        }

        /**
         * Converts to an array for passing into native java script.
         */
        public JavaScriptObject toArray ()
        {
            return JavaScriptUtil.createArray(_images);
        }

        protected ArrayList<Object> _images = new ArrayList<Object>();
        protected String[] _accessibleImages;
    }

    /**
     * Creates a JSNI dictionary referring to the given image source and href.
     */
    protected static JavaScriptObject createImage (String src, String href)
    {
        Map<String, Object> image = new HashMap<String, Object>();
        image.put("src", src);
        image.put("href", href);
        return JavaScriptUtil.createDictionaryFromMap(image);
    }
}
