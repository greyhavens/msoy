//
// $Id$

package client.people;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.ProfileService;

import client.util.TongueBox;

/**
 * Contains a chunk of content that a user would want to display on their personal page.
 */
public abstract class Blurb extends TongueBox
{
    /**
     * Returns true if we should display this blurb, false if we should skip it.
     */
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    /**
     * Configures this blurb with a context and the member id for whom it is displaying content.
     */
    public void init (ProfileService.ProfileResult pdata)
    {
        _name = pdata.name;
    }

    protected Blurb ()
    {
    }

    protected MemberName _name;
}
