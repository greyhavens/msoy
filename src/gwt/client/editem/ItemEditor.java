//
// $Id$

package client.editem;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;
import client.util.BorderedDialog;
import client.util.LimitedTextArea;
import client.util.MsoyUI;
import client.util.StyledTabPanel;

/**
 * The base class for an interface for creating and editing digital items.
 */
public abstract class ItemEditor extends BorderedDialog
{
    public static class Binder
    {
        public void textUpdated (String newText) {
        }
        public void valueChanged () {
        }
    }

    public static interface MediaUpdater
    {
        /**
         * Return null, or a message indicating why the specified media will not do.
         *
         * @param name the name of the uploaded file (from the user's local filesystem).
         * @param desc a media descriptor referencing the uploaded media.
         * @param width if the media is a non-thumbnail image this will contain the width of the
         * image, otherwise zero.
         * @param height if the media is a non-thumbnail image this will contain the height of the
         * image, otherwise zero.
         */
        public String updateMedia (String name, MediaDesc desc, int width, int height);
    }

    /**
     * Creates an item editor interface for items of the specified type.  Returns null if the type
     * is unknown.
     */
    public static ItemEditor createItemEditor (int type, EditorHost host)
    {
        ItemEditor editor = null;
        if (type == Item.PHOTO) {
            editor = new PhotoEditor();
        } else if (type == Item.DOCUMENT) {
            editor = new DocumentEditor();
        } else if (type == Item.FURNITURE) {
            editor = new FurnitureEditor();
        } else if (type == Item.GAME) {
            editor = new GameEditor();
        } else if (type == Item.AVATAR) {
            editor = new AvatarEditor();
        } else if (type == Item.PET) {
            editor = new PetEditor();
        } else if (type == Item.AUDIO) {
            editor = new AudioEditor();
        } else if (type == Item.VIDEO) {
            editor = new VideoEditor();
        } else if (type == Item.DECOR) {
            editor = new DecorEditor();
        } else if (type == Item.TOY) {
            editor = new ToyEditor();
        } else if (type == Item.LEVEL_PACK) {
            editor = new LevelPackEditor();
        } else if (type == Item.ITEM_PACK) {
            editor = new ItemPackEditor();
        } else if (type == Item.TROPHY_SOURCE) {
            editor = new TrophySourceEditor();
        } else if (type == Item.PRIZE) {
            editor = new PrizeEditor();
        } else if (type == Item.PROP) {
            editor = new PropEditor();
        } else {
            return null; // woe be the caller
        }
        editor.init(host);
        return editor;
    }

