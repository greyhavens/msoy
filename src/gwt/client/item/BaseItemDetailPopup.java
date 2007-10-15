//
// $Id$

package client.item;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.util.BorderedDialog;
import client.util.CreatorLabel;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.PopupMenu;
import client.util.TagDetailPanel;

/**
 * Defines the base item detail popup from which we derive an inventory and catalog item detail.
 */
public class BaseItemDetailPopup extends BorderedDialog
{
    protected BaseItemDetailPopup (Item item)
    {
        super(true);
        _item = item;

        // create our user interface
        _header.add(_name = createTitleLabel(item.name, "itemDetailName"));
        // this is a goddamned hack, but GWT doesn't support valign=baseline, dooh!
        DOM.setStyleAttribute(DOM.getParent(_name.getElement()), "verticalAlign", "baseline");

        // configure our item preview
        ((FlexTable)_contents).setWidget(0, 0, createPreview(item));

        // allow derived classes to add their own nefarious bits
        createInterface(_details, _controls);

        // add our tag business at the bottom
        _footer.add(new TagDetailPanel(new TagDetailPanel.TagService() {
            public void tag (String tag, AsyncCallback callback) {
                CItem.itemsvc.tagItem(CItem.ident, _item.getIdent(), tag, true, callback);
            } 
            public void untag (String tag, AsyncCallback callback) {
                CItem.itemsvc.tagItem(CItem.ident, _item.getIdent(), tag, false, callback);
            }
            public void getRecentTags (AsyncCallback callback) {
                CItem.itemsvc.getRecentTags(CItem.ident, callback);
            }
            public void getTags (AsyncCallback callback) {
                CItem.itemsvc.getTags(CItem.ident, _item.getIdent(), callback);
            }
            public boolean supportFlags () {
                return true;
            }
            public void setFlags (final byte flag, final Label statusLabel) {
                CItem.itemsvc.setFlags(CItem.ident, _item.getIdent(), flag, flag,
                                       new AsyncCallback () {
                    public void onSuccess (Object result) {
                        _item.flagged |= flag;
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

        // load up the item details
        CItem.itemsvc.loadItemDetail(CItem.ident, _item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                gotDetail(_detail = (ItemDetail)result);
                center();
            }
            public void onFailure (Throwable caught) {
                _description.setText(CItem.serverError(caught));
            }
        });
    }

    protected Widget createContents ()
    {
        FlexTable middle = new FlexTable();
        middle.setStyleName("itemDetailContent");

        // a place for the item's preview visualization
        middle.getFlexCellFormatter().setStyleName(0, 0, "itemDetailPreview");
        middle.getFlexCellFormatter().setRowSpan(0, 0, 2);

        // a place for details
        middle.setWidget(0, 1, _details = new VerticalPanel());
        middle.getFlexCellFormatter().setVerticalAlignment(0, 1, VerticalPanel.ALIGN_TOP);
        _details.setStyleName("itemDetailDetails");

        // a place for controls
        middle.setWidget(1, 0, _controls = new VerticalPanel());
        middle.getFlexCellFormatter().setVerticalAlignment(1, 0, VerticalPanel.ALIGN_BOTTOM);
        _controls.setStyleName("itemDetailControls");
        return middle;
    }

    protected Widget createPreview (Item item)
    {
        MediaDesc preview = item.getPreviewMedia();
        if (item instanceof Avatar) {
            // special avatar viewer: TODO: only display in catalog / inventory
            // and not for 3rd parties?
            return FlashClients.createAvatarViewer(
                preview.getMediaPath(), ((Avatar) item).scale, false);

        } else if (preview.isWhirledVideo()) {
            return FlashClients.createVideoViewer(preview.getMediaPath());
         
        } else {
            return MediaUtil.createMediaView(preview, MediaDesc.PREVIEW_SIZE);
        }
    }

    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        details.add(_creator = new CreatorLabel());
        details.add(_description = new Label(ItemUtil.getDescription(_item)));
    }

    protected void gotDetail (ItemDetail detail)
    {
        _creator.setMember(detail.creator);
        if (_item.isRatable()) {
            _details.add(new ItemRating(detail.item, CItem.getMemberId(), detail.memberRating));
        }
    }

    protected Item _item;
    protected ItemDetail _detail;

    protected VerticalPanel _details, _controls;
    protected Label _name, _description;
    protected CreatorLabel _creator;
}
