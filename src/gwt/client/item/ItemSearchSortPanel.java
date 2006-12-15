/**
 * 
 */
package client.item;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.threerings.msoy.item.web.CatalogListing;

public class ItemSearchSortPanel extends HorizontalPanel
{
    public static interface Listener {
        public void searchStringUpdated (String newText);
        
        public void sortCriteriumUpdated (byte newCriterium);
    }
    public byte sortBy;
    public String search;


    public ItemSearchSortPanel (Listener listener)
    {
        _listener = listener;
        
        setStyleName("itemSearchPanel");
        
        TextBox searchBox = new TextBox();
        searchBox.setStyleName("itemSearchBox");
        add(searchBox);

        Button searchButton = new Button("search");
        add(searchButton);
        
        Label sortLabel = new Label("Sort by:");
        sortLabel.setStyleName("itemSortLabel");
        add(sortLabel);
        
        ListBox sortBox = new ListBox();
        sortBox.setStyleName("itemSortBox");
        sortBox.addItem("Rating", String.valueOf(CatalogListing.SORT_BY_RATING));
        sortBox.addItem("Listed Date", String.valueOf(CatalogListing.SORT_BY_LIST_DATE));
        add(sortBox);
    }
    
    protected Listener _listener;

}