    public ItemEditor ()
    {
        super(false);

        // we have to do this wacky singleton crap because GWT and/or JavaScript doesn't seem to
        // cope with our trying to create an anonymous function that calls an instance method on a
        // JavaScript object
        _singleton = this;

        _header.add(_etitle = createTitleLabel("title", "Title"));

        VerticalPanel contents = (VerticalPanel)_contents;
        contents.add(_mediaTabs = new StyledTabPanel());

        // create and populate our info tab
        FlexTable info = new FlexTable();

        // create a name entry field
        addInfoRow(info, CShell.emsgs.editorName(), bind(_name = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _item.name = text;
            }
        }));
        _name.setMaxLength(Item.MAX_NAME_LENGTH);

        populateInfoTab(info);
        addDescription(info);
        _mediaTabs.add(info, CShell.emsgs.editorInfoTab());

        // create the rest of the interface
        createInterface(contents, _mediaTabs);

        _footer.add(_esubmit = new Button("submit"));
        _esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });
        Button ecancel;
        _footer.add(ecancel = new Button(CShell.cmsgs.cancel()));
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        });
    }

    // @Override // from BorderedPopup
    public void show ()
    {
        _mediaTabs.selectTab(getDefaultTabIndex());
        super.show();
    }

    // @Override // from BorderedPopup
    protected void onClosed (boolean autoClosed)
    {
        super.onClosed(autoClosed);
        _parent.editComplete(_updatedItem);
    }

    /**
     * Configures this editor with a reference to the item service and its item panel parent.
     */
    public void init (EditorHost parent)
    {
        _parent = parent;
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly constructed if we are
     * using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _etitle.setText((item.itemId <= 0) ?
                        CShell.emsgs.editorUploadTitle() : CShell.emsgs.editorEditTitle());
        _esubmit.setText(CShell.emsgs.editorSave());

        safeSetText(_name, _item.name);
        if (_description != null && _item.description != null) {
            _description.setText(_item.description);
        }

        recheckFurniMedia();
        recheckThumbMedia();
    }

    /**
     * Configures the parent item to use when creating an item that is part of an item suite.
     */
    public void setParentItem (ItemIdent parentItem)
    {
        _parentItem = parentItem;
    }

    /**
     * Returns the currently configured item.
     */
    public Item getItem ()
    {
        return _item;
    }

    /**
     * Creates a blank item for use when creating a new item using this editor.
     */
    public abstract Item createBlankItem ();

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("itemEditorContents");
        return panel;
    }

    /**
     * Derived classes can add additional editable components to the main display or as tabs by
     * overriding this method. Anything added before the call to super will go above the tabs in
     * the contents and before the furniture and thumbnail tabs. Anything added after will go
     * after.
     */
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        createFurniUploader(tabs);
        createThumbUploader(tabs);
        _thumbUploader.setHint(CShell.emsgs.editorThumbHint(
            String.valueOf(MediaDesc.THUMBNAIL_WIDTH), String.valueOf(MediaDesc.THUMBNAIL_HEIGHT)));
    }

    /**
     * Returns the index of the tab to be selected by default when this dialog is shown.
     */
    protected int getDefaultTabIndex ()
    {
        return 0;
    }

    protected void createFurniUploader (TabPanel tabs)
    {
        String title = CShell.emsgs.editorFurniTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CShell.emsgs.errFurniNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, CShell.emsgs.editorFurniTab());
    }

    protected void createThumbUploader (TabPanel tabs)
    {
        String title = CShell.emsgs.editorThumbTitle();
        _thumbUploader = createUploader(Item.THUMB_MEDIA, title, true, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.isImage()) {
                    return CShell.emsgs.errThumbNotImage();
                }
                _item.thumbMedia = desc;
                return null;
            }
        });
        tabs.add(_thumbUploader, CShell.emsgs.editorThumbTab());
    }

    /**
     * All items have an "extra information" tab which by default contains the item description but
     * can be extended by overriding this method.
     */
    protected void populateInfoTab (FlexTable info)
    {
    }

    protected void addDescription (FlexTable info)
    {
        addSpacer(info);

        addInfoRow(info, new Label(CShell.emsgs.editorDescrip()));
        _description = new LimitedTextArea(255);
        _description.getTextArea().setCharacterWidth(40);
        _description.getTextArea().setVisibleLines(3);
        bind(_description.getTextArea(), new Binder() {
            public void textUpdated (String text) {
                _item.description = text;
            }
        });
        addInfoRow(info, _description);
        addInfoTip(info, CShell.emsgs.editorDescripTip());
    }

    protected void safeSetText (TextBoxBase box, String value)
    {
        if (box != null && value != null) {
            box.setText(value);
        }
    }

    /**
     * Helper function for overriders of {@link #populateInfoTab}.
     */
    protected void addInfoRow (FlexTable info, String label, Widget widget)
    {
        int row = info.getRowCount();
        info.setText(row, 0, label);
        // this aims to make the label column skinny; it even works on some browsers...
        info.getFlexCellFormatter().setWidth(row, 0, "50px");
        info.getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        info.setWidget(row, 1, widget);
    }

    /**
     * Helper function for overriders of {@link #populateInfoTab}.
     */
    protected void addInfoRow (FlexTable info, Widget widget)
    {
        int row = info.getRowCount();
        info.setWidget(row, 0, widget);
        info.getFlexCellFormatter().setColSpan(row, 0, 2);
    }

    /**
     * Helper function for overriders of {@link #populateInfoTab}.
     */
    protected void addInfoTip (FlexTable info, String tip)
    {
        int row = info.getRowCount();
        info.setText(row, 0, tip);
        info.getFlexCellFormatter().setStyleName(row, 0, "tipLabel");
        info.getFlexCellFormatter().setWidth(row, 0, "400px"); // wrap long text
        info.getFlexCellFormatter().setColSpan(row, 0, 2);
    }

    protected void addSpacer (FlexTable info)
    {
        int row = info.getRowCount();
        info.setText(row, 0, " ");
        info.getFlexCellFormatter().setStyleName(row, 0, "tipLabel");
        info.getFlexCellFormatter().setHeight(row, 0, "10px");
        info.getFlexCellFormatter().setColSpan(row, 0, 2);
    }

    /**
     * This should be called by item editors that are used for editing media that has a 'main'
     * piece of media.
     */
    protected MediaUploader createMainUploader (String title, MediaUpdater updater)
    {
        return (_mainUploader = createUploader(Item.MAIN_MEDIA, title, false, updater));
    }

    /**
     * This should be called by item editors that are used for editing media that has an additional
     * piece of media in addition to main, furni and thumbnail.
     */
    protected MediaUploader createAuxUploader (String title, MediaUpdater updater)
    {
        return (_auxUploader = createUploader(Item.AUX_MEDIA, title, false, updater));
    }

    /**
     * Creates and configures a media uploader.
     */
    protected MediaUploader createUploader (
        String id, String title, boolean thumbnail, MediaUpdater updater)
    {
        return new MediaUploader(id, title, thumbnail, updater);
    }

    /**
     * Get the MediaUploader with the specified id.
     */
    protected MediaUploader getUploader (String id)
    {
        if (Item.FURNI_MEDIA.equals(id)) {
            return _furniUploader;

        } else if (Item.THUMB_MEDIA.equals(id)) {
            return _thumbUploader;

        } else if (Item.MAIN_MEDIA.equals(id)) {
            return _mainUploader; // could be null...

        } else if (Item.AUX_MEDIA.equals(id)) {
            return _auxUploader; // could be null...

        } else {
            return null;
        }
    }

    /**
     * If an item wishes to use the filename of its primary as a default name for the item if one
     * is not already set, it should call this method with the filename when its primary media is
     * uploaded. This method will massage the supplied name (stripping its extension) and configure
     * it as the item name iff the item has no name already configured.
     */
    protected void maybeSetNameFromFilename (String name)
    {
        if (_name.getText().length() != 0) {
            return;
        }

        // if the name has a path, strip it
        int idx = name.lastIndexOf("\\"); // window
        if (idx != -1) {
            name = name.substring(idx+1);
        }
        idx = name.lastIndexOf("//"); // joonix (and Mac OS X by association)
        if (idx != -1) {
            name = name.substring(idx+1);
        }

        // if the name has a file suffix, strip it
        idx = name.lastIndexOf(".");
        switch (name.length() - idx) {
        case 3:
        case 4:
            name = name.substring(0, idx);
            break;
        }

        // replace _ with space
        name = name.replaceAll("_", " ");

        _name.setText(name); // this doesn't trigger a text edited event
        _item.name = name; // so we have to do this by hand
    }

    /**
     * Configures this item editor with the hash value for media that it is about to upload.
     */
    protected void setHash (
        String id, String mediaHash, int mimeType, int constraint, int width, int height,
        String thumbMediaHash, int thumbMimeType, int thumbConstraint)
    {
        MediaUploader mu = getUploader(id);
        if (mu == null) {
            CShell.log("Got setHash() request for unknown uploader [id=" + id + "].");
            return;
        }

        // set the new media in preview and in the item
        mu.setUploadedMedia(
            new MediaDesc(mediaHash, (byte)mimeType, (byte)constraint), width, height);

        // if we got thumbnail media back from this upload, use that as well
        // TODO: avoid overwriting custom thumbnail, sigh
        if (thumbMediaHash.length() > 0) {
            _item.thumbMedia = new MediaDesc(
                thumbMediaHash, (byte)thumbMimeType, (byte)thumbConstraint);
        }

        // have the item re-validate that no media ids are duplicated unnecessarily
        _item.checkConsolidateMedia();

        // re-check the other two, as they may have changed
        if (!Item.THUMB_MEDIA.equals(id)) {
            recheckThumbMedia();
        }
        if (!Item.FURNI_MEDIA.equals(id)) {
            recheckFurniMedia();
        }
    }

    /**
     * Called to re-set the displayed furni media to the MediaDesc returned by the item.
     */
    protected void recheckFurniMedia ()
    {
        if (_furniUploader != null) {
            _furniUploader.setMedia(_item.getFurniMedia());
        }
    }

    /**
     * Called to re-set the displayed thumb media to the MediaDesc returned by the item.
     */
    protected void recheckThumbMedia ()
    {
        if (_thumbUploader != null) {
            _thumbUploader.setMedia(_item.getThumbnailMedia());
        }
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server as a response to our file upload POST request.
     */
    protected static void callBridge (
        String id, String mediaHash, int mimeType, int constraint, int width, int height,
        String thumbMediaHash, int thumbMimeType, int thumbConstraint)
    {
        _singleton.setHash(id, mediaHash, mimeType, constraint, width, height,
                           thumbMediaHash, thumbMimeType, thumbConstraint);
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void uploadError ()
    {
        MsoyUI.error(CShell.emsgs.errUploadError());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that the upload was too large.
     */
    protected static void uploadTooLarge ()
    {
        MsoyUI.error(CShell.emsgs.errUploadTooLarge());
    }

    /**
     * Called when the user clicks the "save" button to commit their edits or create a new item.
     */
    protected void commitEdit ()
    {
        try {
            prepareItem();
        } catch (Exception e) {
            MsoyUI.error(e.getMessage());
            return;
        }

        // make sure the item is consistent
        if (_item == null || !_item.isConsistent()) {
            MsoyUI.error(CShell.emsgs.editorNotConsistent());
            return;
        }

        AsyncCallback cb = new AsyncCallback() {
            public void onSuccess (Object result) {
                if (_item.itemId == 0) {
                    MsoyUI.info(CShell.emsgs.msgItemCreated());
                    _item.itemId = ((Integer)result).intValue();
                } else {
                    MsoyUI.info(CShell.emsgs.msgItemUpdated());
                }
                _updatedItem = _item; // this will be passed to our parent in onClosed()
                hide();
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CShell.serverError(caught));
            }
        };
        if (_item.itemId == 0) {
            CShell.itemsvc.createItem(CShell.ident, _item, _parentItem, cb);
        } else {
            CShell.itemsvc.updateItem(CShell.ident, _item, cb);
        }
    }

    /**
     * Called when the user clicks create or update. The editor should flush any interface element
     * settings back to the item being edited and return null if the item is ready for editing. It
     * can throw an exception with a message to display to the user if the commit should be aborted
     * due to invalid or missing data.
     */
    protected void prepareItem ()
        throws Exception
    {
    }

    /**
     * A convenience method for attaching a textbox directly to a field in the item to be edited.
     *
     * TODO: If you paste text into the field, this doesn't detect it.
     */
    protected Widget bind (Widget widget, final Binder binder)
    {
        if (widget instanceof TextBoxBase) {
            final TextBoxBase textbox = (TextBoxBase)widget;
            textbox.addKeyboardListener(new KeyboardListenerAdapter() {
                public void onKeyPress (Widget sender, char keyCode, int mods) {
                    if (_item != null) {
                        DeferredCommand.add(new Command() {
                            public void execute () {
                                binder.textUpdated(textbox.getText());
                            }
                        });
                    }
                }
            });

        } else if (widget instanceof ListBox) {
            ((ListBox)widget).addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    if (_item != null) {
                        binder.valueChanged();
                    }
                }
            });
        }
        return widget;
    }

    /**
     * Makes sure that the specified text is not null or all-whitespace and is less than or equal
     * to the specified maximum length.  Usually used in {@link #prepareItem}.
     */
    protected static boolean nonBlank (String text, int maxLength)
    {
        text = (text == null) ? "" : text.trim();
        return (text.length() > 0 && text.length() <= maxLength);
    }

    /**
     * This wires up a sensibly named function that our POST response JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (id, hash, type, constraint, width, height, thash, ttype, tconstraint) {
           @client.editem.ItemEditor::callBridge(Ljava/lang/String;Ljava/lang/String;IIIILjava/lang/String;II)(id, hash, type, constraint, width, height, thash, ttype, tconstraint);
        };
        $wnd.uploadError = function () {
           @client.editem.ItemEditor::uploadError()();
        };
        $wnd.uploadTooLarge = function () {
           @client.editem.ItemEditor::uploadTooLarge()();
        };
    }-*/;

    protected EditorHost _parent;

    protected TabPanel _mediaTabs;
    protected Item _item, _updatedItem;
    protected ItemIdent _parentItem;

    protected VerticalPanel _content;

    protected Label _etitle;
    protected TextBox _name;
    protected LimitedTextArea _description;
    protected Button _esubmit;

    protected static ItemEditor _singleton;

    protected MediaUploader _thumbUploader;
    protected MediaUploader _furniUploader;
    protected MediaUploader _mainUploader;
    protected MediaUploader _auxUploader;
}
