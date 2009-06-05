//
// $Id$

package client.billing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.SmartTable;

import client.images.billing.BillingImages;
import client.ui.MsoyUI;

/**
 * Displays a billing pitch with a standard header.
 */
public class BillingPanel extends FlowPanel
{
    protected BillingPanel (String titleImg, String introText)
    {
        setStyleName("billing");

        SmartTable header = new SmartTable("Header", 0, 0);
        header.setWidget(0, 0, new Image("/images/billing/tofuonbars.png"));
        header.getFlexCellFormatter().setRowSpan(0, 0, 2);
        header.setWidget(0, 1, new Image(titleImg));
        header.setWidget(1, 0, MsoyUI.createHTML(introText, "Intro"));
        add(header);
    }

    protected static final BillingMessages _msgs = GWT.create(BillingMessages.class);
    protected static final BillingImages _images = GWT.create(BillingImages.class);
}
