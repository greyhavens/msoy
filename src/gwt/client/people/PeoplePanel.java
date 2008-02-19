//
// $Id$

package client.people;

import com.threerings.gwt.ui.SmartTable;

/**
 * The main landing page for the "people" section.
 */
public class PeoplePanel extends SmartTable
{
    public PeoplePanel ()
    {
        super("peoplePanel", 0, 5);
        setWidget(0, 0, new SearchControls());
    }
}
