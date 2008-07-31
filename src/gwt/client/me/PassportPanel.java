//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;

import com.threerings.msoy.badge.data.all.Badge;

import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class PassportPanel extends SimplePanel
{
    public PassportPanel ()
    {
        setStyleName("passportPanel");

        _mesvc.loadBadges(CMe.ident, new MsoyCallback<List<Badge>>() {
            public void onSuccess (List<Badge> badges) {
                init(badges);
            }
        });
    }

    protected void init (List<Badge> badges)
    {

    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
