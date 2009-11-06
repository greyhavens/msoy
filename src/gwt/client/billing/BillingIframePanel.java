//
// $Id$

package client.billing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;

import client.images.billing.BillingImages;
import client.util.BillingUtil;

/**
 * Displays a billing pitch with a standard header.
 */
public class BillingIframePanel extends FlowPanel
{
    /**
     * @param path Relative page to a page on billing eg "paypal/choosecoins.jspx"
     */
    public BillingIframePanel (String startPage)
    {
        setStyleName("billingIframe");

        _iframe = new Frame(BillingUtil.getAbsoluteBillingURL(startPage));

        // may scroll internally but should fill the height of the window
        _iframe.setWidth("680px");
        setIframeHeight();
        Window.addResizeHandler(new ResizeHandler() {
            public void onResize (ResizeEvent event) {
                setIframeHeight();
            }
        });

        // IE defaults to using a sunken border on iframes, clear it
        _iframe.getElement().setAttribute("frameBorder", "0");

        add(_iframe);
    }

    @Override
    protected void onAttach ()
    {
        super.onAttach();
        setIframeHeight();
    }

    protected void setIframeHeight ()
    {
        Widget parent = this.getParent();
        final int newHeight;
        if (parent != null && parent.getElement().getClientHeight() > BOTTOM_GUTTER) {
            newHeight = parent.getElement().getClientHeight() - BOTTOM_GUTTER;
        } else {
            newHeight = 500;
        }
        _iframe.setHeight(newHeight + "px");
    }

    protected Frame _iframe;
    protected static final int BOTTOM_GUTTER = 6;

    protected static final BillingMessages _msgs = GWT.create(BillingMessages.class);
    protected static final BillingImages _images = GWT.create(BillingImages.class);
}
