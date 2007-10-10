//
// $Id$

package client.item;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.shell.Application;
import client.shell.Args;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.PopupMenu;
import client.util.StyledTabPanel;
import client.util.TagDetailPanel;

/**
 * Defines the base item detail popup from which we derive an inventory and catalog item detail.
 */
public abstract class BaseItemDetailPanel extends FlexTable
{
    protected BaseItemDetailPanel (ItemDetail detail)
    {
        setStyleName("itemDetailPanel");
        setCellPadding(0);
        setCellSpacing(5);

        _detail = detail;
        _item = detail.item;

        Image back = new Image("/images/item/inventory_up.png");
        back.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                returnToList();
            }
        });
        getFlexCellFormatter().setStyleName(0, 0, "Back");
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        setWidget(0, 0, back);

        FlexTable box = new FlexTable();
        box.setStyleName("Box");
        box.setCellPadding(0);
        box.setCellSpacing(0);
        box.getFlexCellFormatter().setColSpan(0, 0, 2);
        box.getFlexCellFormatter().setStyleName(0, 0, "Name");
        box.setText(0, 0, _item.name);
        box.getFlexCellFormatter().setColSpan(1, 0, 2);
        box.getFlexCellFormatter().setStyleName(1, 0, "Preview");
        box.setWidget(1, 0, createPreview(_item));
        box.getFlexCellFormatter().setStyleName(2, 0, "Extras");
        box.setWidget(2, 0, _extras = new HorizontalPanel());
        box.getFlexCellFormatter().setStyleName(2, 1, "Buttons");
        box.setWidget(2, 1, _buttons = new HorizontalPanel());
        getFlexCellFormatter().setRowSpan(0, 1, 2);
        setWidget(0, 1, box);

        // a place for details
        setWidget(0, 2, _details = new VerticalPanel());
        getFlexCellFormatter().setVerticalAlignment(0, 2, VerticalPanel.ALIGN_TOP);
        _details.setStyleName("Details");
        _details.setSpacing(5);

        // allow derived classes to add their own nefarious bits
        createInterface(_details);

        // add our tag business at the bottom
        setWidget(1, 0, _footer = new HorizontalPanel());
        getFlexCellFormatter().setHeight(1, 0, "10px");
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

        configureCallbacks(this);
    }

    protected Widget createPreview (Item item)
    {
        MediaDesc preview = item.getPreviewMedia();
        if (item instanceof Avatar) {
            // special avatar viewer: TODO: only display in catalog / inventory
            // and not for 3rd parties?
            return FlashClients.createAvatarViewer(preview.getMediaPath(), ((Avatar) item).scale,
                allowAvatarScaleEditing());

        } else if (preview.isWhirledVideo()) {
            return FlashClients.createVideoViewer(preview.getMediaPath());
         
        } else {
            return MediaUtil.createMediaView(preview, MediaDesc.PREVIEW_SIZE);
        }
    }

    protected void createInterface (VerticalPanel details)
    {
        if (_item.isRatable()) {
            details.add(new ItemRating(_detail.item, CItem.getMemberId(), _detail.memberRating));
            details.add(WidgetUtil.makeShim(1, 5));
        }
        details.add(_creator = new CreatorLabel());
        _creator.setMember(_detail.creator);
        details.add(_description = new Label(ItemUtil.getDescription(_item)));

        if (_item instanceof Game) {
            String args = Args.compose("d" , ((Game)_item).gameId);
            details.add(Application.createLink("More info...", "game", args));
        }
    }

    protected void addTabBelow (String title, Widget content, boolean select)
    {
        if (_belowTabs == null) {
            addBelow(_belowTabs = new StyledTabPanel());
        }
        _belowTabs.add(content, title);
        if (select) {
            _belowTabs.selectTab(0);
        }
    }

    /**
     * Adds a widget below the primary item detail contents.
     */
    protected void addBelow (Widget widget)
    {
        int row = getRowCount();
        setWidget(row, 0, widget);
        getFlexCellFormatter().setColSpan(row, 0, 3);
    }

    /**
     * Overrideable by subclasses to enable avatar scale editing.
     */
    protected boolean allowAvatarScaleEditing ()
    {
        return false;
    }

    /**
     * Called from the avatarviewer to effect a scale change.
     */
    protected void updateAvatarScale (float newScale)
    {
        if (!(_item instanceof Avatar)) {
            return;
        }

        Avatar av = (Avatar) _item;
        if (av.scale != newScale) {
            // stash the new scale in the item
            av.scale = newScale;
            _scaleUpdated = true;

            // try immediately updating in the whirled client
            sendAvatarScaleToWorld(av.itemId, newScale);
        }
    }

    // @Override // Panel
    protected void onDetach ()
    {
        super.onDetach();

        // persist our new scale to the server
        if (_scaleUpdated) {
            CItem.itemsvc.scaleAvatar(CItem.ident, _item.itemId, ((Avatar) _item).scale,
                new AsyncCallback () {
                    public void onSuccess (Object result) {
                        // nada
                    }
                    public void onFailure (Throwable caught) {
                        CItem.log("Failed to update scale [item=" + _item.getIdent() + "]", caught);
                    }
                });
        }
    }

    /**
     * Called when the user clicks the "up arrow" button next to the name to return to their
     * inventory or catalog listing.
     */
    protected abstract void returnToList ();

    /**
     * Sends the new avatar scale to the whirled client.
     */
    protected static native void sendAvatarScaleToWorld (int avatarId, float newScale) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.updateAvatarScale(avatarId, newScale);
        }
    }-*/;

    /**
     * Configures interface to be called by the avatarviewer.
     */
    protected static native void configureCallbacks (BaseItemDetailPanel panel) /*-{
        $wnd.updateAvatarScale = function (newScale) {
            panel.@client.item.BaseItemDetailPanel::updateAvatarScale(F)(newScale);
        }
    }-*/;

    protected Item _item;
    protected ItemDetail _detail;

    protected HorizontalPanel _header, _footer;
    protected HorizontalPanel _extras, _buttons;
    protected VerticalPanel _details;

    protected Label _description;
    protected CreatorLabel _creator;

    protected StyledTabPanel _belowTabs;

    /** Have we updated the scale (of an avatar?) */
    protected boolean _scaleUpdated;
}
