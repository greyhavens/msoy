//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains id, name and population of a place.
 */
public class PlaceCard
    implements IsSerializable
{
    /** The place id. */
    public int placeId;

    /** The place's name. */
    public String name;

    /** The place's population (as of the last snapshot calculation). */
    public int population;
}
