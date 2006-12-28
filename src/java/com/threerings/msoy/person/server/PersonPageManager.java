//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.PersonLayout;

import com.threerings.msoy.person.server.persist.PersonPageRepository;

/**
 * Handles the collection of all person page blurb data.
 */
public class PersonPageManager
{
    /**
     * Configures us with our repository.
     */
    public void init (PersonPageRepository pprepo)
    {
        _pprepo = pprepo;
    }

    /**
     * Loads up all person page information for the specified member and
     * resolves the underlying blurb data for each of the blurbs on their page.
     *
     * <p> The returned array list contains a {@link PersonLayout} object in
     * the first position and blurb-specific objects in subsequent positions
     * (which can be decoded by looking at the {@link PersonLayout#blurbs}
     * array). If any particular blurb resolution failed, the list slot in
     * question will contain a translatable error string describing the
     * failure.
     */
    public void loadPersonPage (
        int memberId, ResultListener<ArrayList<Object>> listener)
    {
        // check the cache for their page layout information
        PersonLayout layout = _layoutCache.get(memberId);
        if (layout != null) {
            resolveBlurbData(memberId, layout, listener);
            return;
        }

        // for now fake it
        layout = new PersonLayout();
        layout.layout = PersonLayout.TWO_COLUMN_LAYOUT;

        ArrayList<BlurbData> blurbs = new ArrayList<BlurbData>();
        BlurbData blurb = new BlurbData();
        blurb.type = BlurbData.PROFILE;
        blurb.blurbId = 0;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.FRIENDS;
        blurb.blurbId = 1;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.GROUPS;
        blurb.blurbId = 2;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.HOOD;
        blurb.blurbId = 3;
        blurbs.add(blurb);

        layout.blurbs = blurbs;
        resolveBlurbData(memberId, layout, listener);

        // TODO: load their layout information from the repository, then
        // resolve it
    }

    /**
     * Called once we have our page layout to resolve the data for the
     * individual blurbs.
     */
    protected void resolveBlurbData (int memberId, PersonLayout layout,
        ResultListener<ArrayList<Object>> listener)
    {
        // create resolvers for each of the individual blurbs, they'll take
        // care of the rest
        HashIntMap<Object> resolved = new HashIntMap<Object>();
        resolved.put(-1, layout);
        for (int ii = 0, ll = layout.blurbs.size(); ii < ll; ii++) {
            BlurbData bdata = (BlurbData)layout.blurbs.get(ii);
            BlurbResolver resolver = BlurbResolver.create(bdata);
            if (resolver == null) {
                // an error will already have been logged and the client knows
                // to check for a string error message in the result list
                resolved.put(ii, MsoyCodes.INTERNAL_ERROR);
            } else {
                resolver.resolve(memberId, bdata, ii, ll, resolved, listener);
            }
        }
    }

    /** Handles persistent stuff. */
    protected PersonPageRepository _pprepo;

    /** A cache of person page layout information keyed on member id. */
    protected SoftCache<Integer,PersonLayout> _layoutCache =
        new SoftCache<Integer,PersonLayout>();
}
