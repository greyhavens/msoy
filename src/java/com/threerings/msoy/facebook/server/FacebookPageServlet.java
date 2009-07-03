//
// $Id$

package com.threerings.msoy.facebook.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.inject.Inject;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.msoy.server.FacebookLogic;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.facebook.data.FacebookCodes;
import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookService;

import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;

import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Implementation of FacebookService.
 * TODO: rename this FacebookServlet and the class currently using that name
 * FacebookCallbackServlet since this fits better into the convention of what servlets do
 */
public class FacebookPageServlet extends MsoyServiceServlet
    implements FacebookService
{
    @Override // from FacebookService
    public List<FacebookFriendInfo> getFriends ()
        throws ServiceException
    {
        // TODO: remove heavy log.info
        MemberRecord mrec = requireAuthedUser();
        log.info("Loading facebook page friends", "memberId", mrec.memberId);

        // get the session key
        String sessionKey = _memberRepo.lookupExternalSessionKey(
            ExternalAuther.FACEBOOK, mrec.memberId);
        if (sessionKey == null) {
            throw new ServiceException(FacebookCodes.NO_SESSION);
        }

        log.info("Got session key", "sessionKey", sessionKey);

        // get facebook friends to seed (more accurate)
        Set<String> facebookFriendIds = Sets.newHashSet();
        try {
            FacebookJaxbRestClient client = _fbLogic.getFacebookClient(sessionKey);
            for (Long uid : client.friends_get().getUid()) {
                facebookFriendIds.add(String.valueOf(uid));
            }

        } catch (FacebookException fe) {
            log.warning("Unable to get facebook friends for friend bar",
                "memberId", mrec.memberId, fe);
            // pass along the translated text for now
            throw new ServiceException(fe.getMessage());
        }

        log.info("Got fb friends", "size", facebookFriendIds.size());

        // filter by those hooked up to Whirled and set up return map
        IntMap<FacebookFriendInfo> friendsInfo = IntMaps.newHashIntMap();
        for (ExternalMapRecord exRec : _memberRepo.loadExternalAccounts(
            ExternalAuther.FACEBOOK, facebookFriendIds)) {
            FacebookFriendInfo info = new FacebookFriendInfo();
            info.facebookUid = Long.valueOf(exRec.externalId);
            info.memberId = exRec.memberId;
            friendsInfo.put(exRec.memberId, info);
        }

        log.info("Got mapped friends", "size", friendsInfo.size());

        // insert level
        for (MemberCardRecord friend : _memberRepo.loadMemberCards(friendsInfo.keySet())) {
            FacebookFriendInfo info = friendsInfo.get(friend.memberId);
            info.level = friend.level;
        }

        // insert coins
        for (MemberAccountRecord friendAcc : _moneyRepo.loadAll(friendsInfo.keySet())) {
            FacebookFriendInfo info = friendsInfo.get(friendAcc.memberId);
            info.coins = friendAcc.coins;
        }

        // find the last played games & ratings (1p only) + collect game ids
        IntMap<FacebookFriendInfo.Thumbnail> games = IntMaps.newHashIntMap();
        IntMap<RatingRecord> lastGames = IntMaps.newHashIntMap();
        for (RatingRecord rating : _ratingRepo.getMostRecentRatings(friendsInfo.keySet(), -1)) {
            lastGames.put(rating.playerId, rating);
            FacebookFriendInfo.Thumbnail thumbnail = new FacebookFriendInfo.Thumbnail();
            thumbnail.id = Math.abs(rating.gameId);
            games.put(thumbnail.id, thumbnail);
        }

        log.info("Got most recent ratings", "size", lastGames.size());

        // load up games
        for (GameInfoRecord grec : _mgameRepo.loadGenre(GameGenre.ALL, 200)) {
            FacebookFriendInfo.Thumbnail thumbnail = games.get(grec.gameId);
            if (thumbnail == null) {
                continue;
            }
            thumbnail.media = grec.getThumbMedia();
            thumbnail.name = grec.name;
        }

        // build final list and assign last games
        List<FacebookFriendInfo> result = Lists.newArrayList();
        for (FacebookFriendInfo info : friendsInfo.values()) {
            RatingRecord lastGame = lastGames.get(info.memberId);
            FacebookFriendInfo.Thumbnail thumbnail = lastGame != null ?
                games.get(Math.abs(lastGame.gameId)) : null;
            if (thumbnail == null || thumbnail.media == null) {
                // no point in returning folks with no last game or whose last game was deleted
                // or something
                log.info("Null thing", "thumbnail", thumbnail, "lastGame", lastGame);
                continue;
            }
            info.lastGame = thumbnail;
            result.add(info);
        }

        log.info("Got friends with games", "size", result.size());

        // TODO: get the trophy counts for each friend

        // sort by level
        Collections.sort(result, new Comparator<FacebookFriendInfo>() {
            @Override public int compare (FacebookFriendInfo o1, FacebookFriendInfo o2) {
                // highest levels first
                int cmp = o2.level - o1.level;
                if (cmp == 0) {
                    // disambiguate on member id
                    cmp = o1.memberId - o2.memberId;
                }
                return cmp;
            }
        });
        return result;
    }

    @Inject protected FacebookLogic _fbLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected RatingRepository _ratingRepo;
}
