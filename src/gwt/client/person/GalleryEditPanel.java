//
// $Id$

package client.person;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.allen_sauer.gwt.dnd.client.drop.SimpleDropController;

import com.threerings.gwt.ui.CenteredBox;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.imagechooser.gwt.ImageChooserService;
import com.threerings.msoy.imagechooser.gwt.ImageChooserServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.dnd.DropListener;
import client.dnd.DropModel;
import client.dnd.DropPanel;
import client.dnd.PayloadWidget;
import client.dnd.SimpleDropModel;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.InfoCallback;

/**
 * Allows a user to edit one of their galleries.
 *
 * @author mjensen
 */
public class GalleryEditPanel extends AbsolutePanel // AbsolutePanel needed to support drag-n-drop
{
    /**
     * Creating a new Gallery
     */
    public GalleryEditPanel (boolean isProfile)
    {
        // create a new gallery
        _newGallery = true;
        GalleryData galleryData = new GalleryData();
        galleryData.gallery = new Gallery();
        galleryData.owner = CShell.creds.name;
        if (isProfile) {
            // null name is the indicator for profile galleries
            galleryData.gallery.name = null;
        } else {
            galleryData.gallery.name = _pmsgs.galleryNewName();
        }
        galleryData.gallery.description = "";
        galleryData.photos = new ArrayList<Photo>();
        display(galleryData);
    }

    /**
     * Editing an existing gallery
     */
    public GalleryEditPanel (int galleryId)
    {
        _gallerysvc.loadGallery(galleryId, new InfoCallback<GalleryData>() {
            public void onSuccess (GalleryData result) {
                display(result);
            }
        });
    }

