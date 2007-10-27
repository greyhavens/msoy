//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import client.shell.CShell;

public class ItemSearchSortPanel extends HorizontalPanel
{
    /** An interface to use for instantiating an ItemSearchSortPanel. */
    public static interface Listener
    {
        /** This method is called when the user hits enters while on the search field, or clicks
         * the search button. */
        public void search (String query);

        /** This method is called when the user selects a sort criterium. */
        public void sort (byte sortBy);
    }

    public ItemSearchSortPanel (Listener listener, String[] sortNames, byte[] sortValues)
    {
        setStyleName("itemSearchPanel");
        _listener = listener;
        _sortValues = sortValues;

        _searchBox = new TextBox();
        _searchBox.setVisibleLength(20);
        _searchBox.addStyleName("itemSearchBox");
        ClickListener clickListener = new ClickListener() {
            public void onClick (Widget sender) {
                _listener.search(_searchBox.getText());
            }
        };
        _searchBox.addKeyboardListener(new EnterClickAdapter(clickListener));
        add(_searchBox);

        Button searchButton = new Button(CShell.imsgs.searchSearch());
        searchButton.addClickListener(clickListener);
        add(searchButton);

        Label sortLabel = new Label(CShell.imsgs.searchSortBy());
        sortLabel.setStyleName("itemSortLabel");
        add(sortLabel);

        _sortBox = new ListBox();
        _sortBox.addStyleName("itemSortBox");
        for (int ii = 0; ii < sortNames.length; ii ++) {
            _sortBox.addItem(sortNames[ii]);
        }
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                _listener.sort(_sortValues[((ListBox)widget).getSelectedIndex()]);
            }
        });
        add(_sortBox);
    }

    /**
     * Configures the search text.
     */
    public void setSearch (String text)
    {
        _searchBox.setText(text);
    }

    /**
     * Clear the search box.
     */
    public void clearSearchBox ()
    {
        _searchBox.setText("");
    }

    /**
     * Configures the selected sort type.
     */
    public void setSelectedSort (byte sortType)
    {
        for (int ii = 0; ii < _sortValues.length; ii++) {
            if (_sortValues[ii] == sortType) {
                _sortBox.setSelectedIndex(ii);
            }
        }
    }

    protected Listener _listener;
    protected byte[] _sortValues;

    protected TextBox _searchBox;
    protected ListBox _sortBox;
}
