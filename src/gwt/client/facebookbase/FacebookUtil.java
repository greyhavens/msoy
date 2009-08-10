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

    /**
     * Manages the creation of an array of feed story images.
     */
    public static class FeedStoryImages
    {
        /**
         * Adds a new image to the array.
         * @param altPublicImageUrl for convenience, this url (if not null) is swapped in for the
         * real URL if the deployment does not have {@link #IS_MEDIA_ACCESSIBLE}.
         */
        public void add (String imageUrl, String actionUrl, String altPublicImageUrl)
        {
            if (!IS_MEDIA_ACCESSIBLE && altPublicImageUrl != null) {
                imageUrl = altPublicImageUrl;
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
