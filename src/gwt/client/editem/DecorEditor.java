//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;
import client.util.FlashClients;


/**
 * A class for creating and editing {@link Decor} digital items.
 */
public class DecorEditor extends ItemEditor
{
    // @Override from ItemEditor
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

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        super.createInterface(contents, tabs);

        // note: the container has to be added to the page *before* we add the flash viewer
        VerticalPanel bits = new VerticalPanel();
        tabs.add(bits, CShell.emsgs.decorConfigTab());

        FlashClients.embedDecorViewer(bits); 
        bits.add(_label = new HTML());
        configureCallbacks(this);
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        String title = CShell.emsgs.decorMainTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CShell.emsgs.errFurniNotFlash();
                }
                _item.furniMedia = desc;
                if (width > 0 && height > 0) {
                    // set dimensions
                    _decor.width = (short) width; 
                    _decor.height = (short) height;
                    _decor.depth = (short) height;
                    // clear offsets
                    _decor.offsetX = _decor.offsetY = 0;
                    updateDebuggingLabel();
                }
                return null;
            }
        });
        tabs.add(_furniUploader, CShell.emsgs.decorMainTab());
    }

    // @Override from ItemEditor
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
    protected void updateDebuggingLabel()
    {
        // the following is useful while debugging gwt/flash interop
        
        /*
        String typelabel = CShell.emsgs.decorType_None();
        switch (_decor.type) {
        case Decor.IMAGE_OVERLAY: typelabel = CShell.emsgs.decorType_Standard(); break;
        case Decor.FIXED_IMAGE: typelabel = CShell.emsgs.decorType_Fixed(); break;
        }

        // GWT doesn't emulate java.text.NumberFormat...
        // so we "format" this float by hand to three decimal places. ugh. :(
        String horizon = Float.toString(Math.round (_decor.horizon * 1000f) / 1000f);
        
        _label.setHTML(
            CShell.emsgs.decorDimensions() + " " + _decor.width + " x " +
            _decor.height + " x " + _decor.depth + "<br/>" +
            CShell.emsgs.decorHorizon() + " " + horizon + " / " +
            _decor.offsetX + ", " + _decor.offsetY + ", " + _decor.hideWalls + "<br/>" +
            CShell.emsgs.decorType() + " " + typelabel);
        */
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
    protected HTML _label;

}
