//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.ui.HeaderBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class SearchPanel extends FlowPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 3;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public SearchPanel ()
    {
        setStyleName("searchPanel");
        add(_ctrls = new SearchControls());
    }

    public void setArgs (Args args)
    {
        final int page = args.get(1, 0);
        final String query = args.get(2, "");
        _ctrls.setSearch(query);

        // if we're already showing this search, page through it
        if (showingResultsFor(query)) {
            displayPage(page);
            return;
        }

        clearResults();

        if (query.length() > 0) {
            _profilesvc.findProfiles(query, new MsoyCallback<List<MemberCard>>() {
                public void onSuccess (List<MemberCard> members) {
                    setResults(members, page, query);
                }
            });
        }
    }

    public void clearResults ()
    {
        while (getWidgetCount() > 1) {
            remove(getWidget(1));
        }
        _members = null;
        _searchString = null;
    }

    public void setResults (List<MemberCard> cards, int page, String search)
    {
        _searchString = search;
        _members = new MemberList(_msgs.searchResultsNoMatch(search));
        add(new HeaderBox(_msgs.searchResultsTitle(search), _members));
        _members.setModel(new SimpleDataModel<MemberCard>(cards), page);
    }

    public void displayPage (int page)
    {
        if (_members != null) {
            _members.displayPage(page, false);
        }
    }

    public boolean showingResultsFor (String search)
    {
        return _searchString != null && _searchString.equals(search);
    }

    protected SearchControls _ctrls;
    protected MemberList _members;
    protected String _searchString;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ProfileServiceAsync _profilesvc = (ProfileServiceAsync)
        ServiceUtil.bind(GWT.create(ProfileService.class), ProfileService.ENTRY_POINT);
}
