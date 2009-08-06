//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.ImmutableMap;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.ValueExp;

/**
 * Manages persistent structures for integrating with Facebook.
 */
@Singleton
public class FacebookRepository extends DepotRepository
{
    /**
     * Assembles the unique id for a trophy published action.
     */
    public static String getTrophyPublishedActionId (int gameId, String trophyIdent)
    {
        return gameId + ":" + trophyIdent;
    }

    /**
     * Creates a new repository.
     */
    @Inject
    public FacebookRepository (PersistenceContext context)
    {
        super(context);
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
     * Records a member having published a trophy with the given identifier and game.
     */
    public void noteTrophyPublished (int memberId, int gameId, String trophyIdent)
    {
        store(createAction(memberId, FacebookActionRecord.Type.PUBLISHED_TROPHY,
            getTrophyPublishedActionId(gameId, trophyIdent)));
    }

    /**
     * Loads all actions performed by the member of the given id.
     */
    public List<FacebookActionRecord> loadActions (int memberId)
    {
        return findAll(FacebookActionRecord.class, new Where(
            FacebookActionRecord.MEMBER_ID, memberId));
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
        return findAll(FacebookNotificationRecord.class);
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
            FacebookNotificationRecord.FINISHED, new ValueExp(null),
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
            FacebookNotificationRecord.NODE, new ValueExp(null)));
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

    /**
     * Creates a new action with the given fields and the current time.
     */
    protected FacebookActionRecord createAction (
        int memberId, FacebookActionRecord.Type type, String id)
    {
        FacebookActionRecord action = new FacebookActionRecord();
        action.memberId = memberId;
        action.type = type;
        action.id = id;
        action.timestamp = new Timestamp(System.currentTimeMillis());
        return action;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FacebookTemplateRecord.class);
        classes.add(FacebookActionRecord.class);
        classes.add(FacebookNotificationRecord.class);
    }
}
