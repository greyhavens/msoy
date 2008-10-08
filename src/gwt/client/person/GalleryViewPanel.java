//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
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
    /** Show a gallery by gallery id */
    public static final String VIEW_ACTION = "gallery";
    /** Show the profile gallery for a member id */
    public static final String VIEW_PROFILE_ACTION = "pgallery";
    /** Show a specific photo in a gallery by gallery id and photo id */
    public static final String VIEW_PHOTO_ACTION = "galleryPhoto";

    /**
     * Constructor.
     * @param memberId If > 0, fetch the data for the member's profile gallery.
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
            setPhotoArg(photoId);
            return;
        }

        // fetch the gallery data
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData galleryData) {
                displayGallery(galleryData);
                setPhotoArg(photoId);
            }
        });
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
                    final String args = Args.compose(GalleryEditPanel.CREATE_PROFILE_ACTION,
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
                final String args = Args.compose(GalleryEditPanel.EDIT_ACTION,
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

        add(_galleryView = new FlowPanel());

        _galleryView.add(new GalleryDetailPanel(galleryData));

        // slieshow | view all galleries | edit actions
        InlinePanel actions = new InlinePanel("Actions");
        final String viewGalleriesArgs = Args.compose(GalleryPanel.GALLERIES_ACTION,
            _galleryData.owner.getMemberId());
        actions.add(MsoyUI.createActionLabel(_pmsgs.galleryViewAll(),
            Link.createListener(Pages.PEOPLE, viewGalleriesArgs)));
        if (galleryData.owner.getMemberId() == CShell.getMemberId()) {
            actions.add(new Label("|"));
            final String args = Args.compose(GalleryEditPanel.EDIT_ACTION,
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
                    Link.go(Pages.PEOPLE, Args.compose(GalleryPanel.GALLERIES_ACTION,
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
                    gotoPhotoIndex(photoIndex);
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

        // this will be filled with the full size image
        _currentPhoto = new AbsolutePanel();
        _currentPhoto.addStyleName("CurrentPhoto");
    }

    /**
     * Change which photo is being shown by photo itemId. If photoId is < 0, clear the current
     * photo. The photoId should be fetched from the page args; other calls to display a photo
     * should use gotoPhoto() instead.
     */
    protected void setPhotoArg (final int photoId)
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

        _currentPhoto.clear();
        _currentPhotoIndex = photoIndex;

        if (photoIndex < 0 || photoIndex >= _galleryData.photos.size()) {
            remove(_currentPhoto);
            add(_galleryView);
            return;
        }
        final Photo photo = _galleryData.photos.get(photoIndex);
        remove(_galleryView);
        add(_currentPhoto);

        // Photo and creator name are inline
        InlinePanel nameAndCreator = new InlinePanel("NameAndCreator");
        nameAndCreator.add(MsoyUI.createLabel(photo.name, "LargePhotoName"));
        nameAndCreator.add(new CreatorLabel(_galleryData.owner));
        _currentPhoto.add(nameAndCreator, 10, 0);

        // back to gallery overview button
        _currentPhoto.add(MsoyUI.createActionImage("/images/ui/back_arrow.png",
            _pmsgs.galleryBack(), new ClickListener() {
                public void onClick (Widget sender) {
                    stopSlideshow();
                    gotoPhotoIndex(-1);
                }
            }), 430, 35);

        // prev and next buttons
        ClickListener onPrev = new ClickListener() {
            public void onClick (Widget sender) {
                stopSlideshow();
                gotoPhotoIndex(photoIndex == 0 ? _galleryData.photos.size() - 1 : photoIndex - 1);
            }
        };
        ClickListener onNext = new ClickListener() {
            public void onClick (Widget sender) {
                stopSlideshow();
                gotoPhotoIndex((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        _currentPhoto.add(MsoyUI.createPrevNextButtons(onPrev, onNext), 455, 35);

        // slideshow button
        ClickListener slideshowClick = new ClickListener() {
            public void onClick (Widget sender) {
                Button button = (Button)sender;
                // toggle slideshow and text of this link
                if (button.getText().equals(_pmsgs.gallerySlideshowStart())) {
                    button.setText(_pmsgs.gallerySlideshowStop());
                    startSlideshow();
                } else {
                    button.setText(_pmsgs.gallerySlideshowStart());
                    stopSlideshow();
                }
            }
        };
        _currentPhoto.add(new Button(_slideshowTimer == null ? _pmsgs.gallerySlideshowStart()
            : _pmsgs.gallerySlideshowStop(), slideshowClick), 570, 35);

        // determine the width of the most constrained side and override the media constraint
        int width = photo.photoWidth;
        int height = photo.photoHeight;
        if (width > MAX_PHOTO_WIDTH) {
            width = MAX_PHOTO_WIDTH;
            height = Math.round((MAX_PHOTO_WIDTH / (float)photo.photoWidth) * photo.photoHeight);
        } else {
            width = 0;
        }
        if (height > MAX_PHOTO_HEIGHT) {
            height = MAX_PHOTO_HEIGHT;
            width = 0;
        } else {
            height = 0;
        }
        byte constraint = MediaDesc.NOT_CONSTRAINED;
        if (width > 0) {
            constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
        } else if (height > 0) {
            constraint = MediaDesc.VERTICALLY_CONSTRAINED;
        }
        photo.photoMedia.constraint = constraint;

        // clear the photo onclick
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                stopSlideshow();
                gotoPhotoIndex((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        Widget largePhoto = MediaUtil.createMediaView(photo.photoMedia, width, height, onClick);
        FlowPanel largePhotoContainer = MsoyUI.createFlowPanel("LargePhoto");
        largePhotoContainer.add(largePhoto);

        // if this image has been scaled down, link to the full size
        if (photo.photoMedia.constraint != MediaDesc.NOT_CONSTRAINED) {
            largePhotoContainer.add(MsoyUI.createActionLabel(_pmsgs.galleryFullSize(),
                "ViewFullSize", new ClickListener() {
                    public void onClick (Widget sender) {
                        // open the full image in a new window
                        Window.open(photo.photoMedia.getMediaPath(), "_blank", null);
                    }
                }));
        }
        _currentPhoto.add(largePhotoContainer, 0, 65);
    }

    /**
     * Moves the page to the appropriate photo based on it's location in the list of photos. Use
     * itemId instead of photo index in case the order of photos changes.
     */
    protected void gotoPhotoIndex (int photoIndex)
    {
        Photo photo = photoIndex >= 0 ? _galleryData.photos.get(photoIndex) : null;
        Link.go(Pages.PEOPLE, Args.compose(VIEW_PHOTO_ACTION, _galleryData.gallery.galleryId,
            photo == null ? 0 : photo.itemId));
    }

    /**
     * Start the slideshow immediately at the first photo or next photo if one is being shown.
     */
    protected void startSlideshow ()
    {
        _slideshowTimer = new Timer() {
            @Override public void run() {
                advanceSlideshow();
            }
        };
        _slideshowTimer.schedule(1);
    }

    /**
     * Display the next image in the gallery and schedule another change in 5 seconds.
     */
    protected void advanceSlideshow ()
    {
        if (_currentPhotoIndex < 0) {
            gotoPhotoIndex(0);
        } else {
            gotoPhotoIndex((_currentPhotoIndex + 1) % _galleryData.photos.size());
        }
        _slideshowTimer.schedule(5000);
    }

    /**
     * Halt the slideshow and return to the list of photos.
     */
    protected void stopSlideshow ()
    {
        if (_slideshowTimer != null) {
            _slideshowTimer.cancel();
            _slideshowTimer = null;
        }
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    /** List of photos and gallery details */
    protected GalleryData _galleryData;

    /** The index of the photo currently being displayed, used for slideshow */
    protected int _currentPhotoIndex;

    /** Fires every 5 seconds while slideshow is running */
    protected Timer _slideshowTimer;

    /** Gallery details and list of photos; hidden while viewing one photo in full */
    protected FlowPanel _galleryView;

    /** Panel to display the photo currently being displayed in full */
    protected AbsolutePanel _currentPhoto;

    /** If this is a profile gallery, whose profile is this */
    protected int _profileMemberId = 0;

    protected static int MAX_PHOTO_WIDTH = 600;
    protected static int MAX_PHOTO_HEIGHT = 400;
}
