//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.ImmutableMap;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.jdbc.DatabaseLiaison;
import com.threerings.msoy.facebook.gwt.FacebookInfo;

/**
 * Manages persistent structures for integrating with Facebook.
 */
@Singleton
public class FacebookRepository extends DepotRepository
{
    /**
     * Creates a new repository.
     */
    @Inject
    public FacebookRepository (PersistenceContext context)
    {
        super(context);

        // we need to drop the primary key to force a refresh
        context.registerMigration(FacebookInfoRecord.class, new SchemaMigration(5) {
            @Override protected int invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException {
                liaison.dropPrimaryKey(conn, "FacebookInfoRecord", "FacebookInfoRecord_pkey");
                return 1;
            }
        });
    }

    /**
     * Loads the Facebook info for the specified game. If no info is registered for the game in
     * question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfo loadGameFacebookInfo (int gameId)
    {
        return loadFacebookInfo(gameId, 0);
    }

    /**
     * Loads the Facebook info for the specified application. If no info is registered for the game
     * in question a blank record is created with gameId filled in but no key or secret.
     */
    public FacebookInfo loadAppFacebookInfo (int appId)
    {
        return loadFacebookInfo(0, appId);
    }

    /**
     * Creates or updates the Facebook info for the game referenced by the supplied record.
     */
    public void updateFacebookInfo (FacebookInfo info)
    {
        if (info.appId != 0 && info.gameId != 0) {
            throw new IllegalArgumentException("Invalid key for facebook info");
        }
        store(FacebookInfoRecord.fromFacebookInfo(info));
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
    public void deleteTemplate (String code, String variant)
    {
        delete(FacebookTemplateRecord.getKey(code, variant));
    }

    /**
     * Loads a list of all saved templates.
     */
    public List<FacebookTemplateRecord> loadTemplates ()
    {
        return findAll(FacebookTemplateRecord.class, CacheStrategy.NONE);
    }

    /**
     * Loads a list of all templates for the given code.
     */
    public List<FacebookTemplateRecord> loadVariants (String code)
    {
        return findAll(FacebookTemplateRecord.class,
            new Where(FacebookTemplateRecord.CODE.eq(code)));
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
    public FacebookActionRecord getLastAction (int memberId, FacebookActionRecord.Type type)
    {
        List<FacebookActionRecord> visits = findAll(FacebookActionRecord.class, new Where(Ops.and(
            FacebookActionRecord.TYPE.eq(type), FacebookActionRecord.MEMBER_ID.eq(memberId))),
            new Limit(0, 1), OrderBy.descending(FacebookActionRecord.TIMESTAMP));
        return visits.size() > 0 ? visits.get(0) : null;
    }

    /**
     * Loads all Facebook actions related to the member of the given id.
     */
    public List<FacebookActionRecord> loadActions (int memberId)
    {
        return findAll(FacebookActionRecord.class, new Where(
            FacebookActionRecord.MEMBER_ID, memberId));
    }

    /**
     * Loads all actions related to a set of members and of the given type.
     */
    public List<FacebookActionRecord> loadActions (
        Collection<Integer> memberIds, FacebookActionRecord.Type type)
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
     * Loads all notifications.
     */
    public List<FacebookNotificationRecord> loadNotifications ()
    {
        return findAll(FacebookNotificationRecord.class,
            OrderBy.ascending(FacebookNotificationRecord.ID));
    }

    /**
     * Updates or adds a new notification.
     */
    public FacebookNotificationRecord storeNotification (String id, String text)
    {
        FacebookNotificationRecord notifRec = loadNotification(id);
        if (notifRec != null) {
            updatePartial(FacebookNotificationRecord.getKey(id), ImmutableMap.of(
                FacebookNotificationRecord.TEXT, text));
            notifRec.text = text;

        } else {
            notifRec = new FacebookNotificationRecord();
            notifRec.id = id;
            notifRec.text = text;
            insert(notifRec);
        }
        return notifRec;
    }

    /**
     * Notes that the notification with the given id has been scheduled to run on the given node.
     */
    public void noteNotificationScheduled (String id, String node)
    {
        updatePartial(FacebookNotificationRecord.getKey(id), ImmutableMap.of(
            FacebookNotificationRecord.NODE, node));
    }

    /**
     * Notes that a notification batch has been kicked off.
     */
    public void noteNotificationStarted (String id)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        updatePartial(FacebookNotificationRecord.getKey(id), ImmutableMap.of(
            FacebookNotificationRecord.STARTED, now,
            FacebookNotificationRecord.FINISHED, Exps.value(null),
            FacebookNotificationRecord.SENT_COUNT, 0,
            FacebookNotificationRecord.USER_COUNT, 0));
    }

    /**
     * Notes a change in the progress of a running notification.
     */
    public void noteNotificationProgress (
        String id, String newProgress, int userCountDelta, int sentCountDelta)
    {
        Map<ColumnExp, Object> updates = Maps.newHashMap();
        if (newProgress != null) {
            updates.put(FacebookNotificationRecord.PROGRESS, newProgress);
        }
        if (userCountDelta != 0) {
            updates.put(FacebookNotificationRecord.USER_COUNT,
                FacebookNotificationRecord.USER_COUNT.plus(userCountDelta));
        }
        if (sentCountDelta != 0) {
            updates.put(FacebookNotificationRecord.SENT_COUNT,
                FacebookNotificationRecord.SENT_COUNT.plus(sentCountDelta));
        }
        updatePartial(FacebookNotificationRecord.getKey(id), updates);
    }

    /**
     * Notes that a notification batch is now finished executing, setting the finished time and
     * clearing the node field.
     */
    public void noteNotificationFinished (String id)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        updatePartial(FacebookNotificationRecord.getKey(id), ImmutableMap.of(
            FacebookNotificationRecord.FINISHED, now,
            FacebookNotificationRecord.NODE, Exps.value(null)));
    }

    /**
     * Loads and returns the text of the notification with the given id, or null if it does not
     * exist.
     */
    public FacebookNotificationRecord loadNotification (String id)
    {
        return load(FacebookNotificationRecord.getKey(id));
    }

    /**
     * Deletes the notification with the given id.
     */
    public void deleteNotification (String id)
    {
        delete(FacebookNotificationRecord.getKey(id));
    }

    protected FacebookInfo loadFacebookInfo (int gameId, int appId)
    {
        FacebookInfoRecord info = load(FacebookInfoRecord.getKey(gameId, appId));
        if (info != null) {
            return info.toFacebookInfo();
        }
        FacebookInfo blank = new FacebookInfo();
        blank.gameId = gameId;
        blank.appId = appId;
        return blank;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FacebookTemplateRecord.class);
        classes.add(FacebookActionRecord.class);
        classes.add(FacebookNotificationRecord.class);
        classes.add(FacebookInfoRecord.class);
    }
}
