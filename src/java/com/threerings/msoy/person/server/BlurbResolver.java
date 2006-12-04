//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.web.data.BlurbData;

import static com.threerings.msoy.Log.log;

/**
 * Handles the asynchronous resolution of data for a particular person page
 * blurb.
 */
public abstract class BlurbResolver
{
    /**
     * Creates the appropriate resolver for the specified blurb data.
     */
    public static BlurbResolver create (BlurbData data)
    {
        switch (data.type) {
        case BlurbData.PROFILE:
            return new ProfileResolver();
        case BlurbData.FRIENDS:
            return new FriendsResolver();
        case BlurbData.GROUPS:
            return new GroupsResolver();
        default:
            log.warning("Requested resolver for unknown blurb " + data + ".");
            return null;
        }
    }

    public void resolve (int memberId, BlurbData data, int index, int count,
        HashIntMap<Object> results, ResultListener<ArrayList<Object>> listener)
    {
        _memberId = memberId;
        _data = data;
        _index = index;
        _count = count;
        _results = results;
        _listener = listener;
        resolve();
    }

    protected abstract void resolve ();

    protected void resolutionCompleted (Object result)
    {
        // stuff our results in the table
        _results.put(_index, result);

        // if we were the last one to the party, ship the results off to the
        // listener
        if (_results.size() == _count+1) {
            ArrayList<Object> rlist = new ArrayList<Object>(_count+1);
            rlist.add(_results.get(-1)); // add the layout
            for (int ii = 0; ii < _count; ii++) {
                rlist.add(_results.get(ii));
                _listener.requestCompleted(rlist);
            }
        }
    }

    protected void resolutionFailed (Exception cause)
    {
        log.log(Level.WARNING, "Failed to resolve blurb data " +
            "[type=" + getClass().getName() + ", who=" + _memberId +
            "]", cause);
        resolutionCompleted(new BlurbData.ResolutionFailure(MsoyCodes.INTERNAL_ERROR));
    }

    protected int _memberId;
    protected BlurbData _data;
    protected int _index, _count;
    protected HashIntMap<Object> _results;
    protected ResultListener<ArrayList<Object>> _listener;
}
