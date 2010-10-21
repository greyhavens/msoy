//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains information on a promotion.
 */
public class Promotion
    implements IsSerializable
{
    /** This promotion's unique identifier. */
    public String promoId;

    /** The promotion's optional icon (a thumbnail size image). */
    public MediaDesc icon;

    /** This promotion's (HTML) blurb. */
    public String blurb;

    /** The time at which this promotion starts. */
    public Date starts;

    /** The time at which this promotion ends. */
    public Date ends;
}
