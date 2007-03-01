//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.PagedGrid;

/**
 * Displays an interface for searching for members.
 */
public class SearchMembersPanel extends PagedGrid
{
    public SearchMembersPanel ()
    {
        super(1, MATCH_COLUMNS);

        addToHeader(new Label(CProfile.msgs.search()));
        addToHeader(_search = new TextBox());
    }

    protected TextBox _search;

    protected static final int MATCH_COLUMNS = 5;
}
