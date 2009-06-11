//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;

import client.util.events.FlashEvent;

/**
 * Provides a means by which the pages can interact with the frame.
 */
public interface Frame
{
    /** Codes for use with our inner frame to top frame RPC mechanism. */
    public static enum Calls {
        SET_TITLE, ADD_NAV_LINK, NAVIGATE_TO, NAVIGATE_REPLACE, CLOSE_CLIENT, CLOSE_CONTENT,
        DID_LOGON, LOGOFF, EMAIL_UPDATED, GET_WEB_CREDS, GET_PAGE_TOKEN, GET_MD5,
        CHECK_FLASH_VERSION, GET_ACTIVE_INVITE, GET_VISITOR_INFO, TEST_ACTION, GET_EMBEDDING,
        IS_HEADERLESS
    };

    /** Choice of embeddings the frame is in. */
    public enum Embedding { NONE, FACEBOOK }

    /**
     * Sets the title of the browser window and the page.
     */
    void setTitle (String title);

    /**
     * Adds an additional link to the title bar sub-navigation. When the history changes, the
     * sub-navigation is reset to its default, so a page should set up any custom sub-navigation
     * every time the history changes.
     */
    void addNavLink (String label, Pages page, Args args, int position);

    /**
     * Navigates to the page represented by the specified token.
     */
    void navigateTo (String token);

    /**
     * Replaces the current page with the page represented by the specified token, removing the
     * current page from the browser history in the process.
     */
    void navigateReplace (String token);

    /**
     * Clears any open client and restores the content display.
     */
    void closeClient ();

    /**
     * Clears the open content and restores the client to its full glory.
     */
    void closeContent ();

    /**
     * Displays the supplied dialog.
     */
    void showDialog (String title, Widget dialog);

    /**
     * Hides the current dialog contents.
     */
    void clearDialog ();

    /**
     * Dispatches a Flash event to all registered listeners.
     */
    void dispatchEvent (FlashEvent event);

    /**
     * This should be called by any entity that logs us on.
     */
    void dispatchDidLogon (SessionData data);

    /**
     * Logs off of our current session.
     */
    void logoff ();

    /**
     * Notifies the frame that our email address and validation status changed.
     */
    void emailUpdated (String address, boolean validated);

    /**
     * MD5 encodes the supplied text.
     */
    String md5hex (String text);

    /**
     * Checks that the installed version of Flash is kosher. Returns null if so, a HTML snipped to
     * embed otherwise.
     */
    String checkFlashVersion (int width, int height);

    /**
     * Returns the invitation via which the current user arrived or null.
     */
    Invitation getActiveInvitation ();

    /**
     * Returns our visitor identification. If we are in an anonmyous web session, this will be
     * created on demand the first time it is requested so that we avoid creating spurious visitor
     * info records when returning registered users show up at the landing page and immediately log
     * in, or log out and then close their browser.
     */
    VisitorInfo getVisitorInfo ();

    /**
     * Reports an A/B test action to the server for statistical grindizations.
     *
     * @param test the name of the A/B test with which this action is associated.
     * @param action a string identifying the action in question (be brief!).
     */
    void reportTestAction (String test, String action);

    /**
     * Gets the embedding of the frame.
     */
    Embedding getEmbedding ();

    /**
     * Returns true if we're headerless, false if we have (or should have) a header.
     */
    boolean isHeaderless ();
}
