//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.ClickBox;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryListData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 * Displays all of a member's galleries.
 *
 * @author mjensen
 */
public class GalleryPanel extends FlowPanel
{
    public GalleryPanel (final int memberId)
    {
        addStyleName("galleryPanel");

        _gallerysvc.loadGalleries(memberId, new MsoyCallback<GalleryListData>() {
            public void onSuccess (GalleryListData result) {
                displayGalleries(result);
            }
        });
    }

    /**
     * Print a list of galleries
     */
    protected void displayGalleries (GalleryListData data)
    {
        add(MsoyUI.createLabel(_pmsgs.galleryListTitle(data.owner.toString()), "Title"));

        FloatPanel galleries = new FloatPanel("GalleriesList");
        add(galleries);
        for (int i = 0; i < data.galleries.size(); i++) {
            Gallery gallery = data.galleries.get(i);
            String args = Args.compose(GalleryActions.VIEW, gallery.galleryId);
            Widget click = new ClickBox(gallery.thumbMedia, getGalleryLabel(gallery, data.owner),
                Pages.PEOPLE, args);
            galleries.add(click);
        }

        // add a create gallery button if player is looking at their own galleries
        if (CShell.getMemberId() == data.owner.getMemberId()) {
            PushButton create = MsoyUI.createButton(MsoyUI.LONG_THIN,
                _pmsgs.galleryCreate(), Link.createListener(Pages.PEOPLE, GalleryActions.CREATE));
            create.addStyleName("CreateButton");
            add(create);
        }
    }

    /**
     * If the gallery is the "profile" gallery, this will return the correct profile label;
     * otherwise, this just returns the gallery name.
     */
    public static String getGalleryLabel (Gallery gallery, MemberName owner) {
        return gallery.isProfileGallery() ?
            _pmsgs.galleryProfileName(owner.toString()) : gallery.name;
    }

    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
