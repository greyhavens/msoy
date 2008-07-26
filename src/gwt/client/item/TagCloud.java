//
// $Id$

package client.item;

import java.util.Arrays;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;

import client.item.ItemMessages;
import client.shell.CShell;
import client.util.MsoyUI;
import client.util.ServiceUtil;

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

    public TagCloud (byte type, int count, TagListener listener)
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
            add(MsoyUI.createActionLabel(tag, "Link", new ClickListener() {
                public void onClick (Widget widget) {
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

    protected byte _type;
    protected TagListener _listener;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
