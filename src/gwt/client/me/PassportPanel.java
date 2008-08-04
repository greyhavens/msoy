//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;

import com.threerings.msoy.badge.data.all.Badge;

import client.ui.Marquee;

import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class PassportPanel extends VerticalPanel
{
    public PassportPanel ()
    {
        setStyleName("passport");

        _mesvc.loadBadges(CMe.ident, new MsoyCallback<List<Badge>>() {
            public void onSuccess (List<Badge> badges) {
                init(badges);
            }
        });
    }

    protected void init (List<Badge> badges)
    {
        add(new NextPanel());
    }

    protected static class NextPanel extends HorizontalPanel
    {
        public NextPanel ()
        {
            setStyleName("NextPanel");
            add(new Marquee(null, _msgs.passportMarquee()));
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
