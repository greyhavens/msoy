//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.MochiGameInfo;

/**
 * Possibly temporary info on a mochi game.
 */
public class MochiGameInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MochiGameInfoRecord> _R = MochiGameInfoRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp TAG = colexp(_R, "tag");
    public static final ColumnExp CATEGORIES = colexp(_R, "categories");
    public static final ColumnExp AUTHOR = colexp(_R, "author");
    public static final ColumnExp DESC = colexp(_R, "desc");
    public static final ColumnExp THUMB_URL = colexp(_R, "thumbURL");
    public static final ColumnExp SWF_URL = colexp(_R, "swfURL");
    public static final ColumnExp WIDTH = colexp(_R, "width");
    public static final ColumnExp HEIGHT = colexp(_R, "height");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The unique identifier for this game. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int id;

    /** The name of the game. */
    @Column(length=GameInfo.MAX_NAME_LENGTH)
    public String name;

    /** This game's tag. */
    @Index public String tag;

    /** The categories. */
    public String categories;

    /** The author. */
    public String author;

    /** A more detailed description of the game. */
    @Column(length=GameInfo.MAX_DESCRIPTION_LENGTH)
    public String desc;

    /** Url to the thumbnail. */
    public String thumbURL;

    /** Url to the swf. */
    public String swfURL;

    /** Width of the swf. */
    public int width;

    /** Height of the swf. */
    public int height;

    /**
     */
    public MochiGameInfo toGameInfo ()
    {
        MochiGameInfo info = new MochiGameInfo();
        info.name = name;
        info.tag = tag;
        info.categories = categories;
        info.author = author;
        info.desc = desc;
        info.thumbURL = thumbURL;
        info.swfURL = swfURL;
        info.width = width;
        info.height = height;
        return info;
    }

    /**
     * Updates this record with changes provided by the creator.
     */
    public void update (MochiGameInfo info)
    {
        this.name = info.name;
        this.tag = info.tag;
        this.categories = info.categories;
        this.author = info.author;
        this.desc = info.desc;
        this.thumbURL = info.thumbURL;
        this.swfURL = info.swfURL;
        this.width = info.width;
        this.height = info.height;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MochiGameInfoRecord}
     * with the supplied key values.
     */
    public static Key<MochiGameInfoRecord> getKey (int id)
    {
        return new Key<MochiGameInfoRecord>(
                MochiGameInfoRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END

    /** A Function to turn records into runtime reps. */
    public static final Function<MochiGameInfoRecord,MochiGameInfo> TO_INFO =
        new Function<MochiGameInfoRecord,MochiGameInfo>() {
            public MochiGameInfo apply (MochiGameInfoRecord rec) {
                return rec.toGameInfo();
            }
        };
}
