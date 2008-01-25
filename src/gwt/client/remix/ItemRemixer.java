//
// $Id$

package client.remix;

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

import client.util.FlashClients;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;

public class ItemRemixer extends FlexTable
{
    public ItemRemixer ()
    {
        setStyleName("itemRemixer");
        setCellPadding(0);
        setCellSpacing(5);

        FlexTable.FlexCellFormatter formatter = getFlexCellFormatter();
        formatter.setVerticalAlignment(0, 0, VerticalPanel.ALIGN_TOP);
        formatter.setRowSpan(0, 0, 2);
        setWidget(0, 0, MsoyUI.createBackArrow(new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        }));

        configureBridge();
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
        HorizontalPanel hpan = new HorizontalPanel();
        hpan.add(createRemixControls(item));
        hpan.add(createPreview(item));
        setWidget(0, 1, hpan);
    }

    protected Widget createRemixControls (Item item)
    {
        MediaDesc preview = item.getPreviewMedia();

        return WidgetUtil.createApplet("remixControls",
            "/clients/" + DeploymentConfig.version + "/remixer-applet.jar",
            "com.threerings.msoy.item.remix.client.RemixApplet", 300, 400, true,
            new String[] { "media", URL.encodeComponent(preview.getMediaPath()) });
    }

//    protected Widget createRemixControls (Item item)
//    {
//        MediaDesc preview = item.getPreviewMedia();
//        String flashVars = "media=" + URL.encodeComponent(preview.getMediaPath());
//        return WidgetUtil.createFlashContainer("remixControls", "/media/RemixStub.swf",
//            200, 200, flashVars);
//    }

    protected Widget createPreview (Item item)
    {
        if (item instanceof Avatar) {
            // TODO : I'm using a custom avatar viewer so I can pass the 'message', but this
            // should probably be refactored.
            //return FlashClients.createAvatarViewer("", 1, false);

            return WidgetUtil.createFlashContainer(
                "remixPreview", "/clients/" + DeploymentConfig.version + "/avatarviewer.swf", 
                360, 450, "message=" + URL.encodeComponent("Loading preview..."));
        }

        // TODO: viewers for other item types

        MediaDesc preview = item.getPreviewMedia();
        return MediaUtil.createMediaView(preview, MediaDesc.PREVIEW_SIZE);
    }

    protected static native void configureBridge () /*-{
        $wnd.setMediaBytes = function (base64bytes) {
            $doc.getElementById("remixPreview").setMediaBytes(base64bytes);
        };
    }-*/;
}
