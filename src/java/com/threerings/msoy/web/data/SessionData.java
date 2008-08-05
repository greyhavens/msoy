//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a snapshot of the user's data delivered when they validate their session.
 */
public class SessionData implements IsSerializable
{
    /** Our session credentials. */
    public WebCreds creds;

    /** This member's flow at the time of session start. */
    public int flow;

    /** This member's gold at the time of session start. */
    public int gold;

    /** This member's level at the time of session start. */
    public int level;

    /** This member's new mail message count at the time of session start. */
    public int newMailCount;

    /** If we've just created an account, this will be set to true. */
    public transient boolean justCreated;

    /**
     * Creates and initializes an instance from supplied {@link #flatten}ed string.
     */
    public static SessionData unflatten (Iterator<String> data)
    {
        if (data == null) {
            return null;
        }

        SessionData sdata = new SessionData();
        sdata.creds = WebCreds.unflatten(data);
        sdata.flow = Integer.valueOf(data.next());
        sdata.gold = Integer.valueOf(data.next());
        sdata.level = Integer.valueOf(data.next());
        sdata.newMailCount = Integer.valueOf(data.next());
        sdata.justCreated = Boolean.valueOf(data.next());
        return sdata;
    }

    /**
     * Flattens this data into a list of strings we can send between frames in the GWT app.
     */
    public List<String> flatten ()
    {
        List<String> data = new ArrayList<String>();
        data.addAll(creds.flatten());
        data.add(String.valueOf(flow));
        data.add(String.valueOf(gold));
        data.add(String.valueOf(level));
        data.add(String.valueOf(newMailCount));
        data.add(String.valueOf(justCreated));
        return data;
    }
}
