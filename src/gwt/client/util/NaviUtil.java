//
// $Id$

package client.util;

import com.google.gwt.user.client.History;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import client.ui.MsoyUI;

/**
 * A place where we can encapsulate the creation of arguments that link to complex pages in
 * Whirled. This extends the encapsulation provided by SharedNaviUtil to functions that are only
 * required by the javascript client.
 */
public class NaviUtil extends SharedNaviUtil
{
    public static ClickHandler onCreateItem (byte type, byte ptype, int pitemId)
    {
        final String args = Args.compose("c", type, ptype, pitemId);
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (MsoyUI.requireRegistered()) {
                    Link.go(Pages.STUFF, args);
                }
            }
        };
    }

    public static ClickHandler onEditItem (byte type, int itemId)
    {
        return Link.createListener(Pages.STUFF, Args.compose("e", type, itemId));
    }

    public static ClickHandler onRemixItem (byte type, int itemId)
    {
        return Link.createListener(Pages.STUFF, Args.compose("r", type, itemId));
    }

    public static ClickHandler onViewTransactions (ReportType report)
    {
        return Link.createListener(Pages.ME, Args.compose("transactions", report.toIndex()));
    }

    public static ClickHandler onGoBack ()
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                History.back();
            }
        };
    }
}
