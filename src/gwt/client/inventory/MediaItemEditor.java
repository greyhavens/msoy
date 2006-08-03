//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

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
        _panel.setAction("http://localhost:8080/upload");
        _panel.setTarget("upload");
        _panel.setMethodAsPost();
        _panel.setMultipartEncoding();
        _panel.addField(new FileUploadField("media"), "media");

        _panel.add(_submit = new Button("Upload"));
        _submit.addClickListener(new ClickListener() {
            public void onClick (Widget button) {
                _submit.setEnabled(false);
                _panel.submit();
            }
        });

        int row = getRowCount();
        setText(row, 0, "Upload");
        setWidget(row, 1, _panel);

        setWidget(row+1, 0, _out = new Label());
        setWidget(row+1, 1, _check = new Button("Check"));
        _check.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _out.setText(checkUpload());
            }
        });
    }

    protected static native String checkUpload () /*-{
        return $doc.getElementById('__msoy_uploadFrame').toString();
    }-*/;

    protected FormPanel _panel;
    protected Button _submit;

    protected Label _out;
    protected Button _check;
}
