//
// $Id$

package client.admin;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.RowPanel;

/**
 * An interface for dealing with flagged items: mark them mature if they were flagged thus,
 * or delete them, or simply remove the flags.
 */
public class ReviewPanel extends FlexTable
{
    public ReviewPanel ()
    {
        setStyleName("reviewPanel");

        RowPanel buttons = new RowPanel();
        Button reloadButton = new Button(CAdmin.msgs.reviewReload());
        reloadButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        });
        buttons.add(reloadButton);
        add(buttons);

        refresh();
    }

    // clears the UI and repopuplates the list
    protected void refresh ()
    {
        clear();
        CAdmin.itemsvc.getFlaggedItems(CAdmin.ident, 10, new MsoyCallback() {
            public void onSuccess (Object result) {
                populateUI((List) result);
            }
        });
    }

    // builds the UI from the given list
    protected void populateUI (List list)
    {
        clear();
        if (list.size() == 0) {
            setText(0, 0, CAdmin.msgs.reviewNoItems());
            return;
        }

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            ItemDetail detail = (ItemDetail) iter.next();
            int row = getRowCount();
            setWidget(row, 0, MediaUtil.createMediaView(
                          detail.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE));
            setWidget(row, 1, new ReviewItem(this, detail));
        }
    }
}
