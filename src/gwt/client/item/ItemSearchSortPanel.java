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

    public ItemSearchSortPanel (Listener listener, String[] sortNames, final byte[] sortValues,
                                int selectedSortIndex)
    {
        setStyleName("itemSearchPanel");
        _listener = listener;

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

        Button searchButton = new Button(CItem.imsgs.searchSearch());
        searchButton.addClickListener(clickListener);
        add(searchButton);

        Label sortLabel = new Label(CItem.imsgs.searchSortBy());
        sortLabel.setStyleName("itemSortLabel");
        add(sortLabel);

        ListBox sortBox = new ListBox();
        sortBox.addStyleName("itemSortBox");
        for (int ii = 0; ii < sortNames.length; ii ++) {
            sortBox.addItem(sortNames[ii]);
        }
        sortBox.setSelectedIndex(selectedSortIndex);
        sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                _listener.sort(sortValues[((ListBox)widget).getSelectedIndex()]);
            }
        });
        add(sortBox);
    }

    /**
     * Clear the search box.
     */
    public void clearSearchBox ()
    {
        _searchBox.setText("");
    }
    
    protected TextBox _searchBox;
    protected Listener _listener;
}
