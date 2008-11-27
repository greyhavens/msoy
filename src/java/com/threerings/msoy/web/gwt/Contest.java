//
// $Id: Promotion.java 13403 2008-11-20 01:41:58Z mdb $

package com.threerings.msoy.web.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * A contest as displayed on the contests page.
 */
public class Contest
    implements IsSerializable
{
    public static final int MAX_BLURB_LENGTH = 2000;
    public static final int MAX_PRIZES_LENGTH = 1000;
    public static final int MAX_PASTBLURB_LENGTH = 1000;

    /** This contest's unique identifier. */
    public String contestId;

    /** The contest's optional icon (a thumbnail size image). */
    public MediaDesc icon;

    /** HTML for the name of the contest, optionally linking to another page */
    public String name;

    /** The html text of the contest blurb. */
    public String blurb;

    /** The html text of the contest status. */
    public String status;

    /** The html text of the contest prizes. */
    public String prizes;

    /** HTML to display beside the name under "past contests" after the end date is reached. */
    public String pastBlurb;

    /** The time at which this contest starts. */
    public Date starts;

    /** The time at which this contest ends. */
    public Date ends;
}
