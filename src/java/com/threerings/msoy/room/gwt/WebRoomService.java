//
// $Id$

package com.threerings.msoy.room.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingResult;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * Provides information related to the world.
 */
@RemoteServiceRelativePath(WebRoomService.REL_PATH)
public interface WebRoomService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/roomsvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + WebRoomService.ENTRY_POINT;

    /** Delivers the respose to {@link #loadGroupRooms}. */
    public static class RoomsResult implements IsSerializable
    {
        /**
         * The rooms of this group.
         */
        public List<RoomInfo> groupRooms;

        /**
         * The rooms owned by the caller.
         */
        public List<RoomInfo> callerRooms;
    }

    /** Delivers the respose to {@link #loadMemberRooms}. */
    public static class MemberRoomsResult
        implements IsSerializable
    {
        /** List of visible rooms for this member. */
        public List<RoomInfo> rooms;

        /** Name of the member whose rooms these are */
        public MemberName owner;

        /** The price for a new room. */
        public PriceQuote newRoomQuote;
    }

    /** Delivers the respose to {@link #loadThemeTemplates}. */
    public static class TemplatesResult implements IsSerializable
    {
        /**
         * The rooms of this group.
         */
        public List<RoomInfo> groupRooms;
    }

    /** Delivers the respose to {@link #loadOverview}. */
    public static class OverviewResult
        implements IsSerializable
    {
        /** A sample of the most populated rooms. */
        public List<RoomInfo> activeRooms;

        /** A sample of the newest-hottest rooms. */
        public List<RoomInfo> coolRooms;

        /** Recent winners of the Design Your Whirled contest, in order starting with 1st place */
        public List<RoomInfo> winningRooms;
    }

    /**
     * Loads information on a particular room.
     */
    RoomDetail loadRoomDetail (int sceneId)
        throws ServiceException;

    /**
     * Can the current user gift this particular room?
     */
    void canGiftRoom (int sceneId)
        throws ServiceException;

    /**
     * Loads the list of rooms owned by a given member, excluding ones locked to the calling user.
     */
    MemberRoomsResult loadMemberRooms (int memberId)
        throws ServiceException;

    /**
     * Returns a list of all the rooms owned by a specific group.
     */
    RoomsResult loadGroupRooms (int groupId)
        throws ServiceException;

    /**
     * Request to rate a room.
     */
    RatingResult rateRoom (int sceneId, byte rating)
        throws ServiceException;

    /**
     * Load content for the main rooms page.
     */
    OverviewResult loadOverview ()
        throws ServiceException;

    /**
     * Load content for the Design Your Whirled winners page.
     */
    List<RoomInfo> loadDesignWinners ()
        throws ServiceException;

    /**
     * Purchase a new room.
     */
    PurchaseResult<RoomInfo> purchaseRoom (Currency currency, int authedCost)
        throws ServiceException;

    /**
     * Associate or dissociate a room with a theme. Unlike items, which can be stamped by any
     * number of themes, rooms can either belong to one or not.
     */
    void stampRoom (int sceneId, int groupId, boolean doStamp)
        throws ServiceException;

    /**
     * Make (or unmake) the given scene a home room template for the given theme. The scene
     * must already be stamped appropriately.
     */
    public void makeTemplate (int sceneId, int groupId, boolean doMake)
        throws ServiceException;

    /**
     * Find and return the current template rooms of the given theme.
     */
    public TemplatesResult loadThemeTemplates (int groupId)
        throws ServiceException;
}
