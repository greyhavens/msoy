//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.sql.Timestamp;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.room.data.AudioData;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.gwt.RoomDetail;
import com.threerings.msoy.room.gwt.RoomInfo;

/**
 * Contains metadata for a scene in the Whirled.
 */
@Entity(indices={ @Index(name="ixNewAndHot") })
public class SceneRecord extends PersistentRecord
{
    /** Enumerates our various stock scenes. */
    public enum Stock {
        /** The default public space created on a blank server. */
        PUBLIC_ROOM(1, "A Common Area"),

        /** The scene we clone to create a new member's room. */
        FIRST_MEMBER_ROOM(2, "Member's First Room"),

        /** The scene we clone when a member purchases an additional room. */
        EXTRA_MEMBER_ROOM(3, "Member's Extra Room"),

        /** The scene we clone to create a new group's hall. */
        FIRST_GROUP_HALL(4, "Group's First Hall"),

        /** The scene we clone when someone purchases an additional room for their group. */
        EXTRA_GROUP_HALL(5, "Group's Extra Hall");

        public int getSceneId () {
            return _sceneId;
        }

        public String getName () {
            return _name;
        }

        Stock (int sceneId, String name) {
            _sceneId = sceneId;
            _name = name;
        }

        protected int _sceneId;
        protected String _name;
    };

    // AUTO-GENERATED: FIELDS START
    public static final Class<SceneRecord> _R = SceneRecord.class;
    public static final ColumnExp SCENE_ID = colexp(_R, "sceneId");
    public static final ColumnExp ACCESS_CONTROL = colexp(_R, "accessControl");
    public static final ColumnExp OWNER_TYPE = colexp(_R, "ownerType");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp VERSION = colexp(_R, "version");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DECOR_ID = colexp(_R, "decorId");
    public static final ColumnExp AUDIO_ID = colexp(_R, "audioId");
    public static final ColumnExp AUDIO_MEDIA_HASH = colexp(_R, "audioMediaHash");
    public static final ColumnExp AUDIO_MEDIA_TYPE = colexp(_R, "audioMediaType");
    public static final ColumnExp CANONICAL_IMAGE_HASH = colexp(_R, "canonicalImageHash");
    public static final ColumnExp CANONICAL_IMAGE_TYPE = colexp(_R, "canonicalImageType");
    public static final ColumnExp THUMBNAIL_HASH = colexp(_R, "thumbnailHash");
    public static final ColumnExp THUMBNAIL_TYPE = colexp(_R, "thumbnailType");
    public static final ColumnExp AUDIO_VOLUME = colexp(_R, "audioVolume");
    public static final ColumnExp ENTRANCE_X = colexp(_R, "entranceX");
    public static final ColumnExp ENTRANCE_Y = colexp(_R, "entranceY");
    public static final ColumnExp ENTRANCE_Z = colexp(_R, "entranceZ");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp LAST_PUBLISHED = colexp(_R, "lastPublished");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 7;

    /** Define the sort order for the new & hot queries. */
    public static Tuple<SQLExpression, Order> ixNewAndHot ()
    {
        return Tuple.newTuple(MsoySceneRepository.NEW_AND_HOT_ORDER, OrderBy.Order.ASC);
    }

    /** The unique identifier for this scene. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY, initialValue=6)
    public int sceneId; // initialValue=6 accounts for stock scenes

    /** Access control information. See {@link MsoySceneModel}. */
    @Index(name="ixAccessControl")
    public byte accessControl;

    /** Whether this scene is owned by a member or a group. See {@link MsoySceneModel}. */
    public byte ownerType;

    /** The member id of the owner if this scene. */
    @Index(name="ixOwnerId")
    public int ownerId;

    /** The version of this scene (used to manage scene data modifications). */
    public int version;

    /** The owner supplied name of this scene. */
    public String name;

    /** The item id of the decord item used for this scene. */
    public int decorId;

    /** The item id of this scene's background music. */
    public int audioId;

    /** The hash of this scene's background music. */
    public byte[] audioMediaHash;

    /** The mime type of this scene's background music. */
    public byte audioMediaType;

    /** The hash of this scene's canonical image. */
    @Column(nullable=true)
    public byte[] canonicalImageHash;

    /** The mime type of this scene's image type. */
    public byte canonicalImageType;

