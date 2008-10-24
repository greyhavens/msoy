//
// $Id$

package com.threerings.msoy.data;

import java.util.Date;

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
    public boolean equals (Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MemberExperience other = (MemberExperience)obj;
        if (action != other.action)
            return false;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (dateOccurred == null) {
            if (other.dateOccurred != null)
                return false;
        } else if (!dateOccurred.equals(other.dateOccurred))
            return false;
        return true;
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
