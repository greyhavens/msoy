//
// $Id$

package client.person;

import java.util.List;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.ClickBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 * Displays all of a member's galleries.
 *
 * @author mjensen
 */
public class GalleryPanel extends FlowPanel
{
    public static final String GALLERIES_ACTION = "galleries";

    public GalleryPanel (final int memberId)
    {
        addStyleName("Galleries");

        // TODO Add link to create new gallery

        _gallerysvc.loadGalleries(memberId, new MsoyCallback<List<Gallery>>(){
            public void onSuccess (List<Gallery> result) {
                for (int i = 0; i < result.size(); i++) {
                    Gallery gallery = result.get(i);
                    String args = Args.compose(GalleryViewPanel.VIEW_ACTION, ""+gallery.galleryId);
                    add(new ClickBox(gallery.thumbMedia, getGalleryLabel(gallery),
                        Pages.PEOPLE, args));
                }
            }
        });
    }

    /**
     * If the gallery is the "profile" gallery, this will return the correct profile label;
     * otherwise, this just returns the gallery name.
     */
    public static String getGalleryLabel (Gallery gallery) {
        return gallery.isProfileGallery() ?
            _pmsgs.profileGallery(CShell.creds.name.toString()) : gallery.name;
    }

    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
