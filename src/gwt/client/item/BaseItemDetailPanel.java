//
// $Id$

package client.item;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.web.data.CatalogQuery;

import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shop.CShop;
import client.util.CreatorLabel;
import client.util.FlashClients;
import client.util.HeaderBox;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.RoundBox;
import client.util.StyledTabPanel;
import client.util.TagDetailPanel;

/**
 * Defines the base item detail popup from which we derive an inventory and catalog item detail.
 */
public abstract class BaseItemDetailPanel extends SmartTable
{
    protected BaseItemDetailPanel (ItemDetail detail)
    {
        super("itemDetailPanel", 0, 10);

        _detail = detail;
        _item = detail.item;

        HeaderBox bits = new HeaderBox(null, _item.name);
        SimplePanel preview = new SimplePanel();
        preview.setStyleName("Preview");
        preview.setWidget(createPreview(_item));
        bits.add(preview);
        if (_item.isRatable()) {
            ItemRating rating = new ItemRating(_detail.item, _detail.memberRating, true);
            rating.addStyleName("Rating");
            bits.add(rating);
        }
        setWidget(0, 0, bits);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HorizontalPanel.ALIGN_TOP);

        // a place for details
        setWidget(0, 1, _details = new RoundBox(RoundBox.BLUE), 1, "Details");
        _details.setWidth("100%");
        getFlexCellFormatter().setVerticalAlignment(0, 1, HorizontalPanel.ALIGN_TOP);

        // set up our detail bits
        _details.add(_creator = new CreatorLabel());
        _creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CShell.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        Application.go(Page.PEOPLE, "" + _detail.creator.getMemberId());
                    }
                });
                this.addMenuItem(CShell.imsgs.browseCatalogFor(), new Command() {
                    public void execute () {
                        CatalogQuery query = new CatalogQuery();
                        query.itemType = _detail.item.getType();
                        query.creatorId = _detail.creator.getMemberId();
                        Application.go(Page.SHOP, CShop.composeArgs(query, 0));
                    }
                });
            }
        });

        _details.add(WidgetUtil.makeShim(10, 10));
        _indeets = new RoundBox(RoundBox.WHITE);
        _indeets.addStyleName("Description");
        _details.add(_indeets);
        _indeets.add(new Label(ItemUtil.getDescription(_item)));

        if (_item instanceof Game) {
            _details.add(WidgetUtil.makeShim(10, 10));
            String args = Args.compose("d" , ((Game)_item).gameId);
            _details.add(Application.createLink(CShell.imsgs.bidPlay(), Page.GAMES, args));
        }

        // add our tag business at the bottom
        getFlexCellFormatter().setHeight(1, 0, "10px");
        setWidget(1, 0, new TagDetailPanel(new TagDetailPanel.TagService() {
            public void tag (String tag, AsyncCallback callback) {
                CShell.itemsvc.tagItem(CShell.ident, _item.getIdent(), tag, true, callback);
            } 
            public void untag (String tag, AsyncCallback callback) {
                CShell.itemsvc.tagItem(CShell.ident, _item.getIdent(), tag, false, callback);
            }
            public void getRecentTags (AsyncCallback callback) {
                CShell.itemsvc.getRecentTags(CShell.ident, callback);
            }
            public void getTags (AsyncCallback callback) {
                CShell.itemsvc.getTags(CShell.ident, _item.getIdent(), callback);
            }
            public boolean supportFlags () {
                return true;
            }
            public void setFlags (final byte flag) {
                ItemIdent ident = new ItemIdent(_item.getType(), _item.getPrototypeId());
                CShell.itemsvc.setFlags(CShell.ident, ident, flag, flag, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        _item.flagged |= flag;
                    }
                });
            }
            public void addMenuItems (String tag, PopupMenu menu) { }
        }, true));

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

    /**
     * Called when the user clicks our up arrow.
     */
    protected void onUpClicked ()
    {
        History.back();
    }

    // @Override // Panel
    protected void onDetach ()
    {
        super.onDetach();

        // persist our new scale to the server
        if (_scaleUpdated && _item.ownerId == CShell.getMemberId()) {
            CShell.itemsvc.scaleAvatar(
                CShell.ident, _item.itemId, ((Avatar) _item).scale, new MsoyCallback() {
                public void onSuccess (Object result) {
                    // nada
                }
            });
        }
    }

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

    protected RoundBox _details;
    protected RoundBox _indeets;

    protected CreatorLabel _creator;

    protected StyledTabPanel _belowTabs;

    /** Have we updated the scale (of an avatar?) */
    protected boolean _scaleUpdated;
}
