//
// $Id$

package client.item;

import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.MediaDesc;

import client.shell.Page;
import client.util.BorderedDialog;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.TagDetailPanel;

/**
 * Defines the base item detail popup from which we derive an inventory and catalog item detail.
 */
public abstract class BaseItemDetailPanel extends FlexTable
{
    protected BaseItemDetailPanel (ItemDetail detail)
    {
        setStyleName("itemDetailPanel");

        _detail = detail;
        _item = detail.item;

        // create our header
        int row = 0;
        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, _header = new HorizontalPanel());
        Image back = new Image("/images/item/inventory_up.png");
        back.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                returnToList();
            }
        });
        _header.add(back);
        _header.add(MsoyUI.createLabel(_item.name, "Name"));

        // configure our item preview
        getFlexCellFormatter().setStyleName(row, 0, "Preview");
        getFlexCellFormatter().setRowSpan(row, 0, 2);
        setWidget(row, 0, createPreview(_item));

        // a place for details
        setWidget(row, 1, _details = new VerticalPanel());
        getFlexCellFormatter().setVerticalAlignment(row++, 1, VerticalPanel.ALIGN_TOP);
        _details.setStyleName("Details");

        // a place for controls
        setWidget(row, 0, _controls = new VerticalPanel());
        getFlexCellFormatter().setVerticalAlignment(row++, 0, VerticalPanel.ALIGN_BOTTOM);
        _controls.setStyleName("Controls");

        // allow derived classes to add their own nefarious bits
        createInterface(_details, _controls);

        // add our tag business at the bottom
        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, _footer = new HorizontalPanel());
        _footer.add(new TagDetailPanel(new TagDetailPanel.TagService() {
            public void tag (String tag, AsyncCallback callback) {
                CItem.itemsvc.tagItem(CItem.creds, _item.getIdent(), tag, true, callback);
            } 
            public void untag (String tag, AsyncCallback callback) {
                CItem.itemsvc.tagItem(CItem.creds, _item.getIdent(), tag, false, callback);
            }
            public void getRecentTags (AsyncCallback callback) {
                CItem.itemsvc.getRecentTags(CItem.creds, callback);
            }
            public void getTags (AsyncCallback callback) {
                CItem.itemsvc.getTags(CItem.creds, _item.getIdent(), callback);
            }
            public boolean supportFlags () {
                return true;
            }
            public void setFlags (final byte flag, final Label statusLabel) {
                CItem.itemsvc.setFlags(CItem.creds, _item.getIdent(), flag, flag,
                                       new AsyncCallback () {
                    public void onSuccess (Object result) {
                        _item.flags |= flag;
                    }
                    public void onFailure (Throwable caught) {
                        CItem.log("Failed to update item flags [item=" + _item.getIdent() +
                                  ", flag=" + flag + "]", caught);
                        if (statusLabel != null) {
                            statusLabel.setText(CItem.serverError(caught));
                        }
                    }
                });
            }
            public void addMenuItems (String tag, PopupMenu menu) { }
        }));
    }

    protected Widget createPreview (Item item)
    {
        MediaDesc preview = item.getPreviewMedia();
        if (item instanceof Avatar) {
            // special avatar viewer: TODO: only display in catalog / inventory
            // and not for 3rd parties?
            return FlashClients.createAvatarViewer(preview.getMediaPath());

        } else if (preview.isVideo()) {
            return FlashClients.createVideoViewer(preview.getMediaPath());
         
        } else {
            return MediaUtil.createMediaView(preview, MediaDesc.PREVIEW_SIZE);
        }
    }

    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        details.add(_creator = new CreatorLabel());
        _creator.setMember(_detail.creator);
        details.add(_description = new Label(ItemUtil.getDescription(_item)));
        if (_item.isRatable()) {
            details.add(new ItemRating(_detail.item, _detail.memberRating));
        }
    }

    /**
     * Called when the user clicks the "up arrow" button next to the name to return to their
     * inventory or catalog listing.
     */
    protected abstract void returnToList ();

    protected Item _item;
    protected ItemDetail _detail;

    protected HorizontalPanel _header, _footer;
    protected VerticalPanel _details, _controls;
    protected Label _description;
    protected CreatorLabel _creator;
}
