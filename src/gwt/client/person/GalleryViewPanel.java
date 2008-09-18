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
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 * A read-only view of the photos in a gallery.
 *
 * @author mjensen
 */
public class GalleryViewPanel extends AbsolutePanel
{
    public static final String VIEW_ACTION = "gallery";

    public GalleryViewPanel (final int galleryId)
    {
        addStyleName("Gallery");
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData galleryData)
            {
                // add read-only gallery detail panel
                add(new GalleryDetailPanel(galleryData.gallery, true), 0, 0);

                // if the current member owns this gallery, add an edit button
                if (galleryData.ownerId == CShell.getMemberId()) {
                    final String args = Args.compose(GalleryEditPanel.EDIT_ACTION, galleryId);
                    final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                    add(MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.editButton(), listener),
                        25, 400);
                }

                // TODO Make this the best gallery view ever. This is just a placeholder for now.
                FlowPanel photoPanel = new FlowPanel();
                for (Photo photo : galleryData.photos) {
                    photoPanel.add(MediaUtil.createMediaView(photo.thumbMedia,
                            MediaDesc.THUMBNAIL_SIZE));
                }
                add(photoPanel, 225, 0 );
            }
        });
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);
}
