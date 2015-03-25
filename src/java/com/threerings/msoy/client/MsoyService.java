//
// $Id$

package com.threerings.msoy.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * Provides global non-member-related services.
 */
public interface MsoyService extends InvocationService<ClientObject>
{
    /**
     * Requests that any notifications that were deferred on the MemberObject be dispatched now
     */
    void dispatchDeferredNotifications ();

    /**
     * Indicate whether we want to hear the group chat of the specified group.
     */
    void setHearingGroupChat (int groupId, boolean hear, ConfirmListener listener);

    /**
     * Shares a scene or parlor game by emailing offsite friends.
     * @param friend if set, the link that is sent will cookie the follower so that they send a
     * friend request upon registration
     */
    void emailShare (boolean isGame, String placeName, int placeId,
                     String[] emails, String message, boolean friend, ConfirmListener listener);

    /**
     * Calculate the visitor's a/b test group (eg 1 or 2) or < 0 for no group.
     */
    void getABTestGroup (
        String testName, boolean logEvent, ResultListener listener);

    /**
     * Reports that the client took an action in the specified test.
     */
    void trackTestAction (String test, String action);

    /**
     * Requests a quote for sending a global broadcast. On success, the listener will receive an
     * a {@link PriceQuote} indicating the secured price of sending a broadcast.
     * @see com.threerings.msoy.chat.data.MsoyChatCodes#PAID_BROADCAST_MODE
     */
    void secureBroadcastQuote (ResultListener listener);

    /**
     * Sends a global user paid broadcast message for the previously quoted bar cost. Fails if the
     * user does not have sufficient funds or if the price has changed.
     */
    void purchaseAndSendBroadcast (int authedCost, String message,
        ResultListener listener);
}
