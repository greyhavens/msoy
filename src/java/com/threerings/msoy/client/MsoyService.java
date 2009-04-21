//
// $Id$

package com.threerings.msoy.client;

import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides global non-member-related services.
 */
public interface MsoyService extends InvocationService
{
    /**
     * Requests that any notifications that were deferred on the MemberObject be dispatched now
     */
    void dispatchDeferredNotifications (Client client);

    /**
     * Indicate whether we want to hear the group chat of the specified group.
     */
    void setHearingGroupChat (Client client, int groupId, boolean hear, ConfirmListener listener);

    /**
     * Shares a scene by emailing offsite friends.
     */
    void emailShare (Client client, boolean isGame, String placeName, int placeId,
                     String[] emails, String message, ConfirmListener listener);

    /**
     * Calculate the visitor's a/b test group (eg 1 or 2) or < 0 for no group.
     */
    void getABTestGroup (
        Client client, String testName, boolean logEvent, ResultListener listener);

    /**
     * Generic method for tracking a client-side action such as clicking a button.
     */
    void trackClientAction (Client client, String actionName, String details);

    /**
     * Tracking a client-side action such as clicking a button during an a/b test.  If testName
     * is supplied, the visitor's a/b test group will also be tracked.
     */
    void trackTestAction (Client client, String actionName, String testName);

    /**
     * Requests that any notifications that were deferred on the MemberObject be dispatched now
     */
    void trackVectorAssociation (Client client, String vector);

    /**
     * Requests a quote for sending a global broadcast. On success, the listener will receive an
     * a {@link PriceQuote} indicating the secured price of sending a broadcast.
     * @see com.threerings.msoy.chat.data.MsoyChatCodes#PAID_BROADCAST_MODE
     */
    void secureBroadcastQuote (Client client, ResultListener listener);

    /**
     * Sends a global user paid broadcast message for the previously quoted bar cost. Fails if the
     * user does not have sufficient funds or if the price has changed.
     */
    void purchaseAndSendBroadcast (Client client, int authedCost, String message,
        ResultListener listener);
}
