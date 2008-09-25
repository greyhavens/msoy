//
// $Id$

package client.person;

import java.util.ArrayList;
import java.util.List;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.data.all.MediaDesc;
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
public class GalleryEditPanel extends AbsolutePanel // AbsolutePanel needed to support dnd
{
    public static final String EDIT_ACTION = "editgallery";

    public static final String CREATE_ACTION = "creategallery";
    public static final String CREATE_PROFILE_ACTION = "createprofilegallery";

    public GalleryEditPanel ()
    {
        // create a new gallery
        _newGallery = true;
        GalleryData galleryData = new GalleryData();
        galleryData.gallery = new Gallery();
        galleryData.gallery.name = _pmsgs.newGallery();
        galleryData.gallery.description = "";
        galleryData.photos = new ArrayList<Photo>();
        display(galleryData);
    }

    public GalleryEditPanel (int galleryId)
    {
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData result)
            {
                display(result);
            }
        });
    }

    protected void display (GalleryData galleryData)
    {

        _galleryData = galleryData;
        addStyleName("galleryEditPanel");

        // add editable gallery detail panel
        add(new GalleryDetailPanel(_galleryData, false), 0, 0);

        // add "save" button
        PushButton saveButton = MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.saveButton(),
            new ClickListener() {
                public void onClick (Widget sender) {
                    final PushButton button = (PushButton) sender;
                    button.setEnabled(false);
CShell.log("Photos: "+_galleryData.photos);
CShell.log("Saving: "+_galleryData.getPhotoIds());
                    if (_newGallery) {
                        _gallerysvc.createGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                            new MsoyCallback<Gallery>() {
                                public void onSuccess (Gallery result) {
                                    _newGallery = false;
                                    _galleryData.gallery = result;
                                    success(button);
                                }
                            }
                        );
                    } else {
                        _gallerysvc.updateGallery(_galleryData.gallery, _galleryData.getPhotoIds(),
                            new MsoyCallback<Void>() {
                                public void onSuccess (Void result) {
                                    success(button);
                                }
                            }
                        );
                    }
                }
                protected void success (PushButton button) {
                    button.setEnabled(true);
                    // send the user back to the gallery view on save
                    String args = Args.compose(GalleryViewPanel.VIEW_ACTION,
                        ""+_galleryData.gallery.galleryId);
                    Link.go(Pages.PEOPLE, args);
                }
            }
        );
        saveButton.addStyleName("Save");
        add(saveButton, 45, 350);

        // add drop panel for adding to and organizing this gallery
        _dragController = new PickupDragController(this, false);
        _dragController.setBehaviorDragProxy(false);
        // dragController.setBehaviorMultipleSelection(true);
        DropPanel<Photo> dropPanel = new DropPanel<Photo>(_dragController,
            new SimpleDropModel<Photo>(galleryData.photos)) {
            @Override protected Widget createWidget (Photo photo) {
                return MediaUtil.createMediaView(photo.thumbMedia, MediaDesc.THUMBNAIL_SIZE);
            }
        };
        add(dropPanel, 225, 10);

        // show photos that the member owns
        _itemsvc.loadPhotos(new MsoyCallback<List<Photo>>() {
            public void onSuccess (List<Photo> result) {
                add( new PagedPanel(new SimpleDataModel<Photo>(result), 5), 10, 415);
            }
        });
    }

    /**
     * Like a PageGrid, but more AJAXy.
     */
    protected class PagedPanel extends FlowPanel {
        public PagedPanel (DataModel<Photo> model, int count) {
            _model = model;
            _count = count;
            addStyleName("Contents");
            _prevNext = MsoyUI.createPrevNextButtons(new ClickListener() {
                public void onClick (Widget sender) {
                    prev();
                }
            },
            new ClickListener() {
                public void onClick (Widget sender) {
                    next();
                }
            });
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
                MediaUtil.createMediaView(photo.thumbMedia, MediaDesc.THUMBNAIL_SIZE), photo);
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
