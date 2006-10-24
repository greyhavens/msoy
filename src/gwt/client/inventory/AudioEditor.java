//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.MsoyEntryPoint;

/**
 *  * A class for creating and editing {@link Photo} digital items.
 *   */
public class AudioEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _audio = (Audio)item;
        _description.setText(
            (_audio.description == null) ? "" : _audio.description);
        _mainUploader.setMedia(_audio.audioMedia);
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        configureMainUploader("Upload your audio.", new MediaUpdater() {
            public void updateMedia (byte[] hash, byte mimeType) {
                _audio.audioMedia = new MediaDesc(hash, mimeType);
                recenter(true);
            }
        });

        super.createEditorInterface();

        addRow("Description", _description = new TextBox());
        bind(_description, new Binder() {
            public void textUpdated (String text) {
                _audio.description = text;
            }
        });
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Audio();
    }

    protected Audio _audio;
    protected TextBox _description;
}
