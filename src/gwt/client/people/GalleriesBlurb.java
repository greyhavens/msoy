//
// $Id: TrophiesBlurb.java 11654 2008-09-11 18:03:51Z mdb $

package client.people;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.profile.gwt.ProfileService;

import client.person.GalleryEditPanel;
import client.person.GalleryPanel;
import client.person.GalleryViewPanel;
import client.shell.Args;
import client.shell.Pages;
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
        return (CPeople.getMemberId() == pdata.name.getMemberId());
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.galleriesTitle());

        if (pdata.galleries == null || pdata.galleries.size() == 0) {
            setContent(new InlineLabel(CPeople.msgs.noGalleriesSelf(), false, false, false));

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
        if (CPeople.getMemberId() == _name.getMemberId()) {
            footerLinks.add(Link.create(CPeople.msgs.createGallery(), Pages.PEOPLE,
                GalleryEditPanel.CREATE_ACTION));
        }
        if (pdata.galleries != null && pdata.galleries.size() > NUM_GALLERIES) {
            footerLinks.add(WidgetUtil.makeShim(10, 10));
            footerLinks.add(Link.create(CPeople.msgs.seeAll(), Pages.PEOPLE, Args.compose(
                GalleryPanel.GALLERIES_ACTION, _name.getMemberId())));
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

            ClickListener clickListener = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.PEOPLE, Args.compose(GalleryViewPanel.VIEW_ACTION,
                        gallery.galleryId));
                }
            };
            add(new ThumbBox(gallery.thumbMedia, clickListener));
            add(MsoyUI.createActionLabel(gallery.name, clickListener));
        }
    }

    protected static final int NUM_GALLERIES = 6;
}
