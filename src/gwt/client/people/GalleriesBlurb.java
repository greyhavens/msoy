//
// $Id: TrophiesBlurb.java 11654 2008-09-11 18:03:51Z mdb $

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.person.GalleryActions;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays a member's recently earned trophies.
 */
public class GalleriesBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        // Display if there are galleries, or if viewing own profile
        if (pdata.galleries != null && pdata.galleries.size() > 0) {
            return true;
        }
        return (CShell.getMemberId() == pdata.name.getMemberId());
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.galleriesTitle());

        if (pdata.galleries == null || pdata.galleries.size() == 0) {
            setContent(new InlineLabel(_msgs.noGalleriesSelf(), false, false, false));

        } else {
            FlowPanel galleriesPanel = MsoyUI.createFlowPanel("Galleries");
            for (int i = 0; i < pdata.galleries.size() && i < NUM_GALLERIES; i++) {
                // don't display profile galleries here.
                if (pdata.galleries.get(i).name != null) {
                    galleriesPanel.add(new GalleryWidget(pdata.galleries.get(i)));
                }
            }
            setContent(galleriesPanel);
        }

        // links to create gallery and/or see all galleries
        FlowPanel footerLinks = new FlowPanel();
        if (CShell.getMemberId() == _name.getMemberId()) {
            footerLinks.add(Link.create(_msgs.createGallery(), Pages.PEOPLE, GalleryActions.CREATE));
        }
        if (pdata.galleries != null && pdata.galleries.size() > NUM_GALLERIES) {
            footerLinks.add(WidgetUtil.makeShim(10, 10));
            footerLinks.add(Link.create(_msgs.seeAll(), Pages.PEOPLE,
                Args.compose(GalleryActions.GALLERIES, _name.getMemberId())));
        }
        setFooter(footerLinks);
    }

    /**
     * Displays a single gallery thumbnail.
     */
    protected class GalleryWidget extends FlowPanel
    {
        public GalleryWidget (final Gallery gallery) {
            setStyleName("Gallery");
            String args = Args.compose(GalleryActions.VIEW, gallery.galleryId);
            add(new ThumbBox(gallery.thumbMedia, Pages.PEOPLE, args));
            add(Link.create(gallery.name, Pages.PEOPLE, args));
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);

    protected static final int NUM_GALLERIES = 6;
}
