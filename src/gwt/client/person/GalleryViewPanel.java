//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

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
public class GalleryViewPanel extends AbsolutePanel
{
    public static final String VIEW_ACTION = "gallery";
    public static final String VIEW_PROFILE_ACTION = "profileGallery";

    /**
     * Expects either galleryId if displaying a specific gallery, or a memberId if displaying that
     * member's profile gallery.
     * @param galleryId
     * @param memberId
     */
    public GalleryViewPanel (final int galleryId, final int memberId)
    {
        addStyleName("galleryViewPanel");

        if (galleryId > 0) {
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData galleryData) {
                displayGallery(galleryData);
            }
        });
        } else {
            _profileMemberId = memberId;
            _gallerysvc.loadMeGallery(memberId, new MsoyCallback<GalleryData>() {
                public void onSuccess (GalleryData galleryData) {
                    displayGallery(galleryData);
                }
            });
        }
    }

    protected void displayGallery (GalleryData galleryData)
    {
        if (galleryData == null) {
            if (_profileMemberId > 0) {
                if (_profileMemberId == CShell.getMemberId()) {
                    add(new HTML("You don't have any pictures in your profile gallery yet!"));
                    final String args = Args.compose(GalleryEditPanel.CREATE_PROFILE_ACTION,
                        _profileMemberId);
                    final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                    add(MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.editButton(), listener));
                } else {
                    add(new HTML("This person doesn't have any pictures in their gallery yet!"));
                }
            } else {
                add(new HTML("Could not find gallery."));
            }
            return;

        } else if (galleryData.photos == null || galleryData.photos.size() == 0) {
            if (galleryData.ownerId == CShell.getMemberId()) {
                add(new HTML("You don't have any pictures in this gallery yet!"));
                final String args = Args.compose(GalleryEditPanel.EDIT_ACTION,
                    galleryData.gallery.galleryId);
                final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                add(MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.editButton(), listener));
            } else {
                add(new HTML("There are no pictures in this gallery!"));
            }
            return;
        }
        _galleryData = galleryData;

        // add read-only gallery detail panel
        add(new GalleryDetailPanel(galleryData, true), 0, 0);

        FlowPanel photoPanel = new FlowPanel();
        for (int i = 0; i < galleryData.photos.size(); i++) {
            final int photoIndex = i;
            ClickListener thumbClickListener = new ClickListener() {
                public void onClick (Widget sender) {
                    setPhoto(photoIndex);
                }
            };
            photoPanel.add(MsoyUI.createSimplePanel("Thumbnail", MediaUtil.createMediaView(
                galleryData.photos.get(i).thumbMedia, MediaDesc.THUMBNAIL_SIZE,
                thumbClickListener)));
        }
        add(photoPanel, 225, 0);

        // this will be filled with the photos
        _currentPhoto = new AbsolutePanel();
        _currentPhoto.addStyleName("CurrentPhoto");
    }

    /**
     * Change which photo is being shown. If photo is null, just clear the current photo.
     */
    protected void setPhoto (final int photoIndex)
    {
        _currentPhoto.clear();

        if (photoIndex < 0 || photoIndex >= _galleryData.photos.size()) {
            remove(_currentPhoto);
            return;
        }

        // grey out the bg
        _currentPhoto.add(MsoyUI.createFlowPanel("GreyMask"), 0, 0);

        // determine the width of the most constrained side and override the media constraint
        Photo photo = _galleryData.photos.get(photoIndex);
        int width = photo.photoWidth;
        int height = photo.photoHeight;
        if (width > MAX_PHOTO_WIDTH) {
            width = MAX_PHOTO_WIDTH;
            height = (photo.photoWidth / MAX_PHOTO_WIDTH) * photo.photoHeight;
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
                setPhoto(-1);
            }
        };

        // generate the image media with at most one constrained side
        Widget fullSize = MediaUtil.createMediaView(photo.photoMedia, width, height, onClick);
        fullSize.addStyleName("FullSize");
        _currentPhoto.add(MsoyUI.createSimplePanel("FullSizeContainer", fullSize), 0, 0);

        // prev and next buttons
        ClickListener onPrev = new ClickListener() {
            public void onClick (Widget sender) {
                setPhoto(photoIndex == 0 ? _galleryData.photos.size() - 1 : photoIndex - 1);
            }
        };
        ClickListener onNext = new ClickListener() {
            public void onClick (Widget sender) {
                setPhoto((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        _currentPhoto.add(MsoyUI.createPrevNextButtons(onPrev, onNext), 300, 460);
        add(_currentPhoto, 0, 0);
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    protected GalleryData _galleryData;

    /** Panel to display the photo currently being displayed in full */
    protected AbsolutePanel _currentPhoto;

    /** If this is a profile gallery, whose profile is this */
    protected int _profileMemberId = 0;

    protected static int MAX_PHOTO_WIDTH = 400;
    protected static int MAX_PHOTO_HEIGHT = 400;
}
