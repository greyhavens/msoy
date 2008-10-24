//
// $Id$

package com.threerings.msoy.data;

import java.util.Date;

import com.samskivert.util.ObjectUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.presents.dobj.DSet;

public class MemberExperience
    implements DSet.Entry, Comparable<MemberExperience>
{
    public /* final */ Long dateOccurred;
    public /* final */ byte action;
    public /* final */ Object data;

    public MemberExperience ()
    {
    }

    public MemberExperience (Date dateOccurred, byte action, Object data)
    {
        this.dateOccurred = dateOccurred.getTime();
        this.action = action;
        this.data = data;
    }
    
    public Date getDateOccurred ()
    {
        return new Date(dateOccurred);
    }
    
    public HomePageItem getHomePageItem (MediaDesc media, String name)
    {
        return new HomePageItem(action, data, media, name);
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
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
        return (this.action == that.action) && ObjectUtil.equals(this.data, that.data) &&
            ObjectUtil.equals(this.dateOccurred, that.dateOccurred);
    }

    public int compareTo (MemberExperience o)
    {
        if (action == o.action) {
            if (getDateOccurred().equals(o.getDateOccurred())) {
                String str1 = (data == null ? null : data.toString());
                String str2 = (o.data == null ? null : o.data.toString());
                return (str1 == null && str2 == null) ? 0 :
                    (str1 == null ? -1 : str1.compareTo(str2));
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
