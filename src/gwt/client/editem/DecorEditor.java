//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;

import client.shell.CShell;
import client.util.FlashClients;

/**
 * A class for creating and editing {@link Decor} digital items.
 */
public class DecorEditor extends ItemEditor
{
    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        Decor d = new Decor();
        // some sample values. dimensions will be overwritten once a new image gets uploaded.
        d.type = Decor.IMAGE_OVERLAY;
        d.width = 800;
        d.height = 400;
        d.depth = 400;
        d.horizon = 0.5f;
        d.offsetX = 0;
        d.offsetY = 0;
        d.hideWalls = false;
        return d;
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        addSpacer();
        addRow(_emsgs.decorLabel(), createFurniUploader(true, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidPrimaryMedia(desc)) {
                    return _emsgs.errFurniNotFlash();
                }
                _item.furniMedia = desc;
                if (width > 0 && height > 0) {
                    // set dimensions
                    _decor.width = (short) width;
                    _decor.height = (short) height;
                    _decor.depth = (short) height;
                    // clear offsets
                    _decor.offsetX = _decor.offsetY = 0;
                    sendDecorUpdateToFlash();
                    updateDebuggingLabel();
                }
                return null;
            }
        }), _emsgs.decorTip());

        addSpacer();
        addRow(new Label(_emsgs.decorConfigTab()));

        // note: the container has to be added to the DOM *before* we add the flash viewer due to a
        // bug in IE6; since we're in ItemEditor's constructor right now, we have to add the HTML
        // that will contain the viewer, then queue up a deferred command so that our constructor
        // can return, we can be added into the DOM and *then* the Flash <embed> will be added
        final HTML viewer = new HTML();
        addRow(viewer);
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                FlashClients.embedDecorViewer(viewer);
                configureCallbacks(DecorEditor.this);
            }
        });
    }

    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _decor = (Decor)item;

        updateDebuggingLabel();
        sendDecorUpdateToFlash();
    }

    /**
     * When the decor item had changed, updates numeric values being displayed.
     */
    protected void updateDebuggingLabel ()
    {
        // the following is useful while debugging gwt/flash interop

        /*
        String typelabel = _emsgs.decorType_None();
        switch (_decor.type) {
        case Decor.IMAGE_OVERLAY: typelabel = _emsgs.decorType_Standard(); break;
        case Decor.FIXED_IMAGE: typelabel = _emsgs.decorType_Fixed(); break;
        }

        // GWT doesn't emulate java.text.NumberFormat...
        // so we "format" this float by hand to three decimal places. ugh. :(
        String horizon = Float.toString(Math.round (_decor.horizon * 1000f) / 1000f);

        MsoyUI.info(
            _emsgs.decorDimensions() + " " + _decor.width + " x " +
            _decor.height + " x " + _decor.depth + "<br/>" +
            _emsgs.decorHorizon() + " " + horizon + " / " +
            _decor.offsetX + ", " + _decor.offsetY + ", " + _decor.hideWalls + "<br/>" +
            _emsgs.decorType() + " " + typelabel);
        */
    }

    /**
     * Receives a number of values from DecorViewer, and updates the Decor item accordingly.
     */
    protected void updateDecorFromFlash (
        short width, short height, short depth, float horizon,
        byte type, float offsetX, float offsetY, boolean hideWalls)
    {
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.horizon = horizon;
        _decor.type = type;
        _decor.offsetX = offsetX;
        _decor.offsetY = offsetY;
        _decor.hideWalls = hideWalls;
        updateDebuggingLabel();
    }

    /**
     * Populates DecorViewer fields.
     */
    protected void sendDecorUpdateToFlash ()
    {
        if (_decor.furniMedia != null) {
            mediaUpdateHelper(_decor.furniMedia.getMediaPath());
        }
        decorUpdateHelper(_decor.width, _decor.height, _decor.depth, _decor.horizon, _decor.type,
                          _decor.offsetX, _decor.offsetY, _decor.hideWalls);
    }

    /**
     * Configures foreign interface that will be called by Flash.
     */
    protected static native void configureCallbacks (DecorEditor editor) /*-{
        $wnd.updateDecorInit = function () {
            editor.@client.editem.DecorEditor::sendDecorUpdateToFlash()();
        };
        $wnd.updateDecor = function (width, height, depth, horizon,
                                     type, offsetX, offsetY, hideWalls)
        {
            editor.@client.editem.DecorEditor::updateDecorFromFlash(SSSFBFFZ)(
                width, height, depth, horizon, type, offsetX, offsetY, hideWalls);
        };
    }-*/;

    protected static native void decorUpdateHelper (
        int width, int height, int depth, float horizon, byte type,
        float offsetX, float offsetY, boolean hideWalls) /*-{
        var viewer = $doc.getElementById("decorViewer");
        if (viewer) {
            viewer.updateParameters(
                width, height, depth, horizon, type, offsetX, offsetY, hideWalls);
        }
    }-*/;

    protected static native void mediaUpdateHelper (String mediaPath) /*-{
        var viewer = $doc.getElementById("decorViewer");
        if (viewer) {
            viewer.updateMedia(mediaPath);
        }
    }-*/;

    protected Decor _decor;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
