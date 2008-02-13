//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.util.SimpleDataModel;

import client.shell.Args;
import client.shell.Frame;
import client.util.MsoyCallback;

public class SearchPanel extends VerticalPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 3;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public SearchPanel ()
    {
        setStyleName("searchPanel");
        setWidth("100%");
        setSpacing(5);
        Frame.setTitle(CPeople.msgs.profileSearchTitle());
        add(_ctrls = new SearchControls());
    }

    public void setArgs (Args args)
    {
        final int page = args.get(1, 0);
        final String query = args.get(2, "");

        _ctrls.setSearch(query);

        if (query.length() == 0) {
            clearResults();

        } else if (!showingResultsFor(query)) {
            CPeople.profilesvc.findProfiles(query, new MsoyCallback() {
                public void onSuccess (Object result) {
                    setResults((List) result, page, query);
                }
            });

        } else {
            displayPage(page);
        }
    }

    public void clearResults ()
    {
        if (_profiles != null) {
            remove(_profiles);
            _profiles = null;
            _searchString = null;
        }
    }

    public void setResults (List cards, int page, String search)
    {
        _searchString = search;
        clearResults();
        add(_profiles = new ProfileGrid(ROWS, COLUMNS, ProfileGrid.NAV_ON_TOP,
                                        CPeople.msgs.gridNoProfiles()));
        _profiles.setModel(new SimpleDataModel(cards), page);
    }

    public void displayPage (int page)
    {
        if (_profiles != null) {
            _profiles.displayPage(page, false);
        }
    }

    public boolean showingResultsFor (String search)
    {
        return _searchString != null && _searchString.equals(search);
    }

    protected SearchControls _ctrls;
    protected ProfileGrid _profiles;
    protected String _searchString;
}
