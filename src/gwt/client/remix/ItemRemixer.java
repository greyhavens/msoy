//
// $Id$

package client.remix;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.CostUpdatedException;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

import client.editem.EditemMessages;
import client.editem.EditorHost;
import client.item.ImageChooserPopup;
import client.item.ItemUtil;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.PriceLabel;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

// TODO: Bar me
public class ItemRemixer extends FlexTable
{
    public ItemRemixer (EditorHost host)
    {
        _singleton = this;
        _parent = host;

        setStyleName("itemRemixer");
        setCellPadding(0);
        setCellSpacing(5);

        configureBridges();
    }

    /**
     * Set the price of the item, for use when we're remixing a listed item.
     */
    public void setCatalogInfo (int catalogId, int flowCost, int goldCost)
    {
        _catalogId = catalogId;
        _flowCost = flowCost;
        _goldCost = goldCost;
    }

    public void setItem (byte type, int itemId)
    {
        _stuffsvc.loadItem(new ItemIdent(type, itemId), new MsoyCallback<Item>() {
            public void onSuccess (Item result) {
                setItem(result);
            }
        });
    }

    public void setItem (Item item)
    {
        _item = item;
        VerticalPanel vpan = new VerticalPanel();
        vpan.add(createRemixControls(item));

        if (_catalogId != 0) {
            HorizontalPanel hpan = new HorizontalPanel();
            hpan.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            _priceLabel = new PriceLabel(Currency.COINS, _flowCost);
            hpan.add(_priceLabel);
            hpan.setCellWidth(_priceLabel, WIDTH + "px");
            vpan.add(hpan);
        }

        setWidget(0, 0, vpan);
    }

    protected Widget createRemixControls (Item item)
    {
        MediaDesc main = item.getPrimaryMedia();
        String serverURL = GWT.isScript() ? DeploymentConfig.serverURL
                                          : "http://localhost:8080/";

        String flashVars = "media=" + URL.encodeComponent(main.getMediaPath()) +
            "&name=" + URL.encodeComponent(item.name) +
            "&type=" + URL.encodeComponent(Item.getTypeName(item.getType())) +
            "&server=" + URL.encodeComponent(serverURL) +
            "&mediaId=" + URL.encodeComponent(Item.MAIN_MEDIA) +
            "&auth=" + URL.encodeComponent(CShell.getAuthToken());

        if (item instanceof Decor) {
            flashVars += "&" + ItemUtil.createDecorViewerParams((Decor) item);
        }
        if (_catalogId != 0) {
            flashVars += "&mustBuy=true";
        }

        return WidgetUtil.createFlashContainer("remixControls",
            "/clients/" + DeploymentConfig.version + "/remixer-client.swf",
            WIDTH, 550, flashVars);
    }

    /**
     * Show the ImageFileChooser and let the user select a photo from their inventory.
     *
     * TODO: the damn ImageChooserPopup needs a proper cancel button and a response when it
     * cancels so that we can try to do the right thing in PopupFilePreview.
     */
    protected void pickPhoto ()
    {
        ImageChooserPopup.displayImageChooser(false, new MsoyCallback<MediaDesc>() {
            public void onSuccess (MediaDesc photo) {
                setPhotoUrl(photo.getMediaPath());
            }
        });
    }

    protected void cancelRemix ()
    {
        _parent.editComplete(null);
    }

    protected void setHash (
        final String id, final String mediaHash, final int mimeType, final int constraint,
        final int width, final int height)
    {
        if (id != Item.MAIN_MEDIA) {
            CShell.log("setHash() called on remixer for non-main media: " + id);
            return;
        }

        if (_catalogId != 0) {
            _catalogsvc.purchaseItem(
                _item.getType(), _catalogId, _flowCost, _goldCost, new AsyncCallback<Item>() {
                public void onSuccess (Item result) {
                    _item = result;
                    _catalogId = 0;
                    // re-enter, to save our remix
                    setHash(id, mediaHash, mimeType, constraint, width, height);
                }

                public void onFailure (Throwable cause) {
                    MsoyUI.error(CShell.serverError(cause));
                    if (cause instanceof CostUpdatedException) {
                        CostUpdatedException cue = (CostUpdatedException) cause;
                        _flowCost = cue.getFlowCost();
                        _goldCost = cue.getGoldCost();
                        _priceLabel.updatePrice(Currency.COINS, _flowCost);
                        enableBuyButton();
                    }
                }
            });
            return;
        }

        _item.setPrimaryMedia(new MediaDesc(mediaHash, (byte) mimeType, (byte) constraint));

        _stuffsvc.remixItem(_item, new MsoyCallback<Item>() {
            public void onSuccess (Item item) {
                MsoyUI.info(_emsgs.msgItemUpdated());
                _parent.editComplete(item);
            }
        });
    }

    /**
     * Set a photo as a new image source in the remixer. The PopupFilePreview needs to be up..
     */
    protected static native void setPhotoUrl (String url) /*-{
        var controls = $doc.getElementById("remixControls");
        if (controls) {
            try {
                controls.setPhotoUrl(url);
            } catch (e) {
                // nada
            }
        }
    }-*/;

    protected static native void enableBuyButton () /*-{
        var controls = $doc.getElementById("remixControls");
        if (controls) {
            try {
                controls.enableBuyButton();
            } catch (e) {
                // nada
            }
        }
    }-*/;

    protected static void bridgeSetHash (
        String id, String mediaHash, int mimeType, int constraint, int width, int height)
    {
        // for some reason the strings that come in from JavaScript aren't quite right, so
        // we jiggle them thusly
        String fid = "" + id;
        String fhash = "" + mediaHash;
        _singleton.setHash(fid, fhash, mimeType, constraint, width, height);
    }

    protected static void bridgeCancelRemix ()
    {
        _singleton.cancelRemix();
    }

    protected static void bridgePickPhoto ()
    {
        _singleton.pickPhoto();
    }

    protected static native void configureBridges () /*-{
        $wnd.setRemixHash = function (id, hash, type, constraint, width, height) {
            @client.remix.ItemRemixer::bridgeSetHash(Ljava/lang/String;Ljava/lang/String;IIII)(
                id, hash, type, constraint, width, height);
        };
        $wnd.cancelRemix = function () {
            @client.remix.ItemRemixer::bridgeCancelRemix()();
        };
        $wnd.pickPhoto = function () {
            @client.remix.ItemRemixer::bridgePickPhoto()();
        };
    }-*/;

    protected EditorHost _parent;

    /** The item we're remixing. */
    protected Item _item;

    protected int _catalogId;
    protected int _flowCost, _goldCost;

    protected PriceLabel _priceLabel;

    protected static ItemRemixer _singleton;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);

    protected static final int WIDTH = 680;
}
