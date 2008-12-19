//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A generic class for awards so that awards of new flavors can be brought onto the Passport
 * page from other parts of the codebase.
 */
public class Award
    implements IsSerializable
{
    public enum AwardType {
        BADGE, MEDAL;
    }

    /**
     * The unique id of this award, if there is one.
     */
    public int awardId;

    /**
     * The type of this award.
     */
    public AwardType type;

    /**
     * The name of this award
     */
    public String name;

    /**
     * The description for this award.
     */
    public String description;

    /**
     * The icon media for this award.
     */
    public MediaDesc icon;

    /**
     * When this award was earned.  If this Award represents something that has not yet been
     * earned, this field will contain 0;
     */
    public long whenEarned;
}
