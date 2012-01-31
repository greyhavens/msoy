//
// $Id$

package client.editem;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.BorderedPopup;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.StyledTabPanel;
import client.util.InfoCallback;
import client.util.TextBoxUtil;

/**
 * The base class for an interface for creating and editing digital items.
 */
public abstract class ItemEditor extends FlowPanel
{
//    public enum FileType
//    {
//        SWF(_emsgs.typeSWF(), "*.swf"),
//        ABC(_emsgs.typeABC(), "*.abc"),
//        ;
//
//        FileType (String name, String filespec)
//        {
//            _name = name;
//            _spec = filespec;
//        }
//
//        /**
//         * Get the name of this filetype.
//         */
//        public String getName ()
//        {
//            return _name;
//        }
//
//        /**
//         * Get the filespec.
//         */
//        public String getSpec ()
//        {
//            return _spec;
//        }
//
//        protected String _name, _spec;
//    }

    // Note: these strings are passed directly to the uploader to restrict the files being uploaded
    public static final String TYPE_SWF = "*.swf";
    public static final String TYPE_ABC = "*.abc";
    public static final String TYPE_IMAGE = "*.png;*.gif;*.jpg;*.jpeg";
    public static final String TYPE_AUDIO = "*.mp3";
    public static final String TYPE_VIDEO = "*.flv";
    public static final String TYPE_FLASH = TYPE_SWF + ";" + TYPE_IMAGE;
    public static final String TYPE_FLASH_REMIXABLE = TYPE_FLASH + ";*.zip";
    public static final String TYPE_FLASH_ONLY_REMIXABLE = "*.swf;*.zip"; // no images
    public static final String TYPE_ANY = "*";

    public static class Binder
    {
        public void textUpdated (String newText) {
        }
        public void valueChanged () {
        }
    }

    public static interface MediaUpdater
    {
        public static final String SUPPRESS_ERROR = "_se_";

        /**
         * Return null if ok, or SUPPRESS_ERROR, or an error message indicating why
         * the specified media will not do.
         *
         * @param name the name of the uploaded file (from the user's local filesystem).
         * @param desc a media descriptor referencing the uploaded media.
         * @param width if the media is a non-thumbnail image this will contain the width of the
         * image, otherwise zero.
         * @param height if the media is a non-thumbnail image this will contain the height of the
         * image, otherwise zero.
         */
        String updateMedia (String name, MediaDesc desc, int width, int height);

        /**
         * Clears out the media configured for this slot.
         */
        void clearMedia ();
    }

    /**
     * Creates an item editor interface for items of the specified type.  Returns null if the type
     * is unknown.
     */
    public static ItemEditor createItemEditor (MsoyItemType type, EditorHost host)
    {
        ItemEditor editor = null;
        if (type == MsoyItemType.PHOTO) {
            editor = new PhotoEditor();
        } else if (type == MsoyItemType.DOCUMENT) {
            editor = new DocumentEditor();
        } else if (type == MsoyItemType.FURNITURE) {
            editor = new FurnitureEditor();
        } else if (type == MsoyItemType.AVATAR) {
            editor = new AvatarEditor();
        } else if (type == MsoyItemType.PET) {
            editor = new PetEditor();
        } else if (type == MsoyItemType.AUDIO) {
            editor = new AudioEditor();
        } else if (type == MsoyItemType.VIDEO) {
            editor = new VideoEditor();
        } else if (type == MsoyItemType.DECOR) {
            editor = new DecorEditor();
        } else if (type == MsoyItemType.TOY) {
            editor = new ToyEditor();
        } else if (type == MsoyItemType.LEVEL_PACK) {
            editor = new LevelPackEditor();
        } else if (type == MsoyItemType.ITEM_PACK) {
            editor = new ItemPackEditor();
        } else if (type == MsoyItemType.TROPHY_SOURCE) {
            editor = new TrophySourceEditor();
        } else if (type == MsoyItemType.PRIZE) {
            editor = new PrizeEditor();
        } else if (type == MsoyItemType.PROP) {
            editor = new PropEditor();
        } else if (type == MsoyItemType.LAUNCHER) {
            editor = new LauncherEditor();
        } else {
            return null; // woe be the caller
        }
        editor.init(type, host);
        return editor;
    }

