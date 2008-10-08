//
// $Id$

package com.threerings.msoy.data {

public class UberClientModes
{
    /**
     * Mode constants. Guess what? These can never change. They're referenced
     * externally _all_over_the_place_ (as in, possibly out on the interwebs).
     */
    public static const CLIENT :int = 0;
    public static const FEATURED_PLACE :int = 1;

    public static const STUB :int = 10; // we're loaded from an external site

    public static const AVATAR_VIEWER :int = 100;
    public static const PET_VIEWER :int = 101;
    public static const FURNI_VIEWER :int = 102;
    public static const TOY_VIEWER :int = 103;
    public static const DECOR_VIEWER :int = 104;
    public static const DECOR_EDITOR :int = 105;

    public static const GENERIC_VIEWER :int = 199;
}
}
