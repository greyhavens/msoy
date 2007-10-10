//
// $Id$

package client.profile;

import java.util.List;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import com.threerings.gwt.util.SimpleDataModel;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;

public class SearchPanel extends VerticalPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 3;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public SearchPanel ()
    {
        setStyleName("searchResultsPanel");
        setWidth("100%");
        setSpacing(10);

        ClickListener goButtonListener = new ClickListener() {
            public void onClick (Widget sender) {
                String[] args =  {
                    "search", null, "0", URL.encodeComponent(_search.getText().trim()) };
                if (_radioName.isChecked()) {
                    args[1] = "name";
                } else if (_radioDisplayName.isChecked()) {
                    args[1] = "display";
                } else {
                    args[1] = "email";
                }
                Application.go(Page.PROFILE, Args.compose(args));
            }
        };

        FlexTable searchPanel = new FlexTable();
        searchPanel.addStyleName("SearchPanel");
        add(searchPanel);
        int row = 0;
        searchPanel.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        searchPanel.setText(row, 0, CProfile.msgs.searchType());
        searchPanel.setWidget(row, 1, _radioName = new RadioButton("searchType", 
            CProfile.msgs.searchRadioName()));
        searchPanel.setWidget(row, 2, _radioDisplayName = new RadioButton("searchType",
            CProfile.msgs.searchRadioDisplayName()));
        searchPanel.setWidget(row++, 3, _radioEmail = new RadioButton("searchType",
            CProfile.msgs.searchRadioEmail()));
        _radioName.setChecked(true);

        searchPanel.getFlexCellFormatter().setColSpan(row, 1, 3);
        searchPanel.setWidget(row, 1, _search = new TextBox());
        _search.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char charCode, int modifiers) {
                DeferredCommand.add(new Command() {
                    public void execute () {
                        _go.setEnabled(_search.getText().trim().length() != 0);
                    }
                });
            }
        });
        _search.addKeyboardListener(new EnterClickAdapter(goButtonListener));
        _search.setVisibleLength(40);
        searchPanel.setWidget(row++, 5, 
            _go = new Button(CProfile.msgs.searchGo(), goButtonListener));
        _go.setEnabled(false);
    }

    public void clearResults ()
    {
        if (_profiles != null) {
            remove(_profiles);
            _profiles = null;
            _searchString = null;
            _searchType = null;
        }
    }

    public void setResults (List cards, int page, String type, String search)
    {
        _searchType = type;
        _searchString = search;
        clearResults();
        add(_profiles = new ProfileGrid(
                ROWS, COLUMNS, ProfileGrid.NAV_ON_TOP, CProfile.msgs.gridNoProfiles()));
        _profiles.setModel(new SimpleDataModel(cards), page);
    }

    public void displayPage (int page) 
    {
        if (_profiles != null) {
            _profiles.displayPage(page, false);
        }
    }

    public boolean showingResultsFor (String type, String search)
    {
        return _searchType != null && _searchString != null && _searchType.equals(type) &&
            _searchString.equals(search);
    }

    // @Override // from Widget
    protected void onAttach ()
    {
        super.onAttach();
        _search.setFocus(true);
    }

    protected ProfileGrid _profiles;
    protected String _searchType, _searchString;
    protected RadioButton _radioName, _radioDisplayName, _radioEmail;
    protected TextBox _search;
    protected Button _go;
}
