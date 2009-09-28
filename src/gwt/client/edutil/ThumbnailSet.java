//
// $Id$

package client.edutil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.threerings.msoy.facebook.gwt.FeedThumbnail;

/**
 * Organizes a mixed list of game thumbnails and provides convenient access by type and
 * variant.
 */
public class ThumbnailSet
{
    /**
     * Creates a new thumbnail set.
     */
    public ThumbnailSet (List<FeedThumbnail> allThumbnails)
    {
        for (FeedThumbnail thumb : allThumbnails) {
            get(thumb.code, thumb.variant).add(thumb);
        }
    }

    /**
     * Gets a list of thumbnails for a given type and variant, creating the list if it does
     * not exist. Modifications will take effect.
     */
    public List<FeedThumbnail> get (String code, String variant)
    {
        Map<String, List<FeedThumbnail>> variants = getVariantMap(code);
        List<FeedThumbnail> thumbs = variants.get(variant);
        if (thumbs == null) {
            variants.put(variant, thumbs = new ArrayList<FeedThumbnail>());
        }
        return thumbs;
    }

    /**
     * Gets a map of variant to thumbnail list for a given type, creating the map if it does
     * not exist. Modifications will take effect.
     */
    public Map<String, List<FeedThumbnail>> getVariantMap (String code)
    {
        Map<String, List<FeedThumbnail>> variants = _organized.get(code);
        if (variants == null) {
            _organized.put(code, variants = new HashMap<String, List<FeedThumbnail>>());
            return variants;
        }
        return variants;
    }

    /**
     * Gets a sorted list of variants present in the set for the given type.
     */
    public List<String> getVariants (String code)
    {
        List<String> variants = new ArrayList<String>();
        variants.addAll(getVariantMap(code).keySet());
        Collections.sort(variants);
        return variants;
    }

    protected Map<String, Map<String, List<FeedThumbnail>>> _organized =
        new HashMap<String, Map<String, List<FeedThumbnail>>>();
}
