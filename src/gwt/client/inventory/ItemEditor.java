//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.client.WebContext;

/**
 * The base class for an interface for creating and editing digital items.
 *
 * <p> Styles:
 * <ul>
 * <li> item_editor - the style of the main editor
 * <li> item_editor_title - the style of the title label
 * <li> item_editor_submit - the style of the submit button
 * </ul>
 */
public abstract class ItemEditor extends FlexTable
{
    public static interface Binder
    {
        public void textUpdated (String newText);
    }

    public ItemEditor ()
    {
        setStyleName("item_editor");
        setCellSpacing(5);

        setWidget(0, 0, _etitle = new Label("title"));
        _etitle.setStyleName("item_editor_title");

        FlexCellFormatter cellFormatter = getFlexCellFormatter();

        // have the child do its business
        createEditorInterface();

        // compute our widest row so we can set our colspans
        int rows = getRowCount(), cols = 0;
        for (int ii = 0; ii < rows; ii++) {
            cols = Math.max(cols, getCellCount(ii));
        }
        cellFormatter.setColSpan(0, 0, cols);

        HorizontalPanel bpanel = new HorizontalPanel();
        int butrow = getRowCount();
        setWidget(butrow, 0, bpanel);
        cellFormatter.setHorizontalAlignment(
            0, butrow, HasAlignment.ALIGN_RIGHT);
        cellFormatter.setColSpan(0, butrow, cols);

        bpanel.add(_esubmit = new Button("submit"));
        _esubmit.setStyleName("item_editor_button");
        _esubmit.setEnabled(false);
        _esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });

        Button ecancel;
        bpanel.add(ecancel = new Button("Cancel"));
        ecancel.setStyleName("item_editor_button");
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _parent.editComplete(ItemEditor.this, null);
            }
        });
    }

    /**
     * Configures this editor with a reference to the item service and its item
     * panel parent.
     */
    public void init (WebContext ctx, ItemPanel parent)
    {
        _ctx = ctx;
        _parent = parent;
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly
     * constructed if we are using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _etitle.setText((item.itemId <= 0) ? "Create" : "Edit");
        _esubmit.setText((item.itemId <= 0) ? "Create" : "Update");
        updateSubmittable();
    }

    /**
     * Returns the currently configured item.
     */
    public Item getItem ()
    {
        return _item;
    }

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    /**
     * Derived classes should create and add their interface components in this
     * method.
     */
    protected void createEditorInterface ()
    {
        _panel = new FormPanel(new FlowPanel());
        if (GWT.isScript()) {
            _panel.setAction("/upload");
        } else {
            _panel.setAction("http://localhost:8080/upload");
        }
        _panel.setTarget("upload");
        _panel.setMethodAsPost();
        _panel.setMultipartEncoding();

// TODO: make this work, handle errors, check the status
//        _panel.addFormHandler(new FormHandler() {
//            public void onSubmit (FormSubmitEvent event) {
//                // nada for now
//            }
//
//            public void onSubmitComplete (FormSubmitCompleteEvent event) {
//                // TODO: what is the format of the results?
//                _out.setText(event.getResults());
//            }
//        });

        _panel.addField(new FileUploadField("media"), "media");

        if (GWT.isScript()) {
            _panel.addField(new SubmitField("submit", "Upload"), "submit");
        } else {
            Button submit = new Button("Upload");
            submit.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    _panel.submit();
                }
            });
            _panel.add(submit);
        }

        int row = getRowCount();
        setText(row, 0, "Upload");
        setWidget(row, 1, _panel);

        String msg = "First upload the file from your computer. " +
            "Then create the item below.";
        setWidget(row+1, 0, _out = new Label(msg));
        getFlexCellFormatter().setColSpan(row+1, 0, 2);

        // reserve area for the preview
        reservePreviewSpace();

        // we have to do this wacky singleton crap because GWT and/or
        // JavaScript doesn't seem to cope with our trying to create an
        // anonymous function that calls an instance method on a JavaScript
        // object
        _singleton = this;
    }

    /**
     * Editors should override this method to indicate when the item is in a
     * consistent state and may be uploaded.
     */
    protected boolean itemConsistent ()
    {
        return (_item != null) && _item.isConsistent();
    }

    /**
     * Configures this item editor with the hash value for media that it is
     * about to upload.
     */
    protected void setHash (String mediaHash, int mimeType)
    {
        if (_item != null) {
            // TODO: a bunch of stuff
            _item.setFurniHash(mediaHash, (byte)mimeType);
            updatePreview();
        }
        _out.setText("File uploaded.");
        updateSubmittable();
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code
     * received from the server as a response to our file upload POST request.
     */
    protected static void callBridge (String mediaHash, int mimeType)
    {
        _singleton.setHash(mediaHash, mimeType);
    }

    /**
     * Editors should call this method when something changes that might render
     * an item consistent or inconsistent. It will update the enabled status of
     * the submit button.
     */
    protected void updateSubmittable ()
    {
        _esubmit.setEnabled(itemConsistent());
    }

    /**
     * Called when the user has clicked the "update" or "create" button to
     * commit their edits or create a new item, respectively.
     */
    protected void commitEdit ()
    {
        _ctx.itemsvc.createItem(_ctx.creds, _item, new AsyncCallback() {
            public void onSuccess (Object result) {
                _parent.setStatus("Item created.");
                _parent.editComplete(ItemEditor.this, _item);
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _parent.setStatus("Item creation failed: " + reason);
            }
        });
    }

    /**
     * Creates a blank item for use when creating a new item using this editor.
     */
    protected abstract Item createBlankItem ();

    /**
     * Call from your configureEditorInterface to add a preview area that can
     * later be filled-in by calling updatePreview().
     */
    protected void reservePreviewSpace ()
    {
        FlexCellFormatter cellFormatter = getFlexCellFormatter();
        int row = getRowCount();
        setText(row, 0, "Preview");
        cellFormatter.setColSpan(row, 0, 2);

        _previewRow = row + 1;
        prepareCell(_previewRow, 0);
        cellFormatter.setColSpan(_previewRow, 0, 2);
    }

    /**
     * Call to update the preview.
     */
    protected void updatePreview ()
    {
        if (_item != null) {
            if (_previewRow == -1) {
                reservePreviewSpace(); // a little late, but ok!
            }
            setWidget(_previewRow, 0, new ItemContainer(_item, false, false));
        }
    }

    /**
     * A convenience method for attaching a textbox directly to a field in the
     * item to be edited.
     */
    protected void bind (final TextBox textbox, final Binder binder)
    {
        textbox.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int mods) {
                if (_item != null) {
                    DeferredCommand.add(new Command() {
                        public void execute () {
                            binder.textUpdated(textbox.getText());
                            updateSubmittable();
                        }
                    });
                }
            }
        });
    }

    /**
     * This wires up a sensibly named function that our POST response
     * JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (hash, type) {
           @client.inventory.ItemEditor::callBridge(Ljava/lang/String;I)(hash, type);
        };
    }-*/; 

    /** Create a forum submit button. */
    protected static class SubmitField extends Widget
    {
        public SubmitField (String name, String value) {
            setElement(DOM.createElement("input"));
            DOM.setAttribute(getElement(), "type", "submit");
            DOM.setAttribute(getElement(), "value", value);
            DOM.setAttribute(getElement(), "name", name);
        }
    }

    protected WebContext _ctx;
    protected ItemPanel _parent;

    protected Item _item;
    protected int _previewRow = -1;

    protected FormPanel _panel;
    protected Label _out;
    protected Label _etitle;
    protected Button _esubmit;

    protected static ItemEditor _singleton;
}
