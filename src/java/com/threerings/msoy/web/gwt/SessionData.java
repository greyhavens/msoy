//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Contains a snapshot of the user's data delivered when they validate their session.
 */
public class SessionData implements IsSerializable
{
    public static int TOP_WHIRLEDS = 6;

    /** Identifies A/B groups assigned to sessions during the validation process. */
    public enum Group { NONE, A, B, C };

    /**
     * Optional data for displaying more progress information for Facebook.
     */
    public static class Extra
        implements IsSerializable
    {
        /** Accumulated flow. */
        public int accumFlow;

        /** Accumulated flow to reach SessionData.level. */
        public int levelFlow;

        /** Accumulated flow to reach SessionData.level + 1. */
        public int nextLevelFlow;

        /** Total number of trophies held. */
        public int trophyCount;

        /** Flow awarded for this session. */
        public int flowAwarded;

        /** Levels gained since the last daily visit. */
        public int levelsGained;
    }

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

    /** Registered user's visitor info structure. */
    public VisitorInfo visitor;

    /** A/B test group associated with this session, or NONE. */
    public Group group = Group.NONE;

    /** The id of the theme this user logged into, or 0. */
    public int themeId;

    /** Data about the top themes in the Whirled, JSON-encoded. */
    public String topThemes;

    /** Optional data for displaying more progress information (facebook). */
    public Extra extra;

    /** The optional embedding to force this client into. */
    public Embedding embedding;

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
        sdata.visitor = VisitorInfo.unflatten(data);
        sdata.group = Group.valueOf(data.next());
        sdata.themeId = Integer.valueOf(data.next());
        sdata.topThemes = data.next();

        // Note the extra is not included in flattened state
        return sdata;
    }

    /**
     * Flattens this instance into a list of strings we can send between frames in the GWT app.
     */
    public List<String> flatten ()
    {
        List<String> data = Lists.newArrayList();
        data.addAll(creds.flatten());
        data.add(String.valueOf(flow));
        data.add(String.valueOf(gold));
        data.add(String.valueOf(level));
        data.add(String.valueOf(newMailCount));
        data.addAll(visitor.flatten());
        data.add(String.valueOf(group));
        data.add(String.valueOf(themeId));
        data.add(topThemes);

        // Note the extra is not included in flattened state
        return data;
    }
}
