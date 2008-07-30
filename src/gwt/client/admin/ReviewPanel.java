//
// $Id$

package client.admin;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.item.gwt.ItemDetail;

import client.ui.RowPanel;
import client.ui.ThumbBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * An interface for dealing with flagged items: mark them mature if they were flagged thus,
 * or delete them, or simply remove the flags.
 */
public class ReviewPanel extends FlowPanel
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
        if (_contents != null) {
            remove(_contents);
        }
        add(_contents = new FlexTable());
        _adminsvc.getFlaggedItems(CAdmin.ident, 10, new MsoyCallback<List<ItemDetail>>() {
            public void onSuccess (List<ItemDetail> items) {
                populateUI(items);
            }
        });
    }

    // builds the UI from the given list
    protected void populateUI (List<ItemDetail> items)
    {
        if (items.size() == 0) {
            _contents.setText(0, 0, CAdmin.msgs.reviewNoItems());
            return;
        }

        for (ItemDetail detail : items) {
            int row = _contents.getRowCount();
            _contents.setWidget(row, 0, new ThumbBox(detail.item.getThumbnailMedia(), null));
            _contents.setWidget(row, 1, new ReviewItem(this, detail));
            _contents.getFlexCellFormatter().setStyleName(row, 1, "Item");
            _contents.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
        }
    }

    protected FlexTable _contents;

    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
