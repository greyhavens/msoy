//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Defines the layout of a person page (which blurbs are on the page and in
 * which order).
 */
public class PersonLayout implements IsSerializable
{
    /** One of the available person page layout formats. */
    public static final int ONE_COLUMN_LAYOUT = 0;

    /** One of the available person page layout formats. */
    public static final int TWO_COLUMN_LAYOUT = 1;

    /** The layout format of this page. */
    public int layout;

    /** Arbitrary layout information interpreted by the layout code. */
    public String layoutData;

    /** {@link BlurbData} records for every blurb on this page. */
    public ArrayList blurbs;
}
