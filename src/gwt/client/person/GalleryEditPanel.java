// $Id$

package client.person;

import java.util.ArrayList;
import java.util.List;

import client.dnd.DropListener;
import client.dnd.DropModel;
import client.dnd.DropPanel;
import client.dnd.PayloadWidget;
import client.dnd.SimpleDropModel;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.allen_sauer.gwt.dnd.client.drop.SimpleDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.CenteredBox;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 * Allows a user to edit one of their galleries.
 *
 * @author mjensen
 */
public class GalleryEditPanel extends AbsolutePanel // AbsolutePanel needed to support drag-n-drop
{
    public static final String EDIT_ACTION = "editgallery";
    public static final String CREATE_ACTION = "creategallery";
    public static final String CREATE_PROFILE_ACTION = "createprofilegallery";

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
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
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

        // add "save" button
        PushButton saveButton = MsoyUI.createButton(MsoyUI.MEDIUM_THIN,
            _pmsgs.gallerySaveButton(),
            new ClickListener() {
                public void onClick (Widget sender) {
                    final PushButton button = (PushButton) sender;
                    button.setEnabled(false);
                    saveGallery(true, button);
                }
            }
        );
        saveButton.addStyleName("Save");
        add(saveButton, 575, 70);

        // instructions for drop box will be removed when the first content is inserted
        final FlowPanel dragInstructions = MsoyUI.createFlowPanel("DragInstructions");

        // add drop panel for adding to and organizing this gallery
        SimpleDropModel<Photo> dropModel = new SimpleDropModel<Photo>(galleryData.photos);
        dropModel.addDropListener(new DropListener<Photo>() {
            public void contentInserted (DropModel<Photo> model, Photo content, int index) {
                dragInstructions.clear();
                updateCount(model);
            }
            public void contentRemoved (DropModel<Photo> model, Photo content) {
                updateCount(model);
            }
            protected void updateCount (DropModel<Photo> model) {
                detailPanel.setCount(model.getContents().size());
            }
        });
        DropPanel<Photo> dropPanel = new DropPanel<Photo>(_dragController, dropModel) {
            @Override protected Widget createWidget (Photo photo) {
                return new CenteredBox(MediaUtil.createMediaView(photo.thumbMedia,
                    MediaDesc.THUMBNAIL_SIZE), "ThumbnailImage");
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
        _itemsvc.loadPhotos(new MsoyCallback<List<Photo>>() {
            public void onSuccess (List<Photo> result) {
                final PhotoList myPhotos = new PhotoList(new SimpleDataModel<Photo>(result), 12);
                photoContainer.setWidget(myPhotos);
                // allow photos to get dropped onto this panel (to remove them from a gallery)
                _dragController.registerDropController(
                    new SimpleDropController(GalleryEditPanel.this) {
                        @Override public void onPreviewDrop(DragContext context)
                        throws VetoDragException {
                            super.onPreviewDrop(context);
                            if (context.draggable instanceof PayloadWidget) {
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
        });

        // "your photos" title goes over photo list
        add(new Image("/images/people/gallery_photo_icon.png"), 10, 375);
        add(MsoyUI.createLabel(_pmsgs.galleryPhotoListTitle(), "PhotoListTitle"), 65, 385);

        // button link to photo upload panel goes over photo list
        add(MsoyUI.createActionLabel(_pmsgs.galleryUploadPhotos(), "UploadPhotos",
            new ClickListener() {
                public void onClick (Widget sender) {
                    // save the gallery before leaving this page
                    saveGallery(false, null);
                    Link.go(Pages.STUFF, Args.compose("c", "" + Item.PHOTO));
                }
            }), 210, 395);
    }

    /**
     * Create a new gallery or update an existing one.
     * @param backToView if true, player will be returned to gallery view on success.
     * @param saveButton if supplied, this will be enabled on success.
     */
    protected void saveGallery (final boolean backToView, final PushButton saveButton)
    {


        if (_newGallery) {
            _gallerysvc.createGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                new MsoyCallback<Gallery>() {
                    public void onSuccess (Gallery result) {
                        _newGallery = false;
                        _galleryData.gallery = result;
                        if (saveButton != null) {
                            saveButton.setEnabled(true);
                        }
                        if (backToView) {
                            Link.go(Pages.PEOPLE, Args.compose(GalleryViewPanel.VIEW_ACTION,
                                _galleryData.gallery.galleryId));
                        }
                    }
                }
            );
        } else {
            _gallerysvc.updateGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        if (saveButton != null) {
                            saveButton.setEnabled(true);
                        }
                        if (backToView) {
                            Link.go(Pages.PEOPLE, Args.compose(GalleryViewPanel.VIEW_ACTION,
                                _galleryData.gallery.galleryId));
                        }
                    }
                }
            );
        }
    }

    /**
     * A paginated list of Photos.
     * TODO add page numbers.
     */
    protected class PhotoList extends FlowPanel {
        public PhotoList (DataModel<Photo> model, int count) {
            _model = model;
            _count = count;
            addStyleName("PhotoList");
            _prevNext = MsoyUI.createPrevNextButtons(
                new ClickListener() {
                    public void onClick (Widget sender) {
                        prev();
                    }
                },
                new ClickListener() {
                    public void onClick (Widget sender) {
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
            _model.doFetchRows(_page * _count, _count, new MsoyCallback<List<Photo>>() {
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
                    photo.thumbMedia, MediaDesc.THUMBNAIL_SIZE), "ThumbnailImage"), photo);
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
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
