//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.ImmutableMap;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.server.util.DropPrimaryKey;

/**
 * Manages persistent structures for integrating with Facebook.
 */
@Singleton
public class FacebookRepository extends DepotRepository
{
    public static final int LEGACY_APP_ID = 0;

    /**
     * Creates a new repository.
     */
    @Inject
    public FacebookRepository (PersistenceContext context)
    {
        super(context);

        // workaround to add the new @Id column properly
        context.registerMigration(FacebookInfoRecord.class, new DropPrimaryKey(5));

        // workaround to add the new @Id column properly
        Class<FacebookNotificationRecord> fnr = FacebookNotificationRecord.class;
        context.registerMigration(fnr, new DropPrimaryKey(3));

        // drop the old columns, we don't care about the data in them
        for (String col : new String[] {
            "node", "progress", "started", "finished", "userCount", "sentCount"}) {
            context.registerMigration(fnr, new SchemaMigration.Drop(3, col));
        }

        // explicitly add the app id column, it should not have a default value
        context.registerMigration(fnr, new SchemaMigration.Add(
            3, FacebookNotificationRecord.APP_ID, ""+LEGACY_APP_ID));

        // workaround to add the new @Id column properly
        context.registerMigration(FacebookTemplateRecord.class, new DropPrimaryKey(3));

        // workaround to add the new @Id column properly
        context.registerMigration(FacebookActionRecord.class, new DropPrimaryKey(2));
    }

    /**
     * Loads the Facebook info for the specified game. If no info is registered for the game in
     * question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfoRecord loadGameFacebookInfo (int gameId)
    {
        return loadFacebookInfo(gameId, 0);
    }

    /**
     * Loads the Facebook info for the specified application. If no info is registered for the game
     * in question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfoRecord loadAppFacebookInfo (int appId)
    {
        return loadFacebookInfo(0, appId);
    }

    /**
     * Creates or updates the Facebook info for the game referenced by the supplied record.
     */
    public void updateFacebookInfo (FacebookInfoRecord info)
    {
        Preconditions.checkArgument(info.appId == 0 ^ info.gameId == 0);
        store(info);
    }

    /**
     * Deletes the facebook info for the given game id.
     */
    public void deleteGameFacebookInfo (int gameId)
    {
        delete(FacebookInfoRecord.getKey(gameId, 0));
    }

    /**
     * Deletes the facebook info for the given application id.
     */
    public void deleteAppFacebookInfo (int appId)
    {
        delete(FacebookInfoRecord.getKey(0, appId));
    }

    /**
     * Adds or updates the given template.
     */
    public void storeTemplate (FacebookTemplateRecord template)
    {
        store(template);
    }

    /**
     * Deletes the template record with the given code.
     */
    public void deleteTemplate (int appId, String code, String variant)
    {
        delete(FacebookTemplateRecord.getKey(appId, code, variant));
    }

    /**
     * Loads a list of all saved templates.
     */
    public List<FacebookTemplateRecord> loadTemplates (int appId)
    {
        return findAll(FacebookTemplateRecord.class, CacheStrategy.NONE, new Where(
            FacebookTemplateRecord.APP_ID.eq(appId)));
    }

    /**
     * Loads a list of all templates for the given code.
     */
    public List<FacebookTemplateRecord> loadVariants (int appId, String code)
    {
        return findAll(FacebookTemplateRecord.class, new Where(Ops.and(
            FacebookTemplateRecord.CODE.eq(code), FacebookTemplateRecord.APP_ID.eq(appId))));
    }

    /**
     * Records an action.
     */
    public void recordAction (FacebookActionRecord action)
    {
        store(action);
    }

    /**
     * Gets the most recent record of the given type, or null if there are none.
     */
    public FacebookActionRecord getLastAction (
        int appId, int memberId, FacebookActionRecord.Type type)
    {
        List<FacebookActionRecord> visits = findAll(FacebookActionRecord.class, new Where(Ops.and(
            FacebookActionRecord.TYPE.eq(type), FacebookActionRecord.MEMBER_ID.eq(memberId))),
            new Limit(0, 1), OrderBy.descending(FacebookActionRecord.TIMESTAMP));
        return visits.size() > 0 ? visits.get(0) : null;
    }

    /**
     * Loads all Facebook actions related to the member of the given id.
     */
    public List<FacebookActionRecord> loadActions (int appId, int memberId)
    {
        return findAll(FacebookActionRecord.class, new Where(
            FacebookActionRecord.MEMBER_ID, memberId));
    }

    /**
     * Loads all actions related to a set of members and of the given type.
     */
    public List<FacebookActionRecord> loadActions (
        int appId, Collection<Integer> memberIds, FacebookActionRecord.Type type)
    {
        return findAll(FacebookActionRecord.class, new Where(Ops.and(
            FacebookActionRecord.TYPE.eq(type), FacebookActionRecord.MEMBER_ID.in(memberIds))));
    }

    /**
     * Removes old facbook action records.
     */
    public void pruneActions ()
    {
        // TODO: implement and schedule with cron logic
    }

    /**
     * Loads all notifications for an application.
     */
    public List<FacebookNotificationRecord> loadNotifications (int appId)
    {
        return findAll(FacebookNotificationRecord.class,
            new Where(FacebookNotificationRecord.APP_ID.eq(appId)),
            OrderBy.ascending(FacebookNotificationRecord.ID));
    }

