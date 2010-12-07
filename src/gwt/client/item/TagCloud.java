//
// $Id$

package client.item;

import java.util.Arrays;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 * Fetches and displays the tag cloud for a given item type.
 */
public class TagCloud extends FlowPanel
    implements AsyncCallback<Map<String, Integer>>
{
    public interface TagListener
    {
        /** Called when a tag has been clicked (or cleared if the argument is null). */
        void tagClicked (String tag);
    }

    public TagCloud (MsoyItemType type, int count, TagListener listener)
    {
        setStyleName("tagCloud");
        _type = type;
        _listener = listener;
        _catalogsvc.getPopularTags(_type, count, this);
    }

    // from AsyncCallback
    public void onSuccess (Map<String, Integer> tagMap)
    {
        if (tagMap.size() == 0) {
            add(MsoyUI.createLabel(_imsgs.msgNoTags(), "Link"));
            return;
        }

        // then sort the tag names
        Object[] sortedTags = tagMap.keySet().toArray();
        Arrays.sort(sortedTags);

        for (int ii = 0; ii < sortedTags.length; ii++) {
            final String tag = (String)sortedTags[ii];
            add(MsoyUI.createActionLabel(tag, "Link", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _listener.tagClicked(tag);
                }
            }));
        }
    }

    // from AsyncCallback
    public void onFailure (Throwable caught)
    {
        CShell.log("getPopularTags failed", caught);
        add(MsoyUI.createLabel(CShell.serverError(caught), "Link"));
    }

    protected MsoyItemType _type;
    protected TagListener _listener;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
}
