//
// $Id$

package client.editem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Decor;

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

        FlexTable bits = new FlexTable();
        tabs.add(bits, CEditem.emsgs.gameConfigTab());

        int row = 0;

        bits.setText(row, 0, CEditem.emsgs.decorWidth());
        bits.setWidget(row++, 1, bind(_width = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.width = asShort(text);
                sendDecorUpdateToFlash();
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.decorHeight());
        bits.setWidget(row++, 1, bind(_height = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.height = asShort(text);
                sendDecorUpdateToFlash();
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.decorDepth());
        bits.setWidget(row++, 1, bind(_depth = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.depth = asShort(text);
                sendDecorUpdateToFlash();
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.decorHorizon());
        bits.setWidget(row++, 1, bind(_horizon = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _decor.horizon = asFloat(text);
                sendDecorUpdateToFlash();
            }
        }));

        bits.setText(row, 0, CEditem.emsgs.decorPreview());
        bits.setWidget(row++, 1, _viewer = FlashClients.createDecorViewer());

        // don't let users edit any values in the browser
        _width.setEnabled(false); 
        _height.setEnabled(false); 
        _depth.setEnabled(false); 
        _horizon.setEnabled(false);

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
        _width.setText("" + _decor.width);
        _height.setText("" + _decor.height);
        _depth.setText("" + _decor.depth);
        _horizon.setText("" + _decor.horizon);
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
        mediaUpdateHelper(_decor.furniMedia.getMediaPath());
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
        
        
    // mr. utility, stolen from game editor
    protected static short asShort (String s)
    {
        try {
            return (short) Integer.parseInt(s);
        } catch (Exception e) {
            return (short) 0;
        }
    }

    // these utilities are silly
    protected static float asFloat (String s)
    {
        try {
            return (float) Float.parseFloat(s);
        } catch (Exception e) {
            return (float) 0;
        }
    }

    protected Decor _decor;
    protected TextBox _width, _height, _depth, _horizon;
    protected ListBox _type;
    protected HTML _viewer;

}
