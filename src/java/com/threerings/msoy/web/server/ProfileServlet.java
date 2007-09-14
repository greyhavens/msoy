//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GameRating;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.ProfileLayout;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.parlor.rating.server.RatingManagerDelegate;
import com.threerings.parlor.rating.server.persist.RatingRecord;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        // TODO: whatever filtering and profanity checking that we want

        try {
            // load their old profile record for "first time configuration" purposes
            ProfileRecord oprof = MsoyServer.profileRepo.loadProfile(memrec.memberId);

            // stuff their updated profile data into the database
            ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
            if (oprof != null) {
                nrec.modifications = oprof.modifications+1;
                nrec.birthday = oprof.birthday;
                nrec.realName = oprof.realName;
            } else {
                log.warning("Account missing old profile [id=" + memrec.memberId + "].");
            }
            MsoyServer.profileRepo.storeProfile(nrec);

            // record that the user updated their profile
            logUserAction(memrec, (nrec.modifications == 1) ?
                          UserAction.CREATED_PROFILE : UserAction.UPDATED_PROFILE, null);

            // handle a display name change if necessary
            if (memrec.name == null || !memrec.name.equals(displayName)) {
                MsoyServer.memberRepo.configureDisplayName(memrec.memberId, displayName);

                // let the member manager know about the display name change
                final MemberName name = new MemberName(displayName, memrec.memberId);
                MsoyServer.omgr.postRunnable(new Runnable() {
                    public void run () {
                        MsoyServer.memberMan.displayNameChanged(name);
                    }
                });
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's profile " +
                    "[who=" + memrec.who() + "].", pe);
        }
    }

    // from interface ProfileService
    public ProfileResult loadProfile (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            ProfileResult result = new ProfileResult();
            result.name = tgtrec.getName();
            result.layout = loadLayout(tgtrec);

            // resolve the data for whichever blurbs are active on this player's profile page
            for (Object bdata : result.layout.blurbs) {
                BlurbData blurb = (BlurbData)bdata;
                switch (blurb.type) {
                case BlurbData.PROFILE:
                    result.profile = resolveProfileData(memrec, tgtrec);
                    break;

                case BlurbData.FRIENDS:
                    result.friends = resolveFriendsData(memrec, tgtrec);
                    break;

                case BlurbData.GROUPS:
                    result.groups = resolveGroupsData(memrec, tgtrec);
                    break;

//                 case BlurbData.HOOD:
//                     result.hood = resolveHoodData(memrec, tgtrec);
//                     break;

                case BlurbData.RATINGS:
                    result.ratings = resolveRatingsData(memrec, tgtrec);
                    break;

                default:
                    log.log(Level.WARNING, "Requested to resolve unknown blurb " + bdata + ".");
                    break;
                }
            }
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public List<MemberCard> findProfiles (String type, String search)
        throws ServiceException
    {
        try {
            // locate the members that match the supplied search
            HashIntMap<MemberCard> cards = new HashIntMap<MemberCard>();
            if ("email".equals(type)) {
                MemberRecord memrec = MsoyServer.memberRepo.loadMember(search);
                if (memrec != null) {
                    MemberCard card = new MemberCard();
                    card.name = new MemberName(memrec.name, memrec.memberId);
                    cards.put(memrec.memberId, card);
                }

            } else {
                List<MemberNameRecord> names = null;
                if ("display".equals(type)) {
                    names = MsoyServer.memberRepo.findMemberNames(search, MAX_PROFILE_MATCHES);
                } else if ("name".equals(type)) {
                    names = MsoyServer.profileRepo.findMemberNames(search, MAX_PROFILE_MATCHES);
                }
                if (names != null) {
                    for (MemberNameRecord mname : names) {
                        MemberCard card = new MemberCard();
                        card.name = mname.toMemberName();
                        cards.put(mname.memberId, card);
                    }
                }
            }

            // load up their profile data
            resolveCardData(cards);

            ArrayList<MemberCard> results = new ArrayList<MemberCard>();
            results.addAll(cards.values());
            return results;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure finding profiles [search=" + search + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    protected ProfileLayout loadLayout (MemberRecord memrec)
        throws PersistenceException
    {
        // TODO: store this metadata in a database, allow it to be modified
        ProfileLayout layout = new ProfileLayout();
        layout.layout = ProfileLayout.TWO_COLUMN_LAYOUT;

        ArrayList<BlurbData> blurbs = new ArrayList<BlurbData>();
        BlurbData blurb = new BlurbData();
        blurb.type = BlurbData.PROFILE;
        blurb.blurbId = 0;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.FRIENDS;
        blurb.blurbId = 1;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.GROUPS;
        blurb.blurbId = 2;
        blurbs.add(blurb);

//         blurb = new BlurbData();
//         blurb.type = BlurbData.HOOD;
//         blurb.blurbId = 3;
//         blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.RATINGS;
        blurb.blurbId = 4;
        blurbs.add(blurb);

        layout.blurbs = blurbs;
        return layout;
    }

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        ProfileRecord prec = MsoyServer.profileRepo.loadProfile(tgtrec.memberId);
        Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec.permaName);

        // TODO: if they're online right now, show that
        profile.lastLogon = (tgtrec.lastSession != null) ? tgtrec.lastSession.getTime() : 0L;

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        HashIntMap<MemberCard> cards = new HashIntMap<MemberCard>();
        for (FriendEntry entry : MsoyServer.memberRepo.loadFriends(tgtrec.memberId)) {
            MemberCard card = new MemberCard();
            card.name = entry.name;
            cards.put(entry.name.getMemberId(), card);
        }
        resolveCardData(cards);

        ArrayList<MemberCard> results = new ArrayList<MemberCard>();
        results.addAll(cards.values());
        return results;
    }

    protected List<GroupMembership> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        MemberName name = tgtrec.getName();
        List<GroupMembership> result = new ArrayList<GroupMembership>();
        for (GroupMembershipRecord gmRec : MsoyServer.groupRepo.getMemberships(tgtrec.memberId)) {
            GroupRecord gRec = MsoyServer.groupRepo.loadGroup(gmRec.groupId);
            if (gRec == null) {
                log.warning("Unknown group membership [memberId=" + tgtrec.memberId +
                            ", groupId=" + gmRec.groupId + "]");
                continue;
            }
            result.add(gmRec.toGroupMembership(gRec, name));
        }
        return result;
    }

// TODO: do we really want the hood on the profile page?
//     protected String resolveHoodData (MemberRecord reqrec, MemberRecord tgtrec)
//         throws PersistenceException
//     {
//         MsoyServer.memberMan.serializeNeighborhood(_memberId, false, new ResultListener<String>() {
//             public void requestCompleted (String hood) {
//                 resolutionCompleted(hood);
//             }
//             public void requestFailed (Exception cause) {
//                 resolutionFailed(cause);
//             }
//         });
//         return null;
//     }

    protected List<GameRating> resolveRatingsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        // fetch all the rating records for the user
        List<RatingRecord> ratings = MsoyServer.ratingRepo.getRatings(tgtrec.memberId);

        log.info("Loading ratings for " + tgtrec.memberId + ": " + ratings);

        // sort them by rating
        Collections.sort(ratings, new Comparator<RatingRecord>() {
            public int compare (RatingRecord o1, RatingRecord o2) {
                return (o1.rating > o2.rating) ? -1 : (o1.rating == o2.rating) ? 0 : 1;
            }
        });

        // create GameRating records for all the games we know about
        List<GameRating> result = new ArrayList<GameRating>();
        HashIntMap<GameRating> map = new HashIntMap<GameRating>();
        for (RatingRecord record : ratings) {
            GameRating rrec = map.get(Math.abs(record.gameId));
            if (rrec == null) {
                // stop adding results
                if (result.size() >= MAX_PROFILE_MATCHES) {
                    continue;
                }
                rrec = new GameRating();
                rrec.gameId = Math.abs(record.gameId);
                result.add(rrec);
                map.put(rrec.gameId, rrec);
            }
            float rating = record.rating - RatingManagerDelegate.MINIMUM_RATING;
            rating /= (RatingManagerDelegate.MAXIMUM_RATING - RatingManagerDelegate.MINIMUM_RATING);
            if (record.gameId < 0) {
                rrec.singleRating = rating;
            } else {
                rrec.multiRating = rating;
            }
        }

        // now load up and fill in the game details
        for (IntMap.IntEntry<GameRating> entry : map.intEntrySet()) {
            int gameId = entry.getIntKey();
            GameRecord record = MsoyServer.itemMan.getGameRepository().loadGameRecord(gameId);
            if (record == null) {
                log.warning("Player has rating for non-existent game [id=" + gameId + "].");
                entry.getValue().gameName = "";
            } else {
                entry.getValue().gameName = record.name;
                if (record.thumbMediaHash != null) {
                    entry.getValue().gameThumb = new MediaDesc(
                        record.thumbMediaHash, record.thumbMimeType, record.thumbConstraint);
                }
            }
        }

        return result;
    }

    protected void resolveCardData (HashIntMap<MemberCard> cards)
        throws PersistenceException
    {
        for (ProfileRecord profile : MsoyServer.profileRepo.loadProfiles(
                 cards.intKeySet().toIntArray())) {
            MemberCard card = cards.get(profile.memberId);
            card.photo = profile.getPhoto();
            card.headline = profile.headline;
        }
    }

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_GAMES = 20;
}
