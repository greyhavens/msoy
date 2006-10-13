//
// $Id$

package client.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.client.WebContext;

/**
 * Displays all items of a particular type in the catalog.
 */
public class ItemPanel extends DockPanel
{
    public ItemPanel (WebContext ctx, byte type)
    {
        // setStyleName("inventory_item");
        _ctx = ctx;
        _type = type;

        _itemContents = new FlowPanel();
        _itemContents.setStyleName("catalog_contents");
        _tagContents = new FlowPanel();
        _tagContents.setStyleName("tag_contents");
        updateContents();

        Button button = new Button("Items/Tags");
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                _tags = !_tags;
                updateContents();
            }
        });
        add(button, DockPanel.NORTH); 
        add(_status = new Label(""), DockPanel.SOUTH);
    }

    protected void updateContents ()
    {
        if (_tags) {
            remove(_itemContents);
            add(_tagContents, DockPanel.CENTER);
        } else {
            remove(_tagContents);
            add(_itemContents, DockPanel.CENTER);
        }
    }
    
    
    protected void onLoad ()
    {
        // load the catalog if we haven't one already
        if (_listings == null) {
            _ctx.catalogsvc.loadCatalog(_ctx.creds, _type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _listings = (ArrayList)result;
                    if (_listings == null || _listings.size() == 0) {
                        _itemContents.add(new Label(
                            "There are no " + Item.getTypeName(_type) + " items listed."));
                    } else {
                        for (int ii = 0; ii < _listings.size(); ii++) {
                            _itemContents.add(new ItemContainer(
                                (CatalogListing) _listings.get(ii), ItemPanel.this));
                            }
                    }
                }
                public void onFailure (Throwable caught) {
                    GWT.log("loadCatalog failed", caught);
                    // TODO: if ServiceException, translate
                    _itemContents.add(new Label("Failed to load catalog."));
                }
            });
        }
        if (_sortedTags == null) {
            _ctx.catalogsvc.getPopularTags(_ctx.creds, _type, 20, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _tagMap = (HashMap) result;
                    if (_tagMap.size() == 0) {
                        _tagContents.add(new Label("No tags in use."));
                        return;
                    }

                    // figure out the highest use count among all the tags
                    Iterator vIter = _tagMap.values().iterator();
                    _maxTagCount = 0;
                    while (vIter.hasNext()) {
                        int count = ((Integer) vIter.next()).intValue();
                        if (count > _maxTagCount) {
                            _maxTagCount = count;
                        }
                    }

                    // then sort the tag names
                    _sortedTags =  _tagMap.keySet().toArray();
                    Arrays.sort(_sortedTags);
                    StringBuffer buf = new StringBuffer();
                    for (int ii = 0; ii < _sortedTags.length; ii ++) {
                        String tag = (String) _sortedTags[ii];
                        int count = ((Integer)_tagMap.get(tag)).intValue();
                        double rate = ((double) count) / _maxTagCount;
                        // let's start with just 4 different tag sizes
                        int size = 1+(int)(4 * rate);
                        buf.append("<span class='tagSize" + size + "'>");
                        buf.append(tag);
                        buf.append("</span>");
                        buf.append("&nbsp; . &nbsp;");
                    }
                    
                    _tagContents.add(new Label(
                        "The " + _tagMap.size() + " most commonly used tags:"));
                    HTML tags = new HTML(buf.toString());
                    tags.setHorizontalAlignment(HTML.ALIGN_CENTER);
                    _tagContents.add(tags);
                }
                public void onFailure (Throwable caught) {
                    GWT.log("getPopularTags failed", caught);
                    // TODO: if ServiceException, translate
                    _itemContents.add(new Label("Failed to get popular tags."));
                }
            });

        }
    }

    protected void purchaseItem (ItemIdent item)
    {
        _ctx.catalogsvc.purchaseItem(_ctx.creds, item, new AsyncCallback() {
            public void onSuccess (Object result) {
                setStatus("Item purchased.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                setStatus("Item creation failed: " + reason);
            }
        });
    }

    /**
     * Displays a status message to the user, may be called by item editors.
     */
    protected void setStatus (String status)
    {
        _status.setText(status);
    }

    protected WebContext _ctx;
    protected byte _type;

    protected boolean _tags;
    protected FlowPanel _itemContents;
    protected FlowPanel _tagContents;
    protected Label _status;

    protected ArrayList _listings;

    protected int _maxTagCount;
    protected Object[] _sortedTags;
    protected HashMap _tagMap;
}
