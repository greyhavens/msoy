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

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.gwt.RoomDetail;
import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.server.MediaDescFactory;

/**
 * Contains metadata for a scene in the Whirled.
 */
@Entity(indices={ @Index(name="ixNewAndHot_v3") })
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

    /**
     * Various boolean values for the {@link #flags} field.
     */
    public enum Flag {
        /** Set by the owner if the puppetar should be suppressed in this room. */
        SUPPRESS_PUPPET(1<<0),

        /** The next unused flag. Copy this and update the bit mask when making a new flag. */
        UNUSED(1<<1);

        /**
         * Gets the bit mask for this flag.
         */
        public int getMask ()
        {
            return _mask;
        }

        Flag (int mask)
        {
            _mask = mask;
        }

        protected int _mask;
    }

    // AUTO-GENERATED: FIELDS START
    public static final Class<SceneRecord> _R = SceneRecord.class;
    public static final ColumnExp SCENE_ID = colexp(_R, "sceneId");
    public static final ColumnExp ACCESS_CONTROL = colexp(_R, "accessControl");
    public static final ColumnExp OWNER_TYPE = colexp(_R, "ownerType");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp VERSION = colexp(_R, "version");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp THEME_GROUP_ID = colexp(_R, "themeGroupId");
    public static final ColumnExp DECOR_ID = colexp(_R, "decorId");
    public static final ColumnExp PLAYLIST_CONTROL = colexp(_R, "playlistControl");
    public static final ColumnExp CANONICAL_IMAGE_HASH = colexp(_R, "canonicalImageHash");
    public static final ColumnExp CANONICAL_IMAGE_TYPE = colexp(_R, "canonicalImageType");
    public static final ColumnExp THUMBNAIL_HASH = colexp(_R, "thumbnailHash");
    public static final ColumnExp THUMBNAIL_TYPE = colexp(_R, "thumbnailType");
    public static final ColumnExp ENTRANCE_X = colexp(_R, "entranceX");
    public static final ColumnExp ENTRANCE_Y = colexp(_R, "entranceY");
    public static final ColumnExp ENTRANCE_Z = colexp(_R, "entranceZ");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp BACKGROUND_COLOR = colexp(_R, "backgroundColor");
    public static final ColumnExp FLAGS = colexp(_R, "flags");
    public static final ColumnExp LAST_PUBLISHED = colexp(_R, "lastPublished");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 15;

    /** Define the sort order for the new & hot queries. */
    public static Tuple<SQLExpression, Order> ixNewAndHot_v3 ()
    {
        return Tuple.newTuple(MsoySceneRepository.NEW_AND_HOT_ORDER, OrderBy.Order.ASC);
    }

    /** The unique identifier for this scene. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY, initialValue=6)
    public int sceneId; // initialValue=6 accounts for stock scenes

    /** Access control information. See {@link MsoySceneModel}. */
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

    /** The group id of this room's theme, or 0. */
    public int themeGroupId;

    /** The item id of the decord item used for this scene. */
    public int decorId;

    /** Who can add to the playlist? */
    public byte playlistControl;

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

    /** The default entry point for this scene. X coordinate. */
    public float entranceX;

    /** The default entry point for this scene. Y coordinate. */
    public float entranceY;

    /** The default entry point for this scene. Z coordinate. */
    public float entranceZ;

    /** The current sum of all ratings that have been applied to this room. */
    public int ratingSum;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** TODO: Delete when migrations have run. */
    public float rating;

    /** The color to use under and around the decor by default. */
    public int backgroundColor;

    /** Extra boolean information {@link Flag} */
    public int flags;

    /** When the room was last published, or null if it was never published. */
    @Column(nullable=true) @Index(name="ixLastPublished")
    public Timestamp lastPublished;

    /** Used when loading from the database. */
    public SceneRecord ()
    {
    }

    /**
     * Returns true if the given flag is set for this scene.
     */
    public boolean isSet (Flag flag)
    {
        return (flags & flag.getMask()) != 0;
    }

    /**
     * Sets the given flag to the given value for this scene.
     */
    public void set (Flag flag, boolean val)
    {
        if (val) {
            flags |= flag.getMask();
        } else {
            flags &= ~flag.getMask();
        }
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
        playlistControl = model.playlistControl;
        entranceX = model.entrance.x;
        entranceY = model.entrance.y;
        entranceZ = model.entrance.z;
        backgroundColor = model.backgroundColor;
        set(Flag.SUPPRESS_PUPPET, model.noPuppet);
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
        model.themeId = themeGroupId;

        // create an empty Decor item with just the id
        model.decor = new Decor();
        model.decor.itemId = decorId;

        model.playlistControl = playlistControl;
        model.entrance = new MsoyLocation(entranceX, entranceY, entranceZ, 180);

        model.backgroundColor = backgroundColor;
        model.noPuppet = isSet(Flag.SUPPRESS_PUPPET);
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
        info.rating = getRating();
        info.thumbnail = getSnapshotThumb();
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
        detail.snapshot = getSnapshotFull();
        return detail;
    }

    /**
     * Returns the full snapshot image for this scene, or null if it has none.
     */
    public MediaDesc getSnapshotFull ()
    {
        return (canonicalImageHash == null) ? null:
            MediaDescFactory
                .createMediaDesc(canonicalImageHash, canonicalImageType, MediaDesc.NOT_CONSTRAINED);
    }

    /**
     * Returns the thumbnail snapshot image for this scene, or null if it has none.
     */
    public MediaDesc getSnapshotThumb ()
    {
        return (thumbnailHash == null) ? null:
            MediaDescFactory
                .createMediaDesc(thumbnailHash, thumbnailType, MediaDesc.NOT_CONSTRAINED);
    }

    /**
     * Calculate this item's average rating from the sum and count.
     */
    public float getRating ()
    {
        return (ratingCount > 0) ? (float) ratingSum / ratingCount : 0f;
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
        return newKey(_R, sceneId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SCENE_ID); }
    // AUTO-GENERATED: METHODS END
}