    /** The hash of this scene's thumbnail image. */
    @Column(nullable=true)
    public byte[] thumbnailHash;

    /** The mime type of this scene's thumbnail image. */
    public byte thumbnailType;

    /** The volume configure for this scene's background music. */
    public float audioVolume;

    /** The default entry point for this scene. X coordinate. */
    public float entranceX;

    /** The default entry point for this scene. Y coordinate. */
    public float entranceY;

    /** The default entry point for this scene. Z coordinate. */
    public float entranceZ;

    /** The current rating of this room, from 1 to 5. */
    public float rating;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** When the room was last published, or null if it was never published. */
    @Column(nullable=true) @Index(name="ixLastPublished")
    public Timestamp lastPublished;

    /** Used when loading from the database. */
    public SceneRecord ()
    {
    }

    /**
     * Creates and populates a persistent record from the supplied runtime record.
     */
    public SceneRecord (MsoySceneModel model)
    {
        if (model.sceneId > 0) {
            sceneId = model.sceneId;
        }
        accessControl = model.accessControl;
        ownerType = model.ownerType;
        ownerId = model.ownerId;
        version = model.version;
        name = model.name;
        decorId = model.decor.itemId;

        audioId = model.audioData.itemId;
        audioMediaHash = SceneUtil.flattenMediaDesc(model.audioData.media);
        audioMediaType = model.audioData.media.mimeType;
        audioVolume = model.audioData.volume;

        entranceX = model.entrance.x;
        entranceY = model.entrance.y;
        entranceZ = model.entrance.z;
    }

    /**
     * Creates and populates an {@link MsoySceneModel} record from this scene record's data.
     */
    public MsoySceneModel toSceneModel ()
    {
        MsoySceneModel model = new MsoySceneModel();
        model.sceneId = sceneId;
        model.accessControl = accessControl;
        model.ownerType = ownerType;
        model.ownerId = ownerId;
        model.version = version;
        model.name = name.intern();

        // create an empty Decor item with just the id
        model.decor = new Decor();
        model.decor.itemId = decorId;

        AudioData a = model.audioData;
        a.itemId = audioId;
        if (a.itemId != 0) { // only clobber media if the audio item exists
            a.media = SceneUtil.createMediaDesc(audioMediaHash, audioMediaType);
        }
        a.volume = audioVolume;

        model.entrance = new MsoyLocation(entranceX, entranceY, entranceZ, 180);

        return model;
    }

    /**
     * Converts this scene record in to a partially initialized room info record. The
     * {@link RoomInfo#thumbnail} fields must be filled in manually if they are needed.
     */
    public RoomInfo toRoomInfo ()
    {
        RoomInfo info = new RoomInfo();
        info.sceneId = sceneId;
        info.name = name;
        info.rating = rating;
        info.thumbnail = getThumbnail();
        return info;
    }

    /**
     * Converts this scene record in to a partially initialized room detail record. The
     * {@link RoomDetail#owner} and {@link RoomDetail#memberRating} are missing and must be
     * filled in manual if needed. See {@link #toRoomDetail} for caveats on the RoomInfo fields.
     */
    public RoomDetail toRoomDetail ()
    {
        RoomDetail detail = new RoomDetail();
        detail.info = toRoomInfo();
        detail.ratingCount = ratingCount;
        detail.snapshot = getSnapshot();
        return detail;
    }

    /**
     * Returns the full snapshot image for this scene, or null if it has none.
     */
    public MediaDesc getSnapshot ()
    {
        return (canonicalImageHash == null) ? null:
            new MediaDesc(canonicalImageHash, canonicalImageType, MediaDesc.NOT_CONSTRAINED);
    }

    /**
     * Returns the thumbnail snapshot image for this scene, or null if it has none.
     */
    public MediaDesc getThumbnail ()
    {
        return (thumbnailHash == null) ? null:
            new MediaDesc(thumbnailHash, thumbnailType, MediaDesc.NOT_CONSTRAINED);
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SceneRecord}
     * with the supplied key values.
     */
    public static Key<SceneRecord> getKey (int sceneId)
    {
        return new Key<SceneRecord>(
                SceneRecord.class,
                new ColumnExp[] { SCENE_ID },
                new Comparable[] { sceneId });
    }
    // AUTO-GENERATED: METHODS END
}