    protected void display (GalleryData galleryData)
    {
        _galleryData = galleryData;
        addStyleName("galleryEditPanel");

        _dragController = new PickupDragController(this, false);
        _dragController.setBehaviorDragProxy(false);
        _dragController.setBehaviorMultipleSelection(true);

        // add editable gallery detail panel
        final GalleryDetailEditPanel detailPanel = new GalleryDetailEditPanel(_galleryData,
            _dragController);
        add(detailPanel, 0, 10);

        // add "save" and "cancel" buttons
        PushButton saveButton = MsoyUI.createButton(MsoyUI.MEDIUM_THIN,
            _pmsgs.gallerySaveButton(),
            new ClickHandler() {
                public void onClick (ClickEvent event) {
                    final PushButton button = (PushButton) event.getSource();
                    button.setEnabled(false);
                    saveGallery(true, button);
                }
            }
        );
        saveButton.addStyleName("Save");
        add(saveButton, 575, 45);

        Button cancelButton = new Button("Cancel", new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (_newGallery) {
                    Link.go(Pages.PEOPLE, GalleryActions.GALLERIES, CShell.getMemberId());
                } else {
                    Link.go(Pages.PEOPLE, GalleryActions.VIEW, _galleryData.gallery.galleryId);
                }
            }
        });
        cancelButton.setWidth("90px");
        add(cancelButton, 575, 80);

        // instructions for drop box will be removed when the first content is inserted
        final FlowPanel dragInstructions = MsoyUI.createFlowPanel("DragInstructions");

        // add drop panel for adding to and organizing this gallery
        SimpleDropModel<Photo> dropModel = new SimpleDropModel<Photo>(galleryData.photos);
        dropModel.addDropListener(new DropListener<Photo>() {
            public void contentInserted (DropModel<Photo> model, Photo content, int index) {
                dragInstructions.clear();
                updateCount(model);
                _galleryData.hasUnsavedChanges = true;
            }
            public void contentRemoved (DropModel<Photo> model, Photo content) {
                updateCount(model);
                _galleryData.hasUnsavedChanges = true;
            }
            protected void updateCount (DropModel<Photo> model) {
                detailPanel.setCount(model.getContents().size());
            }
        });
        DropPanel<Photo> dropPanel = new DropPanel<Photo>(_dragController, dropModel) {
            @Override protected Widget createWidget (Photo photo) {
                return new CenteredBox(MediaUtil.createMediaView(photo.getThumbnailMedia(),
                    MediaDescSize.THUMBNAIL_SIZE), "ThumbnailImage");
            }
        };
        add(dropPanel, 0, 120);

        // instructions for drop box are added after dropModel
        if (_galleryData.photos.size() == 0) {
            dragInstructions.add(MsoyUI.createLabel(_pmsgs.galleryDragInstructions1(),
                "DragInstructions1"));
            dragInstructions.add(MsoyUI.createLabel(_pmsgs.galleryDragInstructions2(),
                "DragInstructions2"));
            add(dragInstructions, 200, 180);
        }

        // show all photos that the member owns
        final SimplePanel photoContainer = new SimplePanel();
        add(photoContainer, 0, 370);

        // only populate the box if it's a member editing their own gallery (i.e. not support)
        if (CShell.getMemberId() == _galleryData.owner.getMemberId()) {
            _imgsvc.loadPhotos(new InfoCallback<List<Photo>>() {
                public void onSuccess (List<Photo> result) {
                    createPhotoBox(photoContainer, result);
                }
            });
        } else {
            // otherwise create the box explicitly with an empty result
            createPhotoBox(photoContainer, new ArrayList<Photo>());
        }

        // "your photos" title goes over photo list
        add(new Image("/images/people/gallery_photo_icon.png"), 10, 375);

        String photoBoxTitle;
        if (CShell.getMemberId() == _galleryData.owner.getMemberId()) {
            photoBoxTitle = _pmsgs.galleryPhotoListTitle();
        } else {
            photoBoxTitle = "Drag here to remove from gallery.";
        }
        add(MsoyUI.createLabel(photoBoxTitle, "PhotoListTitle"), 65, 385);

        // if we're support, we're done
        if (CShell.getMemberId() != _galleryData.owner.getMemberId()) {
            return;
        }

        // button link to photo upload panel goes over photo list
        add(MsoyUI.createActionLabel(_pmsgs.galleryUploadPhotos(), "UploadPhotos",
            new ClickHandler() {
                public void onClick (ClickEvent event) {
                    // save the gallery before leaving this page
                    saveGallery(false, null);
                    Link.go(Pages.STUFF, "c", Item.PHOTO);
                }
            }), 210, 395);
    }

    protected void createPhotoBox (final SimplePanel photoContainer, List<Photo> result)
    {
        final PhotoList myPhotos = new PhotoList(new SimpleDataModel<Photo>(result), 12);
        photoContainer.setWidget(myPhotos);
        // allow photos to get dropped onto this panel (to remove them from a gallery)
        _dragController.registerDropController(
            new SimpleDropController(GalleryEditPanel.this) {
                @Override public void onPreviewDrop(DragContext context)
                throws VetoDragException {
                    super.onPreviewDrop(context);
                    if (context.draggable instanceof PayloadWidget<?>) {
                        PayloadWidget<?> payload = (PayloadWidget<?>) context.draggable;
                        // Veto any attempts to drop photos that came from the "my photos"
                        // panel. This will position them back where they came from rather
                        // than having them vanish into the ether.
                        if (payload.getSource() == myPhotos) {
                            throw new VetoDragException();
                        }
                    }
                }
                @Override public void onDrop(DragContext context) {
                    super.onDrop(context);
                    for (Widget widget : context.selectedWidgets) {
                        // to the ether with you
                        remove(widget);
                    }
                }
            }
        );
    }

    /**
     * Create a new gallery or update an existing one.
     * @param backToView if true, player will be returned to gallery view on success.
     * @param saveButton if supplied, this will be enabled on success.
     */
    protected void saveGallery (final boolean backToView, final PushButton saveButton)
    {
        if (!_galleryData.hasUnsavedChanges) {
            saveComplete(backToView, saveButton);
            return;
        }

        if (_newGallery) {
            _gallerysvc.createGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                new InfoCallback<Gallery>() {
                    public void onSuccess (Gallery result) {
                        _newGallery = false;
                        _galleryData.gallery = result;
                        saveComplete(backToView, saveButton);
                    }
                }
            );
        } else {
            _gallerysvc.updateGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                new InfoCallback<Void>() {
                    public void onSuccess (Void result) {
                        saveComplete(backToView, saveButton);
                    }
                }
            );
        }
    }

    /**
     * Called when save is finished, enable save button and/or redirect to the gallery view.
     */
    protected void saveComplete (boolean backToView, PushButton saveButton)
    {
        _galleryData.hasUnsavedChanges = false;
        if (saveButton != null) {
            saveButton.setEnabled(true);
        }
        if (backToView) {
            Link.go(Pages.PEOPLE, GalleryActions.VIEW, _galleryData.gallery.galleryId);
        }
    }

    /**
     * A paginated list of Photos.
     * TODO add page numbers and disable prev/next at appropriate times.
     */
    protected class PhotoList extends FlowPanel {
        public PhotoList (DataModel<Photo> model, int count) {
            _model = model;
            _count = count;
            addStyleName("PhotoList");
            _prevNext = MsoyUI.createPrevNextButtons(
                new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        prev();
                    }
                },
                new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        next();
                    }
                });
            _prevNext.addStyleName("PrevNextButtons");
            display();
        }

        public void next () {
            if (_page + 1 < getPageCount()) {
                _page++;
                display();
            }
        }

        public void prev () {
            if (_page != 0) {
                _page--;
                display();
            }
        }

        public int getPageCount () {
            int itemCount = _model.getItemCount();
            int pageCount =  itemCount / _count;
            if (itemCount % _count > 0) {
                pageCount++;
            }
            return pageCount;
        }

        protected void display () {
            clear();
            _model.doFetchRows(_page * _count, _count, new InfoCallback<List<Photo>>() {
                public void onSuccess (List<Photo> result) {
                    add(_prevNext);
                    for (Photo photo : result) {
                        add(createWidget(photo));
                    }
                }
            });
        }

        protected Widget createWidget (Photo photo) {
            PayloadWidget<Photo> payload = new PayloadWidget<Photo>(
                this, new CenteredBox(MediaUtil.createMediaView(
                    photo.getThumbnailMedia(), MediaDescSize.THUMBNAIL_SIZE),
                "ThumbnailImage"), photo);
            _dragController.makeDraggable(payload);
            return payload;
        }

        protected int _page;
        protected int _count;
        protected DataModel<Photo> _model;
        protected Widget _prevNext;
    }

    protected boolean _newGallery;
    protected GalleryData _galleryData;
    protected PickupDragController _dragController;

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = GWT.create(GalleryService.class);
    protected static final ImageChooserServiceAsync _imgsvc = GWT.create(ImageChooserService.class);
}
