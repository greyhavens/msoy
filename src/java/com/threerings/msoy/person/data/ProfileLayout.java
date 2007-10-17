//
// $Id$

package com.threerings.msoy.person.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Defines the layout of a profile page (which blurbs are on the page and in which order).
 */
public class ProfileLayout implements IsSerializable
{
    /** One of the available profile page layout formats. */
    public static final int ONE_COLUMN_LAYOUT = 0;

    /** One of the available profile page layout formats. */
    public static final int TWO_COLUMN_LAYOUT = 1;

    /** The layout format of this page. */
    public int layout;

    /** Arbitrary layout information interpreted by the layout code. */
    public String layoutData;

    /**
     * {@link BlurbData} records for every blurb on this page.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.BlurbData>
     */
    public List blurbs;
}
