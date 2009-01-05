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

import client.util.FlashClients;

/**
 * A class for creating and editing {@link Decor} digital items.
 */
public class DecorEditor extends ItemEditor
{
    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        _attrsNeedFiguring = true;

        Decor d = new Decor();
        // some sample values. dimensions will be overwritten once a new image gets uploaded.
        d.type = Decor.IMAGE_OVERLAY;
        d.width = 800;
        d.height = 400;
        d.depth = 400;
        d.horizon = 0.5f;
        d.hideWalls = false;
        d.actorScale = 1;
        d.furniScale = 1;
        return d;
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        addSpacer();
        addRow(_emsgs.decorLabel(), createFurniUploader(TYPE_FLASH_REMIXABLE, true,
            new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidPrimaryMedia(desc)) {
                    return _emsgs.errFurniNotFlash();
                }
                _attrsNeedFiguring = true;
                _item.setFurniMedia(desc);
                sendDecorUpdateToFlash();
                maybeSetNameFromFilename(name);
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
            }
        });

        // wire up the callbacks needed by the Flash decor editor
        configureCallbacks(DecorEditor.this);
    }

    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _decor = (Decor)item;
        _attrsNeedFiguring = false;
        sendDecorUpdateToFlash();
    }

    @Override
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        if (_attrsNeedFiguring) {
            // this could only really happen if flash is turned off or if it hasn't initialized yet
            throw new Exception(_emsgs.editorNotConsistent());
        }
    }

    /**
     * Receives a number of values from DecorViewer, and updates the Decor item accordingly.
     */
    protected void updateDecorFromFlash (
        byte type, boolean hideWalls, short width, short height, short depth,
        float horizon, float actorScale, float furniScale)
    {
        _decor.type = type;
        _decor.hideWalls = hideWalls;
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.horizon = horizon;
        _decor.actorScale = actorScale;
        _decor.furniScale = furniScale;

        _attrsNeedFiguring = false;
    }

    /**
     * Populates DecorViewer fields.
     */
    protected void sendDecorUpdateToFlash ()
    {
        if (_decor.getRawFurniMedia() != null) {
            mediaUpdateHelper(_decor.getRawFurniMedia().getMediaPath(), _attrsNeedFiguring);
        }
        if (!_attrsNeedFiguring) {
            decorUpdateHelper(_decor.type, _decor.hideWalls, _decor.width, _decor.height,
                _decor.depth, _decor.horizon, _decor.actorScale, _decor.furniScale);
        }
    }

    /**
     * Configures foreign interface that will be called by Flash.
     */
    protected static native void configureCallbacks (DecorEditor editor) /*-{
        $wnd.updateDecorInit = function () {
            editor.@client.editem.DecorEditor::sendDecorUpdateToFlash()();
        };
        $wnd.updateDecor = function (type, hideWalls, width, height, depth, horizon,
                                     actorScale, furniScale)
        {
            editor.@client.editem.DecorEditor::updateDecorFromFlash(BZSSSFFF)(
                type, hideWalls, width, height, depth, horizon, actorScale, furniScale);
        };
    }-*/;

    protected static native void decorUpdateHelper (
        byte type, boolean hideWalls, int width, int height, int depth, float horizon,
        float actorScale, float furniScale) /*-{
        var viewer = $doc.getElementById("decorViewer");
        if (viewer) {
            viewer.updateParameters(
                type, hideWalls, width, height, depth, horizon, actorScale, furniScale);
        }
    }-*/;

    protected static native void mediaUpdateHelper (String mediaPath, boolean figureAttrs) /*-{
        var viewer = $doc.getElementById("decorViewer");
        if (viewer) {
            viewer.updateMedia(mediaPath, figureAttrs);
        }
    }-*/;

    protected Decor _decor;

    /** True if the flash client needs to figure out initial attributes for the decor
     * based on the media. */
    protected boolean _attrsNeedFiguring;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
