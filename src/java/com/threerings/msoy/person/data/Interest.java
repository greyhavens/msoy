//
// $Id$

package com.threerings.msoy.person.data;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Interest implements IsSerializable
{
    public static int HOBBIES = 1;
    public static int TV_SHOWS = 2;
    public static int MOVIES = 3;
    public static int BOOKS = 4;

    public static final int[] TYPES = { HOBBIES, TV_SHOWS, MOVIES, BOOKS };

    /** The type of interest, e.g. {@link #HOBBIES}. */
    public int type;

    /** A user-provided string describing their interests. */
    public String interests;
}
