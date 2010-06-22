//
// $Id$

package com.threerings.msoy.reminders.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.msoy.facebook.server.FacebookLogic;
import com.threerings.msoy.facebook.server.persist.FacebookActionRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.server.persist.ArcadeEntryRecord;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.reminders.gwt.Reminder;
import com.threerings.msoy.reminders.gwt.ReminderType;
import com.threerings.msoy.reminders.gwt.RemindersService;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation for {@link RemindersService}.
 */
public class RemindersServlet extends MsoyServiceServlet
    implements RemindersService
{
    @Override // from RemindersService
    public List<Reminder> getReminders (int appId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        List<Reminder> result = Lists.newArrayList();

        // set up the map of already-published trophies
        Map<String, FacebookActionRecord> published = Maps.newHashMap();
        for (FacebookActionRecord action : _facebookRepo.loadActions(appId, memrec.memberId)) {
            if (action.type == FacebookActionRecord.Type.PUBLISHED_TROPHY) {
                published.put(action.id, action);
            }
        }

        // load up facebook approved games
        Set<Integer> approvedGames = Sets.newHashSet();
        approvedGames.addAll(Lists.transform(_mgameRepo.loadArcadeEntries(
            ArcadeData.Portal.FACEBOOK, true), ArcadeEntryRecord.TO_GAME_ID));

        // add notifications for recent, unpublished trophies in approved games
        Map<Integer, GameInfoRecord> games = Maps.newHashMap();
        for (TrophyRecord trophy : _trophyRepo.loadRecentTrophies(
            memrec.memberId, MAX_RECENT_TROPHIES)) {
            if (published.containsKey(toActionId(trophy)) ||
                !approvedGames.contains(trophy.gameId)) {
                continue;
            }
            GameInfoRecord ginfo = games.get(trophy.gameId);
            if (ginfo == null) {
                games.put(trophy.gameId, ginfo = _mgameRepo.loadGame(trophy.gameId));
                if (ginfo == null) {
                    continue;
                }
            }

            // note we don't resolve the trophy description here; the client only needs it to fill
            // out the trophy info passed to facebook, but our templates don't currently require it
            Reminder reminder = new Reminder();
            reminder.type = ReminderType.TROPHY;
            reminder.data = new Reminder.TrophyData(trophy.toTrophy(), ginfo.name,
                ginfo.description, ginfo.getShotMedia().getMediaPath());
            result.add(reminder);

            if (result.size() == MAX_TROPHY_REMINDERS) {
                break;
            }
        }

        return result;
    }

    protected static String toActionId (TrophyRecord trophy)
    {
        return FacebookActionRecord.getTrophyPublishedId(trophy.gameId, trophy.ident);
    }

    protected static final int MAX_TROPHY_REMINDERS = 10;
    protected static final int MAX_RECENT_TROPHIES = 50;

    protected @Inject FacebookLogic _facebookLogic;
    protected @Inject FacebookRepository _facebookRepo;
    protected @Inject MsoyGameRepository _mgameRepo;
    protected @Inject TrophyRepository _trophyRepo;
    protected @Inject TrophySourceRepository _trophySourceRepo;
}