    /**
     * Loads the status of all notification batches for an application.
     */
    public List<FacebookNotificationStatusRecord> loadNotificationStatus (int appId)
    {
        final int MAX_STATUSES = 100;
        return findAll(FacebookNotificationStatusRecord.class,
            new Where(FacebookNotificationStatusRecord.APP_ID.eq(appId)),
            OrderBy.descending(FacebookNotificationStatusRecord.START_TIME),
            new Limit(0, MAX_STATUSES));
    }

    /**
     * Updates or adds a new notification.
     */
    public FacebookNotificationRecord storeNotification (int appId, String id, String text)
    {
        FacebookNotificationRecord notifRec = loadNotification(appId, id);
        if (notifRec != null) {
            updatePartial(FacebookNotificationRecord.getKey(appId, id), ImmutableMap.of(
                FacebookNotificationRecord.TEXT, text));
            notifRec.text = text;

        } else {
            notifRec = new FacebookNotificationRecord();
            notifRec.appId = appId;
            notifRec.id = id;
            notifRec.text = text;
            insert(notifRec);
        }
        return notifRec;
    }

    /**
     * Notes that the notification with the given id has been scheduled to run on the given node.
     */
    public void noteNotificationScheduled (int appId, String batchId, long startTime)
    {
        FacebookNotificationStatusRecord status = new FacebookNotificationStatusRecord();
        status.appId = appId;
        status.batchId = batchId;
        status.startTime = new Timestamp(startTime);
        status.progress = "Scheduled";
        insert(status);
    }

    /**
     * Notes a change in the progress of a running notification.
     */
    public void noteNotificationProgress (
        int appId, String batchId, String newProgress, int userCountDelta, int sentCountDelta)
    {
        Map<ColumnExp, Object> updates = Maps.newHashMap();
        if (newProgress != null) {
            updates.put(FacebookNotificationStatusRecord.PROGRESS, newProgress);
        }
        if (userCountDelta != 0) {
            updates.put(FacebookNotificationStatusRecord.USER_COUNT,
                FacebookNotificationStatusRecord.USER_COUNT.plus(userCountDelta));
        }
        if (sentCountDelta != 0) {
            updates.put(FacebookNotificationStatusRecord.SENT_COUNT,
                FacebookNotificationStatusRecord.SENT_COUNT.plus(sentCountDelta));
        }
        updatePartial(FacebookNotificationStatusRecord.getKey(appId, batchId), updates);
    }

    /**
     * Loads and returns the notification with the given id, or null if it does not exist.
     */
    public FacebookNotificationRecord loadNotification (int appId, String id)
    {
        return load(FacebookNotificationRecord.getKey(appId, id));
    }

    /**
     * Deletes the notification with the given id.
     */
    public void deleteNotification (int appId, String id)
    {
        delete(FacebookNotificationRecord.getKey(appId, id));
    }

    /**
     * Loads all thumbnails assigned to the given game.
     */
    public List<FeedThumbnailRecord> loadGameThumbnails (int gameId)
    {
        return loadThumbnails(gameId, 0);
    }

    /**
     * Sets the given thumbnails to be the ones assigned to the given game. I.e. DELETES all
     * current thumbnails for the game then stores the new ones.
     */
    public void saveGameThumbnails (int gameId, List<FeedThumbnailRecord> thumbnails)
    {
        saveThumbnails(gameId, 0, thumbnails);
    }

    /**
     * Loads all thumbnails assigned to the given application.
     */
    public List<FeedThumbnailRecord> loadAppThumbnails (int appId)
    {
        return loadThumbnails(0, appId);
    }

    /**
     * Sets the given thumbnails to be the ones assigned to the given application. I.e. DELETES all
     * current thumbnails for the app then stores the new ones.
     */
    public void saveAppThumbnails (int appId, List<FeedThumbnailRecord> thumbnails)
    {
        saveThumbnails(0, appId, thumbnails);
    }

    /**
     * Loads the Kontagent info associated with the given application, or null if there is none.
     */
    public KontagentInfoRecord loadKontagentInfo (int appId)
    {
        return load(KontagentInfoRecord.getKey(appId));
    }

    /**
     * Saves the given kontagent info.
     */
    public void saveKontagentInfo (KontagentInfoRecord kinfo)
    {
        store(kinfo);
    }

    protected FacebookInfoRecord loadFacebookInfo (int gameId, int appId)
    {
        FacebookInfoRecord info = load(FacebookInfoRecord.getKey(gameId, appId));
        if (info == null) {
            info = new FacebookInfoRecord();
            info.gameId = gameId;
            info.appId = appId;
        }
        return info;
    }

    protected List<FeedThumbnailRecord> loadThumbnails (int gameId, int appId)
    {
        return findAll(FeedThumbnailRecord.class, new Where(Ops.and(
            FeedThumbnailRecord.GAME_ID.eq(gameId), FeedThumbnailRecord.APP_ID.eq(appId))));
    }

    protected void saveThumbnails (int gameId, int appId, List<FeedThumbnailRecord> thumbs)
    {
        deleteAll(FeedThumbnailRecord.class, new Where(Ops.and(
            FeedThumbnailRecord.GAME_ID.eq(gameId), FeedThumbnailRecord.APP_ID.eq(appId))));
        for (FeedThumbnailRecord thumb : thumbs) {
            insert(thumb);
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FacebookTemplateRecord.class);
        classes.add(FacebookActionRecord.class);
        classes.add(FacebookNotificationRecord.class);
        classes.add(FacebookNotificationStatusRecord.class);
        classes.add(FacebookInfoRecord.class);
        classes.add(FeedThumbnailRecord.class);
        classes.add(KontagentInfoRecord.class);
    }
}
