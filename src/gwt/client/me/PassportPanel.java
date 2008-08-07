//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.person.gwt.PassportData;

import client.ui.HeaderBox;
import client.ui.Marquee;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class PassportPanel extends VerticalPanel
{
    public PassportPanel ()
    {
        setStyleName("passport");

        _mesvc.loadBadges(CMe.ident, new MsoyCallback<PassportData> () {
            public void onSuccess (PassportData data) {
                init(data);
            }
        });
    }

    protected void init (PassportData data)
    {
        add(new NextPanel(data));
        HeaderBox contents = new HeaderBox(null, _msgs.passportStampsTitle(data.stampOwner));
        contents.makeRoundBottom();
        add(contents);
   }

    protected static class NextPanel extends VerticalPanel
    {
        public NextPanel (PassportData data)
        {
            setStyleName("NextPanel");
            setSpacing(0);

            buildUI(data);
        }

        protected void buildUI (PassportData data)
        {
            HorizontalPanel header = new HorizontalPanel();
            header.add(MsoyUI.createImage("/images/me/passport_icon.png", "Icon"));
            header.add(MsoyUI.createLabel(_msgs.passportDescription(), "Description"));
            header.add(new Marquee(null, _msgs.passportMarquee()));
            add(header);

            HorizontalPanel nextHeader = new HorizontalPanel();
            nextHeader.setStyleName("NextHeader");
            nextHeader.add(MsoyUI.createImage("/images/me/passport_header_left.png", null));
            HorizontalPanel headerContent = new HorizontalPanel();
            headerContent.setStyleName("NextHeaderContent");
            headerContent.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            headerContent.add(MsoyUI.createImage("/images/me/passport_next.png", null));
            Label label = MsoyUI.createLabel(_msgs.passportNextBar(), "NextHeaderText");
            headerContent.add(label);
            headerContent.setCellWidth(label, "100%");
            headerContent.add(MsoyUI.createImage("/images/me/passport_shuffle.png", null));
            nextHeader.add(headerContent);
            nextHeader.setCellWidth(headerContent, "100%");
            nextHeader.add(MsoyUI.createImage("/images/me/passport_header_right.png", null));
            add(nextHeader);

            // TODO pull out the real "next" badges
            HorizontalPanel nextBadges = new HorizontalPanel();
            nextBadges.setStyleName("NextBadges");
            nextBadges.add(MsoyUI.createImage("/images/me/passport_box_left.png", null));
            HorizontalPanel nextBadgesPanel = new HorizontalPanel();
            nextBadgesPanel.setStyleName("NextBadgesPanel");
            addNextBadges(nextBadgesPanel, data);
            nextBadges.add(nextBadgesPanel);
            nextBadges.setCellWidth(nextBadgesPanel, "100%");
            nextBadges.add(MsoyUI.createImage("/images/me/passport_box_right.png", null));
            add(nextBadges);
        }

        protected void addNextBadges (Panel badgePanel, PassportData data)
        {
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
