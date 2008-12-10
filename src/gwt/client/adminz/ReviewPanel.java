//
// $Id$

package client.adminz;

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
        Button reloadButton = new Button(_msgs.reviewReload());
        reloadButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        });
        buttons.add(reloadButton);
        add(buttons);

        refresh();
    }

    public ItemDetail getItemDetail (int id)
    {
        for (ItemDetail detail : _items) {
            if (detail.item.itemId == id) {
                return detail;
            }
        }
        return null;
    }

    // clears the UI and repopuplates the list
    protected void refresh ()
    {
        if (_contents != null) {
            remove(_contents);
        }
        add(_contents = new FlexTable());
        _adminsvc.getFlaggedItems(10, new MsoyCallback<List<ItemDetail>>() {
            public void onSuccess (List<ItemDetail> items) {
                _items = items;
                populateUI();
            }
        });
    }

    // builds the UI from the given list
    protected void populateUI ()
    {
        if (_items.size() == 0) {
            _contents.setText(0, 0, _msgs.reviewNoItems());
            return;
        }

        for (ItemDetail detail : _items) {
            int row = _contents.getRowCount();
            _contents.setWidget(row, 0, new ThumbBox(detail.item.getThumbnailMedia()));
            _contents.setWidget(row, 1, new ReviewItem(this, detail));
            _contents.getFlexCellFormatter().setStyleName(row, 1, "Item");
            _contents.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
        }
    }

    protected FlexTable _contents;
    protected List<ItemDetail> _items;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
