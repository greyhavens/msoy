//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;

/**
 * A utility class with link-related methods.
 */
public class Link
{
    /**
     * Returns link that displays the details of a given group.
     */
    public static Widget groupView (String label, int groupId)
    {
        return create(label, Pages.GROUPS, Args.compose("d", groupId));
    }

    /**
     * Returns link that displays the details of a given member.
     */
    public static Widget memberView (String label, int memberId)
    {
        return create(label, Pages.PEOPLE, ""+memberId);
    }

    /**
     * Returns link that displays the details of a given member.
     */
    public static Widget memberView (MemberName name)
    {
        return create(name.toString(), Pages.PEOPLE, ""+name.getMemberId());
    }

    /**
     * Returns link that displays the transaction history of a given member.
     */
    public static Widget transactionsView (String label, int memberId)
    {
        return create(label, Pages.ME, Args.compose("transactions", "1", ""+memberId));
    }

    /**
     * Get the billing URL, appending the username for easy logins if available.
     */
    public static String billingURL ()
    {
        String href = DeploymentConfig.billingURL;
        if (CShell.creds != null) {
            href += "?initUsername=" + CShell.creds.accountName;
        }
        return href;
    }

    /**
     * Returns link that pops up the billing page.
     */
    public static Anchor buyBars (String label)
    {
        return new Anchor(billingURL(), label, "_blank");
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget create (String label, Pages page, String args)
    {
        return create(label, null, page, args);
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget create (String label, String styleName, Pages page, String args)
    {
        Widget link = new ReroutedHyperlink(label, false, createToken(page, args));
        link.addStyleName("inline");
        if (styleName != null) {
            link.addStyleName(styleName);
        }
        return link;
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget createImage (String path, String tip, Pages page, String args)
    {
        return createHyperlink("<img border=0 src=\"" + path + "\">", tip, page, args);
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget createImage (AbstractImagePrototype image,
                                      String tip, Pages page, String args)
    {
        return createHyperlink(image.getHTML(), tip, page, args);
    }

    /**
     * Returns HTML that links to the specified page with the specified arguments. Don't use this
     * if you can avoid it. Hyperlink does special stuff to make the history mechanism work in some
     * browsers and this breaks that.
     */
    public static String createHtml (String label, Pages page, String args)
    {
        HTML escaper = new HTML();
        escaper.setText(label);
        return "<a href=\"/#" + createToken(page, args) + "\">" + escaper.getHTML() + "</a>";
    }

    /**
     * Returns a string that can be appended to '#' to link to the specified page with the
     * specified arguments.
     */
    public static String createToken (Pages page, String args)
    {
        String token = (page == null) ? "" : page.getPath();
        if (args != null && args.length() > 0) {
            token = token + "-" + args;
        }
        return token;
    }

    /**
     * Creates a click listener that navigates to the supplied page when activated.
     */
    public static ClickListener createListener (final Pages page, final String args)
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                go(page, args);
            }
        };
    }

    /**
     * Move to the page in question.
     */
    public static void go (Pages page, String args)
    {
        CShell.frame.navigateTo(createToken(page, args));
    }

    /**
     * Replace the current page with the one specified.
     */
    public static void replace (Pages page, String args)
    {
        CShell.frame.navigateReplace(createToken(page, args));
    }

    /**
     * Called when Flash wants us to display a page.
     */
    public static void goFromFlash (String page, String args)
    {
    	try {
            go(Enum.valueOf(Pages.class, page.toUpperCase()), args);
    	} catch (Exception e) {
            CShell.log("Unable to display page from Flash [page=" + page +
                       ", args=" + args + "].", e);
    	}
    }

    /**
     * A helper function for both {@link #getImageLink}s.
     */
    protected static Widget createHyperlink (String html, String tip, Pages page, String args)
    {
        Widget link = new ReroutedHyperlink(html, true, createToken(page, args));
        if (tip != null) {
            link.setTitle(tip);
        }
        link.addStyleName("inline");
        return link;
    }

    /**
     * A modified Hyperlink that routes its click through the application (so that it can be sent
     * to the top-level frame for processing) and which sets its path to the path of the top-level
     * frame, so that opening the link in a new tab or window will properly recreate the entire
     * framed environment rather than just opening the internal frame.
     *
     * We'd just extend Hyperlink and make a few small modifications, but we can't get at the
     * anchor element or the list of click listeners because they're both private. So instead we
     * have to reimplement everything. Yay!
     */
    protected static class ReroutedHyperlink extends Widget
        implements SourcesClickEvents
    {
        public ReroutedHyperlink (String text, boolean asHTML, String targetHistoryToken) {
            _targetHistoryToken = targetHistoryToken;

            setElement(DOM.createDiv());
            sinkEvents(Event.ONCLICK);
            setStyleName("gwt-Hyperlink");

            Element anchorElem = DOM.createAnchor();
            DOM.appendChild(getElement(), anchorElem);
            DOM.setElementProperty(anchorElem, "href", "/#" + _targetHistoryToken);

            if (asHTML) {
                DOM.setInnerHTML(anchorElem, text);
            } else {
                DOM.setInnerText(anchorElem, text);
            }
        }

        // from interface SourcesClickEvents
        public void addClickListener (ClickListener listener) {
            if (_clickListeners == null) {
                _clickListeners = new ClickListenerCollection();
            }
            _clickListeners.add(listener);
        }

        // from interface SourcesClickEvents
        public void removeClickListener (ClickListener listener) {
            if (_clickListeners != null) {
                _clickListeners.remove(listener);
            }
        }

        @Override // from Widget
        public void onBrowserEvent (Event event) {
            if (DOM.eventGetType(event) == Event.ONCLICK) {
                if (_clickListeners != null) {
                    _clickListeners.fireClick(this);
                }
                CShell.frame.navigateTo(_targetHistoryToken);
                DOM.eventPreventDefault(event);
            }
        }

        protected String _targetHistoryToken;
        protected ClickListenerCollection _clickListeners;
    }
}
