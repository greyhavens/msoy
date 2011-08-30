//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import client.ui.TongueBox;

public class BlingCashOutPanel extends FlowPanel
{
    public BlingCashOutPanel ()
    {
        add(new TongueBox(_msgs.requestedCashOutsTitle(), new CashOutTable()));
        add(new TongueBox(_msgs.charityCashOutsTitle(), new CharityCashOutTable()));
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
}