    public ItemEditor ()
    {
        // we have to do this wacky singleton crap because GWT and/or JavaScript doesn't seem to
        // cope with our trying to create an anonymous function that calls an instance method on a
        // JavaScript object
        _singleton = this;

        setStyleName("itemEditor");
        add(MsoyUI.createImage("/images/item/editor_box_top.png", "RoundedTop"));
        add(_header = new AbsolutePanel());
        add(_content = new SmartTable("Content", 0, 2));
        _currentTab = _content;
        add(MsoyUI.createImage("/images/item/editor_box_bottom.png", "RoundedBottom"));

        addInfo();
        addExtras();
        addDescription();

        addSpacer();
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        buttons.add(_econfirm = new CheckBox(_emsgs.copyrightConfirm()));
        buttons.add(WidgetUtil.makeShim(5, 5));
        buttons.add(new Button(_cmsgs.cancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                _parent.editComplete(null);
            }
        }));
        buttons.add(WidgetUtil.makeShim(5, 5));
        buttons.add(_esubmit = new Button("submit", new ClickHandler() {
            public void onClick (ClickEvent event) {
                commitEdit();
            }
        }));
        _content.setWidget(_content.getRowCount(), 1, buttons);
        // _content.getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);
    }

    /**
     * Configures this editor with a reference to the item service and its item panel parent.
     */
    public void init (MsoyItemType type, EditorHost parent)
    {
        _type = type;
        _parent = parent;
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly constructed if we are
     * using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _esubmit.setText(_emsgs.editorSave());

        safeSetText(_name, _item.name);
        if (_description != null && _item.description != null) {
            _description.setText(_item.description);
        }

        // build the header now that we know type and whether we are creating or editing
        if (_header.getWidgetCount() == 0) {
            _header.setStyleName("Header");
            String title = _item.itemId == 0
                ? _emsgs.createTitle(_dmsgs.xlateItemType(_type))
                : _emsgs.editTitle(_dmsgs.xlateItemType(_type));
            _header.add(MsoyUI.createLabel(title, "Title"), 15, 0);
            _header.add(MsoyUI.createHTML(_dmsgs.xlateEditorWikiLink(_type), "WikiLink"), 250, 5);
            _header.add(MsoyUI.createImage("/images/item/editor_divider.png", null), 20, 30);
            _header.add(MsoyUI.createHTML(_dmsgs.xlateEditorBlurb(_type), "Blurb"), 15, 40);
        }

        recheckFurniMedia();
        recheckThumbMedia();
    }

    /**
     * Configures the game id to use when creating a game subitem.
     */
    public void setGameId (int gameId)
    {
        // by default we do nothing
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

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    @Override // from Widget
    protected void onUnload ()
    {
        super.onUnload();
        cancelRemix();
    }

    /**
     * Adds the non-media metadata editing fields for this item. By default this is only its name.
     */
    protected void addInfo ()
    {
        addRow(_emsgs.editorName(), bind(_name = new TextBox(), new Binder() {
            @Override public void textUpdated (String text) {
                _item.name = text;
            }
        }));
        _name.setMaxLength(Item.MAX_NAME_LENGTH);
    }

    /**
     * Adds the item description to the editor interface. Items that do not require a description
     * can override this method with a null body.
     */
    protected void addDescription ()
    {
        addSpacer();
        _description = new LimitedTextArea(Item.MAX_DESCRIPTION_LENGTH, 40, 3);
        bind(_description.getTextArea(), new Binder() {
            @Override public void textUpdated (String text) {
                _item.description = text;
            }
        });
        addRow(_emsgs.editorDescrip(), _description, _emsgs.editorDescripTip());
    }

    /**
     * Derived classes can add additional editable components to the display by overriding this
     * method. Anything added before a call to super will go above the furniture and thumbnail
     * image uploaders, anything added after will go after.
     */
    protected void addExtras ()
    {
        addFurniUploader();
        addThumbUploader();
        _uploaders.get(Item.THUMB_MEDIA).setHint(getThumbnailHint());
    }

    /**
     * Returns the hint displayed next to the thumbnail uploader.
     */
    protected String getThumbnailHint ()
    {
        return _emsgs.editorThumbHint(
            String.valueOf(MediaDescSize.THUMBNAIL_WIDTH), String.valueOf(MediaDescSize.THUMBNAIL_HEIGHT));
    }

    protected void addFurniUploader ()
    {
        addSpacer();
        addRow(getFurniTabText(), createFurniUploader(getFurniType(), generateFurniThumbnail(),
            new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    if (!isValidPrimaryMedia(desc)) {
                        return invalidPrimaryMediaMessage();
                    }
                    _item.setFurniMedia(desc);
                    return null;
                }
                public void clearMedia () {
                    _item.setFurniMedia(null);
                }
            }), getFurniTitleText());
    }

    protected String getFurniTabText ()
    {
        return _emsgs.editorFurniTab();
    }

    protected String getFurniType ()
    {
        return TYPE_FLASH_REMIXABLE;
    }

    protected boolean generateFurniThumbnail ()
    {
        return true;
    }

    protected String getFurniTitleText ()
    {
        return _emsgs.editorFurniTitle();
    }

    protected void addThumbUploader ()
    {
        addSpacer();
        addRow(_emsgs.editorThumbTab(), createThumbUploader(new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.isImage()) {
                    return _emsgs.errThumbNotImage();
                }
                _item.setThumbnailMedia(desc);
                return null;
            }
            public void clearMedia () {
                _item.setThumbnailMedia(null);
            }
        }), _emsgs.editorThumbTitle());
    }

    /**
     * Is the specified MediaDesc a valid primary media for item type?
     */
    protected boolean isValidPrimaryMedia (MediaDesc desc)
    {
        switch(_type) {
            case AVATAR: case PET: case TOY:
                // these must be swfs or remixable
                return desc.isSWF() || desc.isRemixed();

            case FURNITURE: case DECOR:
                // these can be swfs, images, or remixable
                return desc.hasFlashVisual() || desc.isRemixed();

            case PHOTO:
                // images must be images... wow
                return desc.isImage();

            default:
                // other types are not yet remixable
                return desc.hasFlashVisual();
        }
    }

    /**
     * String returned if the primary media is invalid. Subclasses should override as appropriate.
     */
    protected String invalidPrimaryMediaMessage ()
    {
        return _emsgs.errFurniNotFlash();
    }

    protected void safeSetText (TextBoxBase box, String value)
    {
        if (box != null && value != null) {
            box.setText(value);
        }
    }

    /**
     * Creates a tab and activates it such that all subsequent configuration values (added with
     * methods like {@link #addRow} and {@link #addTip} are added to the most recently added tab's
     * internal table rather than to the top-level item editor.
     */
    protected void addTab (String label)
    {
        if (_tabs == null) {
            int row = _content.getRowCount();
            _content.setWidget(row, 0, _tabs = new StyledTabPanel(), 2);
            _tabs.setWidth("100%");
        }
        _tabs.add(_currentTab = new FlexTable(), label);
        _tabs.selectTab(0);
    }

    /**
     * Switches to the tab with the specified index. New components added via {@link #addRow},
     * etc. will go onto the specified tab. This of course requires that {@link #addTab} has been
     * called to add the tab in question.
     */
    protected void switchToTab (int tabIdx)
    {
        _currentTab = (FlexTable)_tabs.getWidget(tabIdx);
    }

    /**
     * Helper function for overriders of {@link #addInfo} etc.
     */
    protected void addRow (String label, Widget widget)
    {
        addRow(label, widget, null);
    }

    /**
     * Helper function for overriders of {@link #addInfo} etc.
     */
    protected void addRow (String label, Widget widget, String tip)
    {
        int row = _currentTab.getRowCount();
        // this aims to make the label column skinny; it even works on some browsers...
        _currentTab.getFlexCellFormatter().setWidth(row, 0, "150px");
        if (tip != null) {
            FlowPanel flow = new FlowPanel();
            flow.add(MsoyUI.createLabel(label, "nowrapLabel"));
            flow.add(MsoyUI.createLabel(tip, "tipLabel"));
            _currentTab.setWidget(row, 0, flow);
        } else {
            _currentTab.setText(row, 0, label);
            _currentTab.getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        }
        _currentTab.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
        _currentTab.setWidget(row, 1, widget);
    }

    /**
     * Helper function for overriders of {@link #addInfo} etc.
     */
    protected void addRow (Widget widget)
    {
        int row = _currentTab.getRowCount();
        _currentTab.setWidget(row, 0, widget);
        _currentTab.getFlexCellFormatter().setColSpan(row, 0, 2);
        _currentTab.getFlexCellFormatter().setStyleName(row, 0, "Item");
    }

    /**
     * Helper function for overriders of {@link #addInfo} etc.
     */
    protected void addTip (String tip)
    {
        int row = _currentTab.getRowCount();
        _currentTab.setText(row, 1, tip);
        _currentTab.getFlexCellFormatter().setStyleName(row, 1, "tipLabel");
        _currentTab.getFlexCellFormatter().setWidth(row, 1, "400px");
    }

    /**
     * Helper function for overriders of {@link #addInfo} etc.
     */
    protected void addSpacer ()
    {
        int row = _currentTab.getRowCount();
        _currentTab.setText(row, 0, " ");
        _currentTab.getFlexCellFormatter().setStyleName(row, 0, "tipLabel");
        _currentTab.getFlexCellFormatter().setHeight(row, 0, "10px");
        _currentTab.getFlexCellFormatter().setColSpan(row, 0, 2);
    }

    /**
     * This should be called by item editors that are used for editing media that has a 'main'
     * piece of media.
     * @param isPhoto if True, a thubmnail and 320x200 furni will also be generated and returned
     */
    protected ItemMediaUploader createMainUploader (
        String type, boolean isPhoto, MediaUpdater updater)
    {
        String mediaIds = Item.MAIN_MEDIA;
        mediaIds += isPhoto ? ";" + Item.THUMB_MEDIA + ";" + Item.FURNI_MEDIA : "";
        return createUploader(mediaIds, type, ItemMediaUploader.MODE_NORMAL, updater);
    }

    /**
     * This should be called by item editors that are used for editing media that has an additional
     * piece of media in addition to main, furni and thumbnail.
     */
    protected ItemMediaUploader createAuxUploader (String type, int mode, MediaUpdater updater)
    {
        return createUploader(Item.AUX_MEDIA, type, mode, updater);
    }

    /**
     * This should be called if item editors want to create a custom furni uploader.
     * @param genThumb if True, a thubmnail will also be generated and returned for images
     */
    protected ItemMediaUploader createFurniUploader (
        String type, boolean genThumb, MediaUpdater updater)
    {
        String mediaIds = Item.FURNI_MEDIA;
        mediaIds += genThumb ? ";" + Item.THUMB_MEDIA : "";
        return createUploader(mediaIds, type, ItemMediaUploader.MODE_NORMAL, updater);
    }

    /**
     * This should be called if item editors want to create a custom thumbnail uploader.
     */
    protected ItemMediaUploader createThumbUploader (MediaUpdater updater)
    {
        return createUploader(Item.THUMB_MEDIA, TYPE_IMAGE, ItemMediaUploader.MODE_THUMB, updater);
    }

    /**
     * Creates and configures a media uploader.
     */
    protected ItemMediaUploader createUploader (
        String mediaIds, String type, int mode, MediaUpdater updater)
    {
        ItemMediaUploader uploader = createUploaderWidget(mediaIds, type, mode, updater);
        // record the uploader under the MAIN mediaId.
        _uploaders.put(mediaIds.split(";")[0], uploader);
        return uploader;
    }

    /**
     * Just creates the uploader.
     */
    protected ItemMediaUploader createUploaderWidget (
        String mediaIds, String type, int mode, MediaUpdater updater)
    {
        return new ItemMediaUploader(this, mediaIds, type, mode, updater);
    }

    /**
     * Updates the media displayed by the specified uploader if it exists.
     */
    protected void setUploaderMedia (String id, MediaDesc desc)
    {
        ItemMediaUploader uploader = _uploaders.get(id);
        if (uploader != null) {
            uploader.setMedia(desc);
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
        if (StringUtil.isBlank(name) || (_name.getText().length() != 0)) {
            return;
        }

        // if the name has a path, strip it
        name = name.substring(name.lastIndexOf("\\") + 1); // windows
        name = name.substring(name.lastIndexOf("/") + 1); // unix, MacOS

        // if the name has a file suffix, strip it
        int idx = name.lastIndexOf(".");
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
    protected void setHash (String id, String filename, String mediaHash, int mimeType,
        int constraint, int expiration, String signature, int width, int height)
    {
        ItemMediaUploader mu = _uploaders.get(id);
        if (mu == null) {
            CShell.log("Got setHash() request for unknown uploader [id=" + id + "].");
            return;
        }

        // shut down the remixer, if any
        cancelRemix();

        // set the new media in preview and in the item
        MediaDesc desc = new CloudfrontMediaDesc(HashMediaDesc.stringToHash(mediaHash),
            (byte)mimeType, (byte)constraint, expiration, signature);
        mu.setUploadedMedia(filename, desc, width, height);

        // have the item re-validate that no media ids are duplicated unnecessarily
        _item.checkConsolidateMedia();

        // re-check the furni image as it may have changed
        if (!Item.FURNI_MEDIA.equals(id)) {
            recheckFurniMedia();
        }
    }

    protected void takeSnapshot (String ids)
    {
        ItemMediaUploader mu = _uploaders.get(ids.split(";")[0]);
        if (mu != null) {
            mu.openImageEditor(null, true);
        }
    }

    /**
     * Called to re-set the displayed furni media to the MediaDesc returned by the item.
     */
    protected void recheckFurniMedia ()
    {
        setUploaderMedia(Item.FURNI_MEDIA, _item.getFurniMedia());
    }

    /**
     * Called to re-set the displayed thumb media to the MediaDesc returned by the item.
     */
    protected void recheckThumbMedia ()
    {
        setUploaderMedia(Item.THUMB_MEDIA, _item.getThumbnailMedia());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server as a response to our file upload POST request.
     */
    protected static void callBridge (
        String id, String filename, String mediaHash, int mimeType, int constraint,
        int expiration, String signature, int width, int height)
    {
        // for some reason the strings that come in from JavaScript are not "real" and if we just
        // pass them straight on through to GWT, freakoutery occurs (of the non-hand-waving
        // variety); so we convert them hackily to GWT strings here
        _singleton.setHash("" + id, "" + filename, "" + mediaHash, mimeType, constraint,
            expiration, signature, width, height);
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void uploadError ()
    {
        MsoyUI.error(_emsgs.errUploadError());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that the upload was too large.
     */
    protected static void uploadTooLarge ()
    {
        MsoyUI.error(_emsgs.errUploadTooLarge());
    }

    protected static void cancelRemix ()
    {
        if (_remixPopup != null) {
            _remixPopup.removeFromParent();
            _remixPopup = null;
        }
    }

    protected static void takeSnapshotBridge (String ids)
    {
        String fids = "" + ids;
        _singleton.takeSnapshot(fids);
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
            MsoyUI.error(_emsgs.editorNotConsistent());
            return;
        }

        // make sure they claim to own all of the media in the item
        if (!_econfirm.getValue()) {
            MsoyUI.error(_emsgs.mustConfirm());
            return;
        }

        if (_item.itemId == 0) {
            _stuffsvc.createItem(_item, new InfoCallback<Item>() {
                public void onSuccess (Item item) {
                    editComplete(item, true);
                }
            });

        } else {
            _stuffsvc.updateItem(_item, new InfoCallback<Void>() {
                public void onSuccess (Void result) {
                    editComplete(_item, false);
                }
            });
        }
    }

    /**
     * Called once the item is saved to the server.
     */
    protected void editComplete (Item item, boolean created)
    {
        MsoyUI.info(created ? _emsgs.msgItemCreated() : _emsgs.msgItemUpdated());
        _parent.editComplete(item);
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
     */
    protected Widget bind (Widget widget, final Binder binder)
    {
        if (widget instanceof TextBoxBase) {
            final TextBoxBase textbox = (TextBoxBase)widget;
            textbox.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    if (_item != null) {
                        binder.textUpdated(textbox.getText());
                    }
                }
            });

        } else if (widget instanceof ListBox) {
            ((ListBox)widget).addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    if (_item != null) {
                        binder.valueChanged();
                    }
                }
            });
        }
        return widget;
    }

    /**
     * Some item types allow images to be converted into "easy items".
     * Prompt to see if the user is ready to remix.
     */
    protected void promptEasyItem (
        final String mediaId, final MediaDesc prototype, final MediaDesc image,
        String prompt, String details)
    {
        new PromptPopup(prompt, new Command() {
            public void execute () {
                doEasyRemix(mediaId, prototype, image);
            }
        }).setContext(details).prompt();
    }

    protected void doEasyRemix (String mediaId, MediaDesc prototype, MediaDesc image)
    {
        int popWidth = getOffsetWidth() - 8;
        int popHeight = Math.max(550,
            Math.min(getOffsetHeight() - 8, Window.getClientHeight() - 8));
        String typename = _item.getType().typeName();
        String flashVars = "media=" + URL.encodeComponent(prototype.getMediaPath()) +
            "&type=" + URL.encodeComponent(typename) +
            "&name=" + URL.encodeComponent(StringUtil.getOr(_item.name, typename)) +
            "&server=" + URL.encodeComponent(DeploymentConfig.serverURL) +
            "&mediaId="+ URL.encodeComponent(mediaId) +
            "&auth=" + URL.encodeComponent(CShell.getAuthToken()) +
            "&noPickPhoto=true" + // suppress picking a photo from inventory, here
            "&forceMimeType=" + MediaMimeTypes.APPLICATION_ZIP_NOREMIX +
            "&inject-image=" + URL.encodeComponent(image.getMediaPath());
        if (_item.getType() != MsoyItemType.AVATAR) {
            flashVars += "&username=Tester";
        }

        _remixPopup = new BorderedPopup(false, true);
        _remixPopup.setWidget(WidgetUtil.createFlashContainer("remixControls",
            "/clients/" + DeploymentConfig.version + "/remixer.swf",
            popWidth, popHeight, flashVars));
        _remixPopup.show();
    }

    /**
     * Makes sure that the specified text is not null or all-whitespace and is less than or equal
     * to the specified maximum length.  Usually used in {@link #prepareItem}.
     */
    protected static boolean nonBlank (String text, int maxLength)
    {
        if (text == null) {
            return false;
        }
        int len = text.trim().length();
        return (len > 0) && (len <= maxLength);
    }

    /**
     * This wires up a sensibly named function that our POST response JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (id, filename, hash, type, constraint,
            expiration, signature, width, height) {
            @client.editem.ItemEditor::callBridge(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIILjava/lang/String;II)(
                 id, filename, hash, type, constraint, expiration, signature, width, height);
        };
        $wnd.uploadError = function () {
            @client.editem.ItemEditor::uploadError()();
        };
        $wnd.uploadTooLarge = function () {
            @client.editem.ItemEditor::uploadTooLarge()();
        };
        $wnd.cancelRemix = function () {
            @client.editem.ItemEditor::cancelRemix()();
        };
        $wnd.takeSnapshot = function (ids) {
            @client.editem.ItemEditor::takeSnapshotBridge(Ljava/lang/String;)(ids);
        };
    }-*/;

    /** The type of items we're editing here. */
    protected MsoyItemType _type;
    protected EditorHost _parent;

    protected Item _item;

    protected TextBox _name;
    protected LimitedTextArea _description;
    protected CheckBox _econfirm;
    protected Button _esubmit;

    protected AbsolutePanel _header;
    protected StyledTabPanel _tabs;
    protected FlexTable _currentTab;
    protected SmartTable _content;

    protected Map<String, ItemMediaUploader> _uploaders = Maps.newHashMap();

    protected static ItemEditor _singleton;

    protected static BorderedPopup _remixPopup;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffServiceAsync _stuffsvc = GWT.create(StuffService.class);
}
