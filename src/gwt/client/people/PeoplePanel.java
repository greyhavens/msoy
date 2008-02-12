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

        int row = 0;
        setText(row++, 0, "Find Friends", 1, "Title");
        setWidget(row++, 0, new SearchControls());

        setText(row++, 0, "Invite Friends", 1, "Title");
        setText(row++, 0, "Foo!");
    }
}
