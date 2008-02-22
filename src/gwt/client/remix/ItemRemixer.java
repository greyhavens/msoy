//
// $Id$

package client.remix;

import com.google.gwt.core.client.GWT;

import com.google.gwt.http.client.URL;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.shell.CShell;

import client.stuff.CStuff;

import client.editem.EditorHost;

import client.util.FlashClients;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;

public class ItemRemixer extends FlexTable
{
    public ItemRemixer (EditorHost host)
    {
        _singleton = this;
        _parent = host;

        setStyleName("itemRemixer");
        setCellPadding(0);
        setCellSpacing(5);

        FlexTable.FlexCellFormatter formatter = getFlexCellFormatter();
        formatter.setVerticalAlignment(0, 0, VerticalPanel.ALIGN_TOP);
        formatter.setRowSpan(0, 0, 2);
        setWidget(0, 0, MsoyUI.createBackArrow(new ClickListener() {
            public void onClick (Widget sender) {
                if (_item == null) {
                    History.back();
                } else {
                    CStuff.viewItem(_item.getType(), _item.itemId);
                }
            }
        }));

        configureBridges();
    }

    public void setItem (byte type, int itemId)
    {
        CShell.itemsvc.loadItem(CShell.ident, new ItemIdent(type, itemId), new MsoyCallback() {
            public void onSuccess (Object result) {
                setItem((Item) result);
            }
        });
    }

    public void setItem (Item item)
    {
        _item = item;
        HorizontalPanel hpan = new HorizontalPanel();
        hpan.add(createRemixControls(item));
        setWidget(0, 1, hpan);
    }

    protected Widget createRemixControls (Item item)
    {
        MediaDesc preview = item.getPreviewMedia();
        String serverURL = GWT.isScript() ? GWT.getHostPageBaseURL()
                                          : "http://localhost:8080/";

        String flashVars = "media=" + URL.encodeComponent(preview.getMediaPath()) + "&" +
            "server=" + URL.encodeComponent(serverURL) + "&" +
            "mediaId=" + URL.encodeComponent(Item.MAIN_MEDIA) + "&" +
            "auth=" + URL.encodeComponent(CShell.ident.token);
        return WidgetUtil.createFlashContainer("remixControls",
            "/clients/" + DeploymentConfig.version + "/remixer-client.swf",
            665, 550, flashVars);
    }

    protected void setHash (
        String id, String mediaHash, int mimeType, int constraint, int width, int height)
    {
        if (id != Item.MAIN_MEDIA) {
            CShell.log("setHash() called on remixer for non-main media: " + id);
            return;
        }

        MediaDesc desc = new MediaDesc(mediaHash, (byte) mimeType, (byte) constraint);

        // TODO: this logic is in the item editor.. somewhere else?
        byte type = _item.getType();
        if (type == Item.AVATAR) {
            ((Avatar) _item).avatarMedia = desc;

        } else {
            CShell.log("setHash() called for unhandled item type: " + _item.getType());
            return;
        }

        CShell.itemsvc.updateItem(CShell.ident, _item, new MsoyCallback() {
            public void onSuccess (Object result) {
                MsoyUI.info(CShell.emsgs.msgItemUpdated());
                _parent.editComplete((Item) result);
            }
        });
    }

    protected static void bridgeSetHash (
        String id, String mediaHash, int mimeType, int constraint, int width, int height)
    {
        // for some reason the strings that come in from JavaScript aren't quite right, so
        // we jiggle them thusly
        String fid = "" + id;
        String fhash = "" + mediaHash;
        _singleton.setHash(fid, fhash, mimeType, constraint, width, height);
    }

    protected static native void configureBridges () /*-{
        $wnd.setHash = function (id, hash, type, constraint, width, height) {
            @client.remix.ItemRemixer::bridgeSetHash(Ljava/lang/String;Ljava/lang/String;IIII)(id, hash, type, constraint, width, height);
        };
    }-*/;

    protected static ItemRemixer _singleton;

    protected EditorHost _parent;

    /** The item we're remixing. */
    protected Item _item;
}
