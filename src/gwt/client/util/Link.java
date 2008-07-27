//
// $Id$

package client.util;

import client.shell.Args;
import client.shell.Page;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;

/**
 * A utility class with link-related methods.
 */
public class Link
{
    /**
     * Returns a {@link Hyperlink} that displays the details of a given group.
     */
    public static Hyperlink groupView (String label, int groupId)
    {
        return create(label, Page.WHIRLEDS, Args.compose("d", groupId));
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberView (String label, int memberId)
    {
        return create(label, Page.PEOPLE, ""+memberId);
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberView (MemberName name)
    {
        return create(name.toString(), Page.PEOPLE, ""+name.getMemberId());
    }

    /**
     * Returns a {@link Hyperlink} that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Hyperlink create (String label, String page, String args)
    {
        Hyperlink link = new Hyperlink(label, createToken(page, args));
        link.addStyleName("inline");
        return link;
    }

    /**
     * Returns a {@link Hyperlink} that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Hyperlink createImage (String path, String tip, String page, String args)
    {
        return createHyperlink("<img border=0 src=\"" + path + "\">", tip, page, args);
    }

    /**
     * Returns a {@link Hyperlink} that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Hyperlink createImage (AbstractImagePrototype image,
                                         String tip, String page, String args)
    {
        return createHyperlink(image.getHTML(), tip, page, args);
    }

    /**
     * Returns HTML that links to the specified page with the specified arguments. Don't use this
     * if you can avoid it. Hyperlink does special stuff to make the history mechanism work in some
     * browsers and this breaks that.
     */
    public static String createHtml (String label, String page, String args)
    {
        HTML escaper = new HTML();
        escaper.setText(label);
        return "<a href=\"#" + createToken(page, args) + "\">" + escaper.getHTML() + "</a>";
    }

    /**
     * Returns a string that can be appended to '#' to link to the specified page with the
     * specified arguments.
     */
    public static String createToken (String page, String args)
    {
        String token = page;
        if (args != null && args.length() > 0) {
            token = token + "-" + args;
        }
        return token;
    }

    /**
     * Creates a click listener that navigates to the supplied page when activated.
     */
    public static ClickListener createListener (final String page, final String args)
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
    public static void go (String page, String args)
    {
        String token = createToken(page, args);
        if (!token.equals(History.getToken())) { // TODO: necessary?
            History.newItem(token);
        }
    }

    /**
     * Replace the current page with the one specified.
     */
    public static void replace (String page, String args)
    {
        History.back();
        go(page, args);
    }

    /**
     * A helper function for both {@link #getImageLink}s.
     */
    protected static Hyperlink createHyperlink (String html, String tip, String page, String args)
    {
        Hyperlink link = new Hyperlink(html, true, createToken(page, args));
        if (tip != null) {
            link.setTitle(tip);
        }
        link.addStyleName("inline");
        return link;
    }
}
