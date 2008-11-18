//
// $Id$

package com.threerings.msoy.data;

import java.util.Date;

import com.samskivert.util.ObjectUtil;

import com.threerings.presents.dobj.DSet;

/**
 * Represents an experience the user had in Whirled, such as visiting a game, group, or room.
 *
 * This class is immutable except during serialization.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MemberExperience
    implements DSet.Entry, Comparable<MemberExperience>
{
    /** Date/time this experience occurred. */
    public /* final */ Long dateOccurred;

    /** Action that the user has taken, one of the {@link HomePageItem} ACTION constants. */
    public /* final */ byte action;

    /** Data associated with the action, usually an ID of the place visited.
     * See {@link HomePageItem}. */
    public /* final */ int data;

    public MemberExperience ()
    {
        // For serialization.
    }

    public MemberExperience (Date dateOccurred, byte action, int data)
    {
        this.dateOccurred = dateOccurred.getTime();
        this.action = action;
        this.data = data;
    }

    public Date getDateOccurred ()
    {
        return new Date(dateOccurred);
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + data;
        result = prime * result + ((dateOccurred == null) ? 0 : dateOccurred.hashCode());
        return result;
    }

    @Override
    public boolean equals (Object other)
    {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MemberExperience that = (MemberExperience)other;
        return (this.action == that.action) && (this.data == that.data) &&
            ObjectUtil.equals(this.dateOccurred, that.dateOccurred);
    }

    public int compareTo (MemberExperience o)
    {
        if (action == o.action) {
            if (getDateOccurred().equals(o.getDateOccurred())) {
                return data == o.data ? 0 : (data < o.data ? -1 : 1);
            }
            return getDateOccurred().compareTo(o.getDateOccurred());
        }
        return action < o.action ? -1 : 1;

    }

    public Comparable<?> getKey ()
    {
        return this;
    }
}
