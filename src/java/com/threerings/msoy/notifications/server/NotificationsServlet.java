//
// $Id$

package com.threerings.msoy.notifications.server;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.threerings.msoy.facebook.server.persist.FacebookActionRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;

import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.notifications.gwt.Notification;
import com.threerings.msoy.notifications.gwt.NotificationType;
import com.threerings.msoy.notifications.gwt.NotificationsService;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation for {@link NotificationService}.
 */
public class NotificationsServlet extends MsoyServiceServlet
    implements NotificationsService
{
    @Override // from NotificationService
    public List<Notification> getNotifications ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        long now = System.currentTimeMillis();
        List<Notification> result = Lists.newArrayList();

        // set up the map of already-published trophies
        Map<String, FacebookActionRecord> published = Maps.newHashMap();
        for (FacebookActionRecord action : _facebookRepo.loadActions(memrec.memberId)) {
            if (action.type == FacebookActionRecord.Type.PUBLISHED_TROPHY) {
                published.put(action.id, action);
            }
        }

        // add notifications for recent, unpublished trophies
        for (TrophyRecord trophy : _trophyRepo.loadRecentTrophies(
            memrec.memberId, MAX_RECENT_TROPHIES)) {
            if (now - trophy.whenEarned.getTime() > MAX_TROPHY_AGE) {
                break;
            }
            if (published.containsKey(toActionId(trophy))) {
                continue;
            }

            Notification notif = new Notification();
            notif.type = NotificationType.TROPHY;
            notif.data = new Notification.TrophyData(trophy.toTrophy());
            result.add(notif);
        }

        return result;
    }

    protected static String toActionId (TrophyRecord trophy)
    {
        return FacebookRepository.getTrophyPublishedActionId(trophy.gameId, trophy.ident);
    }

    protected static final int MAX_TROPHY_NOTIFICATIONS = 10;
    protected static final int MAX_RECENT_TROPHIES = 50;
    protected static final long MAX_TROPHY_AGE = 3 * 24 * 60 * 60 * 1000L; // 3 days

    @Inject FacebookRepository _facebookRepo;
    @Inject MsoyGameRepository _mgameRepo;
    @Inject TrophyRepository _trophyRepo;
    @Inject TrophySourceRepository _trophySourceRepo;
}
