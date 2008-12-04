//
// $Id$

package client.adminz;

import client.ui.TongueBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

public class BlingCashOutPanel extends FlowPanel
{
    public BlingCashOutPanel ()
    {
        add(new TongueBox(_msgs.requestedCashOutsTitle(), new CashOutTable()));
        add(new TongueBox(_msgs.charityCashOutsTitle(), new CharityCashOutTable()));
    }
    
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
}
