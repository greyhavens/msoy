//
// $Id$

package client.edutil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.msoy.facebook.gwt.FeedThumbnail;

/**
 * Organizes a mixed list of feed thumbnails and provides convenient access by code and
 * variant.
 */
public class ThumbnailSet
{
    /**
     * Creates a new empty set.
     */
    public ThumbnailSet ()
    {
    }

    /**
     * Creates a new thumbnail set with the given contents.
     */
    public ThumbnailSet (List<FeedThumbnail> thumbnails)
    {
        addAll(thumbnails);
    }

    /**
     * Adds all of the given thumbnails to this set.
     */
    public void addAll (List<FeedThumbnail> thumbnails)
    {
        for (FeedThumbnail thumb : thumbnails) {
            add(thumb);
        }
    }

    /**
     * Adds the given thumbnail to this set.
     */
    public void add (FeedThumbnail thumbnail)
    {
        get(thumbnail.code, thumbnail.variant).add(thumbnail);
        _deepSize++;
    }

    /**
     * Extracts the contents of this set as a list.
     */
    public List<FeedThumbnail> toList ()
    {
        List<FeedThumbnail> thumbnails = Lists.newArrayList();
        for (Map<String, List<FeedThumbnail>> vmap : _organized.values()) {
            for (List<FeedThumbnail> thumbs : vmap.values()) {
                thumbnails.addAll(thumbs);
            }
        }
        return thumbnails;
    }

    /**
     * Gets a list of thumbnails for a given code and variant, creating the list if it does
     * not exist. The resulting list should not be modified.
     */
    public List<FeedThumbnail> get (String code, String variant)
    {
        Map<String, List<FeedThumbnail>> variants = getVariantMap(code);
        List<FeedThumbnail> thumbs = variants.get(variant);
        if (thumbs == null) {
            variants.put(variant, thumbs = Lists.newArrayList());
        }
        return thumbs;
    }

    /**
     * Gets a map of variant to thumbnail list for a given code, creating the map if it does
     * not exist. The resulting map should not be modified.
     */
    public Map<String, List<FeedThumbnail>> getVariantMap (String code)
    {
        Map<String, List<FeedThumbnail>> variants = _organized.get(code);
        if (variants == null) {
            _organized.put(code, variants = Maps.newHashMap());
            return variants;
        }
        return variants;
    }

    /**
     * Gets a sorted list of codes present in the set.
     */
    public List<String> getCodes ()
    {
        List<String> codes = Lists.newArrayList();
        codes.addAll(_organized.keySet());
        Collections.sort(codes);
        return codes;
    }

    /**
     * Gets a sorted list of variants present in the set for the given code.
     */
    public List<String> getVariants (String code)
    {
        List<String> variants = Lists.newArrayList();
        variants.addAll(getVariantMap(code).keySet());
        Collections.sort(variants);
        return variants;
    }

    /**
     * Removes all thumbnails with the given code and variant from this set.
     */
    public boolean remove (String code, String variant)
    {
        Map<String, List<FeedThumbnail>> vmap = _organized.get(code);
        if (vmap != null) {
            List<FeedThumbnail> thumbs = vmap.get(variant);
            if (thumbs != null) {
                vmap.remove(variant);
                _deepSize -= thumbs.size();
                if (vmap.size() == 0) {
                    _organized.remove(code);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this set is empty.
     */
    public boolean isEmpty ()
    {
        return _deepSize == 0;
    }

    protected int _deepSize;
    protected Map<String, Map<String, List<FeedThumbnail>>> _organized =
        Maps.newHashMap();
}
