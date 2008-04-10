//
// $Id$

package com.threerings.msoy.person.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a particular interest of a member.
 */
public class Interest implements IsSerializable
{
    /** An interest {@link #type} code. */
    public static int ACTIVITIES = 1;

    /** An interest {@link #type} code. */
    public static int INTERESTS = 2;

    /** An interest {@link #type} code. */
    public static int GAMES = 3;

    /** An interest {@link #type} code. */
    public static int MUSIC = 4;

    /** An interest {@link #type} code. */
    public static int MOVIES = 5;

    /** An interest {@link #type} code. */
    public static int SHOWS = 6;

    /** An interest {@link #type} code. */
    public static int BOOKS = 7;

    /** An interest {@link #type} code. */
    public static int RANDOM = 8;

    /** Enumerates all interest types in display order. */
    public static final int[] TYPES = {
        ACTIVITIES, INTERESTS, GAMES, MUSIC, MOVIES, SHOWS, BOOKS, RANDOM };

    /** The maximum length of a single interest. */
    public static final int MAX_INTEREST_LENGTH = 2048;

    /** The type of interest, e.g. {@link #ACTIVITIES}. */
    public int type;

    /** A user-provided string describing their interests. */
    public String interests;
}
