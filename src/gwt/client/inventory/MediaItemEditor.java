//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.FlowPanel;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

/**
 * Extends the standard {@link ItemEditor} with some magic to allow uploading
 * of the media during the item creation process.
 */
public abstract class MediaItemEditor extends ItemEditor
{
    public MediaItemEditor ()
    {
    }

    protected void createEditorInterface ()
    {
        _panel = new FormPanel(new FlowPanel());
        _panel.setTarget("/upload");
        _panel.setTarget("__msoy_uploadFrame");
        _panel.setMethodAsPost();
        _panel.setMultipartEncoding();
        _panel.addField(new FileUploadField("media"), "media");

        int row = getRowCount();
        setText(row, 0, "Upload");
        setWidget(row, 1, _panel.getPanel());
    }

    protected FormPanel _panel;
}
