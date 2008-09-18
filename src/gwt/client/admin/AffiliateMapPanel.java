//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.admin.gwt.AffiliateMapping;

import client.ui.MsoyUI;

public class AffiliateMapPanel extends PagedGrid<AffiliateMapping>
{
    public AffiliateMapPanel ()
    {
        super(20, 1);

        setModel(new AffiliateMappingDataModel(), 0);
    }

    @Override
    public Widget createWidget (AffiliateMapping mapping)
    {
        return new MappingWidget(mapping);
    }

    @Override
    public String getEmptyMessage ()
    {
        return "<empty>"; // TODO: translate? (or: fuck it, admin page)
    }

    protected class MappingWidget extends HorizontalPanel
    {
        public MappingWidget (AffiliateMapping mapping)
        {
            // TODO...

            add(new Label(mapping.affiliate));
            add(new Label(String.valueOf(mapping.memberId)));
//            add(MsoyUI.createLabel(mapping.affiliate), "todoStyle");
//            add(MsoyUI.createLabel(String.valueOf(mapping.memberId)), "todoStyle");
        }
    }
}
