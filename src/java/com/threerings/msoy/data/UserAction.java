//
// $Id$

package com.threerings.msoy.data;

import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.data.all.MemberName;

/**
 * Represent an action taken by a user; used to tag money earning and spending transactions.
 */
@ActionScript(omit=true)
public class UserAction extends SimpleStreamableObject
{
    /** Represents our various action types. */
    public enum Type {
        // NOTE: do not change these numbers unless you're also going to update Panopticon reports

        // general actions
        CREATED_PROFILE(1), UPDATED_PROFILE(2), CREATED_ACCOUNT(3),

        // friend related actions
        SENT_FRIEND_INVITE(10), ACCEPTED_FRIEND_INVITE(11), INVITED_FRIEND_JOINED(13),

        // game related actions
        PLAYED_GAME(20),

        // item and shop related actions
        CREATED_ITEM(30), BOUGHT_ITEM(31), LISTED_ITEM(32),

        // metagame related actions
        EARNED_BADGE(40), COMPLETED_SURVEY(41), VISITED_FB_APP(42),
        RATED_MUSIC(43), DJED_MUSIC(44),

        // (purely) money related actions
        BOUGHT_BARS(50), RECEIVED_PAYOUT(51), /*obsolete(52),*/ SUPPORT_ADJUST(53),
        EXCHANGED_CURRENCY(54), CASHED_OUT_BLING(55), RECEIVED_FRIEND_AWARD(56),
        SUBSCRIPTION_BARS(57),

        // buying shit from OOO
        BOUGHT_ROOM(100), BOUGHT_GROUP(101), BOUGHT_BROADCAST(102), BOUGHT_PARTY(103),
        BOUGHT_BARSCRIPTION(104), BOUGHT_THEME(105),

        UNUSED(255);

        /** Fetch the numerical representation of this type. */
        public int getNumber () {
            return _num;
        }

        /** Returns the message key that can be used to describe this action. */
        public String getMessage () {
            return _message;
        }

        Type (final int num) {
            this(num, "m.unknown");
        }

        Type (final int num, final String message) {
            _num = num;
            _message = message;
        }

        protected final int _num;
        protected final String _message;
    }

    /** The type of action taken. */
    public final Type type;

    /** The id of the member that took the action in question. */
    public final int memberId;

    /** A translatable string describing this action. Used by the money services. */
    public final String description;

    /** Additional machine readable data. Used by the humanity helper. */
    public final String data;

    /**
     * Look up an {@link UserAction} by its numerical representation and return it.
     */
    public static Type getActionByNumber (int num)
    {
        return _reverse.get(num);
    }

    public static UserAction createdProfile (int memberId)
    {
        return new UserAction(Type.CREATED_PROFILE, memberId, "m.created_profile");
    }

    public static UserAction updatedProfile (int memberId)
    {
        return new UserAction(Type.UPDATED_PROFILE, memberId, null);
    }

    public static UserAction createdAccount (int memberId)
    {
        return new UserAction(Type.CREATED_ACCOUNT, memberId, "m.created_account");
    }

    public static UserAction sentFriendInvite (int memberId)
    {
        return new UserAction(Type.SENT_FRIEND_INVITE, memberId, null);
    }

    public static UserAction acceptedFriendInvite (int memberId)
    {
        return new UserAction(Type.ACCEPTED_FRIEND_INVITE, memberId, null);
    }

    public static UserAction invitedFriendJoined (int inviterId, MemberName newcomer)
    {
        String descrip = MessageBundle.tcompose(
            "m.invited_friend_joined", newcomer, newcomer.getId());
        return new UserAction(Type.INVITED_FRIEND_JOINED, inviterId, descrip);
    }

    public static UserAction playedGame (int memberId, String gameName, int gameId, int seconds)
    {
        String descrip = MessageBundle.tcompose("m.played_game", gameName, gameId, seconds);
        return new UserAction(Type.PLAYED_GAME, memberId, descrip, gameId + " " + seconds);
    }

    public static UserAction createdItem (int memberId)
    {
        return new UserAction(Type.CREATED_ITEM, memberId, null);
    }

    public static UserAction boughtItem (int memberId)
    {
        return new UserAction(Type.BOUGHT_ITEM, memberId, null);
    }

    public static UserAction boughtFromOOO (int memberId, Type buyActionType, String desc)
    {
        return new UserAction(buyActionType, memberId, desc);
    }

    public static UserAction listedItem (int memberId)
    {
        return new UserAction(Type.LISTED_ITEM, memberId, null);
    }

    public static UserAction earnedBadge (int memberId, int badgeCode, int level)
    {
        String descrip = MessageBundle.compose("m.earned_badge",
            MessageBundle.tcompose("badge_" + Integer.toHexString(badgeCode),
                                   Badge.getLevelName(level)));
        return new UserAction(Type.EARNED_BADGE, memberId, descrip);
    }

    public static UserAction boughtBars (int memberId, String payment)
    {
        String descrip = MessageBundle.tcompose("m.bought_bars", payment);
        return new UserAction(Type.BOUGHT_BARS, memberId, descrip);
    }

    public static UserAction subscriptionBars (int memberId)
    {
        return new UserAction(Type.SUBSCRIPTION_BARS, memberId, "m.sub_bar_grant");
    }

    public static UserAction receivedPayout (int memberId)
    {
        return new UserAction(Type.RECEIVED_PAYOUT, memberId, null);
    }

    public static UserAction supportAdjust (int memberId, MemberName support)
    {
        String descrip = MessageBundle.tcompose("m.support_adjust", support, support.getId());
        return new UserAction(Type.SUPPORT_ADJUST, memberId, descrip, null);
    }

    public static UserAction exchangedCurrency (int memberId)
    {
        return new UserAction(Type.EXCHANGED_CURRENCY, memberId, null);
    }

    public static UserAction cashedOutBling (int memberId)
    {
        return new UserAction(Type.CASHED_OUT_BLING, memberId, null);
    }

    public static UserAction completedSurvey (int memberId, String surveyName, int surveyId)
    {
        String descrip = MessageBundle.tcompose("m.completed_survey", surveyName, surveyId);
        return new UserAction(Type.COMPLETED_SURVEY, memberId, descrip);
    }

    public static UserAction visitedFBApp (int memberId, String appName)
    {
        String descrip = MessageBundle.tcompose("m.visited_fb_app", appName);
        return new UserAction(Type.VISITED_FB_APP, memberId, descrip);
    }

    public static UserAction receivedFriendAward (int memberId, int friendId)
    {
        String descrip = MessageBundle.tcompose("m.award_for_friend", friendId);
        return new UserAction(Type.RECEIVED_FRIEND_AWARD, memberId, descrip);
    }

    public static UserAction ratedMusic (int memberId)
    {
        return new UserAction(Type.RATED_MUSIC, memberId, "m.rated_music");
    }

    public static UserAction djedMusic (int memberId)
    {
        return new UserAction(Type.DJED_MUSIC, memberId, "m.djed_music");
    }

    /** Used for unserialization. */
    public UserAction ()
    {
        this(Type.UNUSED, 0, null, null);
    }

    /**
     * Creates a configured user action.
     */
    protected UserAction (Type type, int memberId, String description)
    {
        this(type, memberId, description, null);
    }

    /**
     * Creates a configured user action.
     */
    protected UserAction (Type type, int memberId, String description, String data)
    {
        this.type = type;
        this.memberId = memberId;
        this.description = description;
        this.data = data;
    }

    protected static Map<Integer, Type> _reverse = Maps.newHashMap();
    static {
        for (Type type : Type.values()) {
            _reverse.put(type.getNumber(), type);
        }
    }
}
