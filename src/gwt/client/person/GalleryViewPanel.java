//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.CenteredBox;
import com.threerings.gwt.ui.InlinePanel;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 * A read-only view of the photos in a gallery. Also handles profile galleries which are special
 * in that there is only one per member (although it may or may not exist), and may be fetched
 * using the memberId instead of the galleryId.
 *
 * @author mjensen
 */
public class GalleryViewPanel extends FlowPanel
{
    /**
     * Constructor.
     */
    public GalleryViewPanel ()
    {
        addStyleName("galleryViewPanel");
    }

    /**
     * Will fetch gallery data only if we don't already have it.
     * @param args Page parameters, may include memberId, galleryId and/or photoId
     * @param isProfileGallery If true, this is a special case gallery selected by memberId.
     */
    public void setArgs (Args args, boolean isProfileGallery)
    {
        if (isProfileGallery) {
            // fetch the profile gallery info
            _profileMemberId = args.get(1, 0);
            _gallerysvc.loadMeGallery(_profileMemberId, new MsoyCallback<GalleryData>() {
                public void onSuccess (GalleryData galleryData) {
                    displayGallery(galleryData);
                }
            });
            return;
        }

        final int galleryId = args.get(1, 0);
        final int photoId = args.get(2, 0);

        if (_galleryData != null && _galleryData.gallery.galleryId == galleryId) {
            // don't refetch gallery data, just display the right image
            setPhoto(photoId);
            return;
        }

        // fetch the gallery data
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData galleryData) {
                displayGallery(galleryData);
                setPhoto(photoId);
            }
        });
    }

    /**
     * Change which photo is being shown by photo itemId. If photoId is 0, clear the current photo
     * instead and display the list of all photos.
     */
    protected void setPhoto (final int photoId)
    {
        // get the location in the photo in our set, if it exists
        int tempPhotoIndex = -1;
        if (photoId != 0) {
            for (Photo photo : _galleryData.photos) {
                if (photo.itemId == photoId) {
                    tempPhotoIndex = _galleryData.photos.indexOf(photo);
                    break;
                }
            }
        }
        final int photoIndex = tempPhotoIndex;
        if (photoIndex == -1) {
            remove(_currentPhoto);
            add(_galleryView);
        } else {
            remove(_galleryView);
            add(_currentPhoto);
            _currentPhoto.setPhoto(photoIndex);
        }
    }

    /**
     * If galleryData contains a list of images, display the gallery details and thumbnails for
     * those images, otherwise display an error and/or edit button.
     */
    protected void displayGallery (GalleryData galleryData)
    {
        FlowPanel error = MsoyUI.createFlowPanel("Error");
        if (galleryData == null) {
            if (_profileMemberId != 0) {
                if (_profileMemberId == CShell.getMemberId()) {
                    error.add(new HTML(_pmsgs.galleryProfileNoPhotosSelf()));
                    final String args = Args.compose(GalleryActions.CREATE_PROFILE,
                        _profileMemberId);
                    final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                    error.add(MsoyUI.createActionLabel(_pmsgs.galleryAddPhotos(), listener));
                } else {
                    error.add(new HTML(_pmsgs.galleryNoPhotosOther()));
                }
            } else {
                error.add(new HTML(_pmsgs.galleryNotFound()));
            }

        } else if (galleryData.photos == null || galleryData.photos.size() == 0) {
            if (galleryData.owner.getMemberId() == CShell.getMemberId()) {
                error.add(new HTML(_pmsgs.galleryNoPhotosSelf()));
                final String args = Args.compose(GalleryActions.EDIT,
                    galleryData.gallery.galleryId);
                final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                error.add(MsoyUI.createActionLabel(_pmsgs.galleryAddPhotos(), listener));
            } else {
                error.add(new HTML(_pmsgs.galleryNoPhotosOther()));
            }
        }
        // if there is an error, display it and stop rendering page
        if (error.getWidgetCount() > 0) {
            add(error);
            return;
        }
        _galleryData = galleryData;

        // this will be filled with the full size image
        _currentPhoto = new GalleryPhotoPanel(galleryData);

        add(_galleryView = new FlowPanel());

        _galleryView.add(new GalleryDetailPanel(galleryData));

        // slieshow | view all galleries | edit | delete actions
        InlinePanel actions = new InlinePanel("Actions");
        actions.add(MsoyUI.createActionLabel(_pmsgs.gallerySlideshowStart(),
                new ClickListener() {
                    public void onClick (Widget sender) {
                        _currentPhoto.startSlideshow();
                    }
            }));
        actions.add(new Label("|"));

        final String viewGalleriesArgs = Args.compose(GalleryActions.GALLERIES,
            _galleryData.owner.getMemberId());
        actions.add(MsoyUI.createActionLabel(_pmsgs.galleryViewAll(),
            Link.createListener(Pages.PEOPLE, viewGalleriesArgs)));
        if (galleryData.owner.getMemberId() == CShell.getMemberId()) {
            actions.add(new Label("|"));
            final String args = Args.compose(GalleryActions.EDIT,
                _galleryData.gallery.galleryId);
            final ClickListener editListener = Link.createListener(Pages.PEOPLE, args);
            actions.add(MsoyUI.createActionLabel(_pmsgs.galleryEditButton(), editListener));
            actions.add(new Label("|"));

            Label delete = MsoyUI.createActionLabel(_pmsgs.galleryDeleteButton(), null);
            delete.addStyleName("actionLabel");
            new ClickCallback<Void>(delete, _pmsgs.galleryConfirmDelete()) {
                public boolean callService () {
                    _gallerysvc.deleteGallery(_galleryData.gallery.galleryId, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(_pmsgs.galleryDeleted());
                    Link.go(Pages.PEOPLE, Args.compose(GalleryActions.GALLERIES,
                        CShell.getMemberId()));
                    return false;
                }
            };
            actions.add(delete);
        }
        _galleryView.add(actions);

        // list all the photos
        FlowPanel photoList = new FlowPanel();
        for (int ii = 0; ii < galleryData.photos.size(); ii++) {
            final int photoIndex = ii;
            Photo photo = galleryData.photos.get(ii);

            // clicking on an image will bring up the full view of it
            ClickListener thumbClickListener = new ClickListener() {
                public void onClick (Widget sender) {
                    _currentPhoto.gotoPhotoIndex(photoIndex);
                }
            };

            // add thumbnail and image name to a box
            Widget image = MediaUtil.createMediaView(
                photo.getPreviewMedia(), MediaDesc.PREVIEW_SIZE, thumbClickListener);
            FlowPanel thumbnail = MsoyUI.createFlowPanel("Thumbnail");
            // size the containing box here, include space for 1px border
            thumbnail.add(new CenteredBox(image, "Image",
                                          MediaDesc.getWidth(MediaDesc.PREVIEW_SIZE) + 2,
                                          MediaDesc.getHeight(MediaDesc.PREVIEW_SIZE) + 2));
            thumbnail.add(MsoyUI.createLabel(photo.name, "Name"));
            photoList.add(thumbnail);
        }
        _galleryView.add(photoList);
    }

    @Override // from Widget
    protected void onUnload ()
    {
        super.onUnload();

        // stop any running slideshow when we're removed from the DOM
        if (_currentPhoto != null) {
            _currentPhoto.stopSlideshow();
        }
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    /** List of photos and gallery details */
    protected GalleryData _galleryData;

    /** Gallery details and list of photos; hidden while viewing one photo in full */
    protected FlowPanel _galleryView;

    /** Panel to display the photo currently being displayed in full */
    protected GalleryPhotoPanel _currentPhoto;

    /** If this is a profile gallery, whose profile is this */
    protected int _profileMemberId = 0;
}
