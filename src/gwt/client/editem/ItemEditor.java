//
// $Id$

package client.editem;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.util.BorderedDialog;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * The base class for an interface for creating and editing digital items.
 */
public abstract class ItemEditor extends BorderedDialog
{
    public static interface Binder
    {
        public void textUpdated (String newText);
    }

    public static interface MediaUpdater
    {
        /**
         * Return null, or a message indicating why the specified media will not do.
         *
         * @param width if the media is a (non-thumbnail) image this will contain the width of the
         * image, otherwise zero.
         * @param height if the media is a (non-thumbnail) image this will contain the height of
         * the image, otherwise zero.
         */
        public String updateMedia (MediaDesc desc, int width, int height);
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

        _header.add(_etitle = MsoyUI.createLabel("title", "Title"));

        VerticalPanel contents = (VerticalPanel)_contents;
        contents.setSpacing(10);
        TabPanel mediaTabs = createTabs();

        // create a name entry field
        contents.add(
            createRow(CEditem.emsgs.editorName(), bind(_name = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _item.name = text;
            }
        })));

        createInterface(contents, mediaTabs);

        // start with main selected
        mediaTabs.selectTab(0);

//         // the main tab will contain the base metadata and primary media uploader
//         VerticalPanel main = new VerticalPanel();
//         main.setStyleName("Tab");
//         createMainInterface(main);
//         tabs.add(main, "Main");

