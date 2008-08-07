//
// $Id$

package com.threerings.msoy.room.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.room.data.AudioData;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Contains metadata for a scene in the Whirled.
 */
@Entity(indices={@Index(name="ixOwnerId", fields={"ownerId"})})
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
    /** The column identifier for the {@link #sceneId} field. */
    public static final String SCENE_ID = "sceneId";

    /** The qualified column identifier for the {@link #sceneId} field. */
    public static final ColumnExp SCENE_ID_C =
        new ColumnExp(SceneRecord.class, SCENE_ID);

    /** The column identifier for the {@link #accessControl} field. */
    public static final String ACCESS_CONTROL = "accessControl";

    /** The qualified column identifier for the {@link #accessControl} field. */
    public static final ColumnExp ACCESS_CONTROL_C =
        new ColumnExp(SceneRecord.class, ACCESS_CONTROL);

    /** The column identifier for the {@link #ownerType} field. */
    public static final String OWNER_TYPE = "ownerType";

    /** The qualified column identifier for the {@link #ownerType} field. */
    public static final ColumnExp OWNER_TYPE_C =
        new ColumnExp(SceneRecord.class, OWNER_TYPE);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(SceneRecord.class, OWNER_ID);

    /** The column identifier for the {@link #version} field. */
    public static final String VERSION = "version";

    /** The qualified column identifier for the {@link #version} field. */
    public static final ColumnExp VERSION_C =
        new ColumnExp(SceneRecord.class, VERSION);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(SceneRecord.class, NAME);

    /** The column identifier for the {@link #decorId} field. */
    public static final String DECOR_ID = "decorId";

    /** The qualified column identifier for the {@link #decorId} field. */
    public static final ColumnExp DECOR_ID_C =
        new ColumnExp(SceneRecord.class, DECOR_ID);

    /** The column identifier for the {@link #audioId} field. */
    public static final String AUDIO_ID = "audioId";

    /** The qualified column identifier for the {@link #audioId} field. */
    public static final ColumnExp AUDIO_ID_C =
        new ColumnExp(SceneRecord.class, AUDIO_ID);

    /** The column identifier for the {@link #audioMediaHash} field. */
    public static final String AUDIO_MEDIA_HASH = "audioMediaHash";

    /** The qualified column identifier for the {@link #audioMediaHash} field. */
    public static final ColumnExp AUDIO_MEDIA_HASH_C =
        new ColumnExp(SceneRecord.class, AUDIO_MEDIA_HASH);

    /** The column identifier for the {@link #audioMediaType} field. */
    public static final String AUDIO_MEDIA_TYPE = "audioMediaType";

    /** The qualified column identifier for the {@link #audioMediaType} field. */
    public static final ColumnExp AUDIO_MEDIA_TYPE_C =
        new ColumnExp(SceneRecord.class, AUDIO_MEDIA_TYPE);

    /** The column identifier for the {@link #audioVolume} field. */
    public static final String AUDIO_VOLUME = "audioVolume";

    /** The qualified column identifier for the {@link #audioVolume} field. */
    public static final ColumnExp AUDIO_VOLUME_C =
        new ColumnExp(SceneRecord.class, AUDIO_VOLUME);

    /** The column identifier for the {@link #entranceX} field. */
    public static final String ENTRANCE_X = "entranceX";

    /** The qualified column identifier for the {@link #entranceX} field. */
    public static final ColumnExp ENTRANCE_X_C =
        new ColumnExp(SceneRecord.class, ENTRANCE_X);

    /** The column identifier for the {@link #entranceY} field. */
    public static final String ENTRANCE_Y = "entranceY";

    /** The qualified column identifier for the {@link #entranceY} field. */
    public static final ColumnExp ENTRANCE_Y_C =
        new ColumnExp(SceneRecord.class, ENTRANCE_Y);

    /** The column identifier for the {@link #entranceZ} field. */
    public static final String ENTRANCE_Z = "entranceZ";

    /** The qualified column identifier for the {@link #entranceZ} field. */
    public static final ColumnExp ENTRANCE_Z_C =
        new ColumnExp(SceneRecord.class, ENTRANCE_Z);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The unique identifier for this scene. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY, initialValue=6)
    public int sceneId; // initialValue=6 accounts for stock scenes

    /** Access control information. See {@link MsoySceneModel}. */
    public byte accessControl;

    /** Whether this scene is owned by a member or a group. See {@link MsoySceneModel}. */
    public byte ownerType;

    /** The member id of the owner if this scene. */
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

    /** The volume configure for this scene's background music. */
    public float audioVolume;

    /** The default entry point for this scene. X coordinate. */
    public float entranceX;

    /** The default entry point for this scene. Y coordinate. */
    public float entranceY;

    /** The default entry point for this scene. Z coordinate. */
    public float entranceZ;

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

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #SceneRecord}
     * with the supplied key values.
     */
    public static Key<SceneRecord> getKey (int sceneId)
    {
        return new Key<SceneRecord>(
                SceneRecord.class,
                new String[] { SCENE_ID },
                new Comparable[] { sceneId });
    }
    // AUTO-GENERATED: METHODS END
}
