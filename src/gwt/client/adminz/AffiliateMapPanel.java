//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.AffiliateMapping;

import client.ui.MsoyUI;

import client.util.ClickCallback;
import client.util.ServiceUtil;

public class AffiliateMapPanel extends PagedTable<AffiliateMapping>
{
    public AffiliateMapPanel ()
    {
        super(20);
// wtf, this isn't working
//        addStyleName("affiliateMapPanel");
        setModel(new AffiliateMappingDataModel(_adminSvc), 0);
    }

    @Override
    public String getEmptyMessage ()
    {
        return "<empty>"; // Fuck translating this, it'll never matter
    }

    @Override
    public List<Widget> createHeader ()
    {
        List<Widget> header = new ArrayList<Widget>();
        header.add(new Label(_msgs.affiliate()));
        header.add(new Label(_msgs.memberId()));
        header.add(new Label("")); // spacer for yon button
        return header;
    }

    @Override
    public List<Widget> createRow (final AffiliateMapping mapping)
    {
        List<Widget> row = new ArrayList<Widget>();
        row.add(new Label(mapping.affiliate));

        final TextBox box = MsoyUI.createTextBox(String.valueOf(mapping.memberId), 10, 10);
        row.add(box);

        Button save = new Button(_msgs.update());
        row.add(save);

        new ClickCallback<Void>(save) {
            public boolean callService () {
                int memberId;
                try {
                    memberId = Integer.valueOf(box.getText());
                } catch (NumberFormatException nfe) {
                    MsoyUI.errorNear(_msgs.badMemberId(box.getText()), box);
                    return false;
                }
                _adminSvc.mapAffiliate(mapping.affiliate, memberId, this);
                return true;
            }

            public boolean gotResult (Void result) {
                return true;
            }
        };

        return row;
    }

    protected AdminMessages _msgs = GWT.create(AdminMessages.class);

    protected AdminServiceAsync _adminSvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