//         // the extra tab will contain the furni and thumbnail media and description
//         VerticalPanel extra = new VerticalPanel();
//         extra.setStyleName("Tab");
//         createExtraInterface(extra);
//         tabs.add(extra, "Extra");

        _footer.add(_esubmit = new Button("submit"));
        _esubmit.setEnabled(false);
        _esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });
        Button ecancel;
        _footer.add(ecancel = new Button(CEditem.cmsgs.cancel()));
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                hide();
            }
        });
    }

    // @Override // from BorderedPopup
    protected void onClosed (boolean autoClosed)
    {
        super.onClosed(autoClosed);
        _parent.editComplete(_updatedItem);
    }

    /**
     * Configures this editor with a reference to the item service and its item
     * panel parent.
     */
    public void init (EditorHost parent)
    {
        _parent = parent;
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly
     * constructed if we are using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _etitle.setText((item.itemId <= 0) ?
                        CEditem.emsgs.editorUploadTitle() : CEditem.emsgs.editorEditTitle());
        _esubmit.setText((item.itemId <= 0) ?
                         CEditem.emsgs.editorUpload() : CEditem.emsgs.editorUpdate());

        if (_item.name != null) {
            _name.setText(_item.name);
        }
        if (_item.description != null) {
            _description.setText(_item.description);
        }

        recheckFurniMedia();
        recheckThumbMedia();

        updateSubmittable();
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
        VerticalPanel contents = new VerticalPanel();
        contents.setStyleName("itemEditor");
        return contents;
    }

    /**
     * Derived classes can add additional editable components to the main display or as tabs by
     * overriding this method. Anything added before the call to super will go above the tabs in
     * the contents and before the furniture and thumbnail tabs. Anything added after will go
     * after.
     */
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        contents.add(tabs);

        createFurniUploader(tabs);
        createThumbUploader(tabs);

        VerticalPanel extras = new VerticalPanel();
        extras.setSpacing(10);
        populateExtrasTab(extras);
        tabs.add(extras, CEditem.emsgs.editorExtraTab());
    }

    protected void createFurniUploader (TabPanel tabs)
    {
        String title = CEditem.emsgs.editorFurniTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CEditem.emsgs.errFurniNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, CEditem.emsgs.editorFurniTab());
    }

    protected void createThumbUploader (TabPanel tabs)
    {
        String title = CEditem.emsgs.editorThumbTitle();
        _thumbUploader = createUploader(Item.THUMB_MEDIA, title, true, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.isImage()) {
                    return CEditem.emsgs.errThumbNotImage();
                }
                _item.thumbMedia = desc;
                return null;
            }
        });
        tabs.add(_thumbUploader, CEditem.emsgs.editorThumbTab());
    }

    /**
     * All items have an "extra information" tab which by default contains the item description but
     * can be extended by overriding this method.
     */
    protected void populateExtrasTab (VerticalPanel extras)
    {
        extras.add(new Label(CEditem.emsgs.editorDescripTitle()));
        extras.add(bind(_description = new TextArea(), new Binder() {
            public void textUpdated (String text) {
                _item.description = text;
            }
        }));
        _description.setCharacterWidth(40);
        _description.setVisibleLines(3);
    }

    /**
     * Derived classes can add editors to the main tab by overriding this method.
     */
    protected void createExtraInterface (VerticalPanel extra)
    {
    }

    protected RowPanel createRow (String label, Widget widget)
    {
        RowPanel row = new RowPanel();
        row.add(new Label(label));
        row.add(widget);
        return row;
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
     * Creates and configures a media uploader.
     */
    protected MediaUploader createUploader (
        String id, String title, boolean thumbnail, MediaUpdater updater)
    {
        return new MediaUploader(id, title, thumbnail, updater);
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

        } else {
            return null;
        }
    }

    /**
     * Configures this item editor with the hash value for media that it is
     * about to upload.
     */
    protected void setHash (
        String id, String mediaHash, int mimeType, int constraint, int width, int height,
        String thumbMediaHash, int thumbMimeType, int thumbConstraint)
    {
        MediaUploader mu = getUploader(id);
        if (mu == null) {
            return; // TODO: log something? in gwt land?
        }

        // set the new media in preview and in the item
        mu.setUploadedMedia(new MediaDesc(MediaDesc.stringToHash(mediaHash), (byte)mimeType,
                                          (byte)constraint), width, height);

        // if we got thumbnail media back from this upload, use that as well
        // TODO: avoid overwriting custom thumbnail, sigh
        if (thumbMediaHash.length() > 0) {
            _item.thumbMedia = new MediaDesc(
                MediaDesc.stringToHash(thumbMediaHash), (byte)thumbMimeType, (byte)thumbConstraint);
        }

        // have the item re-validate that no media ids are duplicated
        // unnecessarily
        _item.checkConsolidateMedia();

        // re-check the other two, as they may have changed
        if (!Item.THUMB_MEDIA.equals(id)) {
            recheckThumbMedia();
        }
        if (!Item.FURNI_MEDIA.equals(id)) {
            recheckFurniMedia();
        }

        // re-check submittable
        updateSubmittable();
    }

    /**
     * Called to re-set the displayed furni media to the MediaDesc
     * returned by the item.
     */
    protected void recheckFurniMedia ()
    {
        if (_furniUploader != null) {
            _furniUploader.setMedia(_item.getFurniMedia());
        }
    }

    /**
     * Called to re-set the displayed thumb media to the MediaDesc
     * returned by the item.
     */
    protected void recheckThumbMedia ()
    {
        if (_thumbUploader != null) {
            _thumbUploader.setMedia(_item.getThumbnailMedia());
        }
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code
     * received from the server as a response to our file upload POST request.
     */
    protected static void callBridge (
        String id, String mediaHash, int mimeType, int constraint, int width, int height,
        String thumbMediaHash, int thumbMimeType, int thumbConstraint)
    {
        _singleton.setHash(id, mediaHash, mimeType, constraint, width, height,
                           thumbMediaHash, thumbMimeType, thumbConstraint);
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
        AsyncCallback cb = new AsyncCallback() {
            public void onSuccess (Object result) {
                _parent.setStatus(_item.itemId == 0 ?
                                  CEditem.emsgs.msgItemCreated() : CEditem.emsgs.msgItemUpdated());
                _updatedItem = _item; // this will be passed to our parent in onClosed()
                hide();
            }
            public void onFailure (Throwable caught) {
                _parent.setStatus(CEditem.serverError(caught));
            }
        };
        if (_item.itemId == 0) {
            CEditem.itemsvc.createItem(CEditem.creds, _item, cb);
        } else {
            CEditem.itemsvc.updateItem(CEditem.creds, _item, cb);
        }
    }

    /**
     * A convenience method for attaching a textbox directly to a field in the item to be edited.
     *
     * TODO: If you paste text into the field, this doesn't detect it.
     */
    protected TextBoxBase bind (final TextBoxBase textbox, final Binder binder)
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
        return textbox;
    }

    /**
     * This wires up a sensibly named function that our POST response JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (id, hash, type, constraint, width, height, thash, ttype, tconstraint) {
           @client.editem.ItemEditor::callBridge(Ljava/lang/String;Ljava/lang/String;IIIILjava/lang/String;II)(id, hash, type, constraint, width, height, thash, ttype, tconstraint);
        };
    }-*/; 

    protected EditorHost _parent;

    protected Item _item, _updatedItem;

    protected VerticalPanel _content;

    protected Label _etitle;
    protected TextBox _name;
    protected TextArea _description;
    protected Button _esubmit;

    protected static ItemEditor _singleton;

    protected MediaUploader _thumbUploader;
    protected MediaUploader _furniUploader;
    protected MediaUploader _mainUploader;
}
