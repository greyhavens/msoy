//
// $Id$

package client.util;

import com.google.gwt.user.client.History;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.msoy.money.data.all.ReportType;
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
    public static ClickHandler onCreateItem (final byte type, final int suiteId)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (MsoyUI.requireRegistered()) {
                    Link.go(Pages.STUFF, "c", type, suiteId);
                }
            }
        };
    }

    public static ClickHandler onEditItem (byte type, int itemId)
    {
        return Link.createHandler(Pages.STUFF, "e", type, itemId);
    }

    public static ClickHandler onRemixItem (byte type, int itemId)
    {
        return Link.createHandler(Pages.STUFF, "r", type, itemId);
    }

    public static ClickHandler onViewTransactions (ReportType report)
    {
        return Link.createHandler(Pages.ME, "transactions", report.toIndex());
    }

    public static ClickHandler onSignUp ()
    {
        return Link.createHandler(Pages.LANDING, "");
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
