//
// $Id$

package client.editem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Decor;

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
        d.type = Decor.IMAGE_OVERLAY;
        d.width = 800;
        d.height = 494; // magic number: (short) Math.round(800 / ((1 + Math.sqrt(5)) / 2)); 
        d.depth = 400;
        d.horizon = 0.5f;
        return d;
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        super.createInterface(contents, tabs);

        VerticalPanel bits = new VerticalPanel();
        tabs.add(bits, CEditem.emsgs.decorConfigTab());

        bits.add(_viewer = FlashClients.createDecorViewer());
        bits.add(_label = new HTML());
        
        configureCallbacks(this);
    }

    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _decor = (Decor)item;

        updateUIFromDecor();
        sendDecorUpdateToFlash();
    }

    /**
     * When the decor item had changed, updates numeric values being displayed.
     */
    protected void updateUIFromDecor()
    {
        String typelabel = CEditem.emsgs.decorType_None();
        switch (_decor.type) {
        case Decor.IMAGE_OVERLAY: typelabel = CEditem.emsgs.decorType_Standard(); break;
        case Decor.FIXED_IMAGE: typelabel = CEditem.emsgs.decorType_Fixed(); break;
        }

        // GWT doesn't emulate java.text.NumberFormat...
        // so we "format" this float by hand to three decimal places. ugh. :(
        String horizon = Float.toString(Math.round (_decor.horizon * 1000f) / 1000f);
        
        _label.setHTML(
            CEditem.emsgs.decorDimensions() + " " + _decor.width + " x " +
            _decor.height + " x " + _decor.depth + "<br/>" +
            CEditem.emsgs.decorHorizon() + " " + horizon + "<br/>" +
            CEditem.emsgs.decorType() + " " + typelabel);
    }
    
    /**
     * Configures foreign interface that will be called by Flash.
     */
    protected static native void configureCallbacks (DecorEditor editor) /*-{
        $wnd.updateDecorInit = function () {
            editor.@client.editem.DecorEditor::sendDecorUpdateToFlash()();
        };
        $wnd.updateDecor = function (width, height, depth, horizon, type) {
            editor.@client.editem.DecorEditor::updateDecorFromFlash(SSSFB)(width, height, depth, horizon, type);
        };
    }-*/;

    /**
     * Receives a number of values from DecorViewer, and updates the Decor item accordingly.
     */
    protected void updateDecorFromFlash (
        short width, short height, short depth, float horizon, byte type)
    {
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.horizon = horizon;
        _decor.type = type;
        updateUIFromDecor();
    }

    /**
     * Populates DecorViewer fields.
     */
    protected void sendDecorUpdateToFlash ()
    {
        if (_decor.furniMedia != null) {
            mediaUpdateHelper(_decor.furniMedia.getMediaPath());
        }
        decorUpdateHelper(_decor.width, _decor.height, _decor.depth, _decor.horizon, _decor.type);
    }

    protected static native void decorUpdateHelper (
        int width, int height, int depth, float horizon, byte type) /*-{
        var viewer = $doc.getElementById("decorViewer");
        if (viewer) {
            viewer.updateParameters(width, height, depth, horizon, type);
        }
    }-*/;

    protected static native void mediaUpdateHelper (String mediaPath) /*-{
        var viewer = $doc.getElementById("decorViewer");
        if (viewer) {
            viewer.updateMedia(mediaPath);
        }
    }-*/;
        
    protected Decor _decor;
    protected HTML _viewer;
    protected HTML _label;

}
