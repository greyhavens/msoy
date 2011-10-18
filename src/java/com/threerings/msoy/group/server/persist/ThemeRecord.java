//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.server.MediaDescFactory;

/**
 *  Contains data specific to the theme aspect of a group.
 */
@Entity
public class ThemeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThemeRecord> _R = ThemeRecord.class;
    public static final ColumnExp<Integer> GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp<Boolean> PLAY_ON_ENTER = colexp(_R, "playOnEnter");
    public static final ColumnExp<Integer> POPULARITY = colexp(_R, "popularity");
    public static final ColumnExp<byte[]> LOGO_MEDIA_HASH = colexp(_R, "logoMediaHash");
    public static final ColumnExp<Byte> LOGO_MIME_TYPE = colexp(_R, "logoMimeType");
    public static final ColumnExp<Byte> LOGO_MEDIA_CONSTRAINT = colexp(_R, "logoMediaConstraint");
    public static final ColumnExp<byte[]> NAV_MEDIA_HASH = colexp(_R, "navMediaHash");
    public static final ColumnExp<Byte> NAV_MIME_TYPE = colexp(_R, "navMimeType");
    public static final ColumnExp<Byte> NAV_MEDIA_CONSTRAINT = colexp(_R, "navMediaConstraint");
    public static final ColumnExp<byte[]> NAV_SEL_MEDIA_HASH = colexp(_R, "navSelMediaHash");
    public static final ColumnExp<Byte> NAV_SEL_MIME_TYPE = colexp(_R, "navSelMimeType");
    public static final ColumnExp<Byte> NAV_SEL_MEDIA_CONSTRAINT = colexp(_R, "navSelMediaConstraint");
    public static final ColumnExp<Integer> NAV_COLOR = colexp(_R, "navColor");
    public static final ColumnExp<Integer> NAV_SEL_COLOR = colexp(_R, "navSelColor");
    public static final ColumnExp<Integer> STATUS_LINKS_COLOR = colexp(_R, "statusLinksColor");
    public static final ColumnExp<Integer> STATUS_LEVELS_COLOR = colexp(_R, "statusLevelsColor");
    public static final ColumnExp<Integer> BACKGROUND_COLOR = colexp(_R, "backgroundColor");
    public static final ColumnExp<Integer> TITLE_BACKGROUND_COLOR = colexp(_R, "titleBackgroundColor");
    public static final ColumnExp<byte[]> CSS_MEDIA_HASH = colexp(_R, "cssMediaHash");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 10;

    /** Extracts the groupId of a record. */
    public static final Function<ThemeRecord, Integer> TO_GROUP_ID =
        new Function<ThemeRecord, Integer>() {
        public Integer apply (ThemeRecord record) {
            return record.groupId;
        }
    };

    /** The groupId of this theme. */
    @Id
    public int groupId;

    /** Whether or not to start this theme group's associated AVRG upon entering a themed room. */
    public boolean playOnEnter;

    /** The long-term population count of this group, an exponential moving average. */
    public int popularity;

    /** A hash code identifying the media for this theme's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The MIME type of this theme's logo. */
    public byte logoMimeType;

    /** The constraint for the logo image. */
    public byte logoMediaConstraint;

    /** A hash code identifying the media for this theme's nav button. */
    @Column(nullable=true)
    public byte[] navMediaHash;

    /** The MIME type of this theme's nav button. */
    public byte navMimeType;

    /** The constraint for the nav image. */
    public byte navMediaConstraint;

    /** A hash code identifying the media for this theme's selected nav button. */
    @Column(nullable=true)
    public byte[] navSelMediaHash;

    /** The MIME type of this theme's selected nav button. */
    public byte navSelMimeType;

    /** The constraint for the selected nav image. */
    public byte navSelMediaConstraint;

    /** The color of the nav button text. */
    @Column(defaultValue=""+Theme.DEFAULT_NAV_COLOR)
    public int navColor = Theme.DEFAULT_THEME.navColor;

    /** The color of the nav selected button text. */
    @Column(defaultValue=""+Theme.DEFAULT_NAV_SEL_COLOR)
    public int navSelColor = Theme.DEFAULT_THEME.navSelColor;

    /** The color of the links in the status panel. */
    @Column(defaultValue=""+Theme.DEFAULT_STATUS_LINKS_COLOR)
    public int statusLinksColor = Theme.DEFAULT_THEME.statusLinksColor;

    /** The color of the levels in the status panel. */
    @Column(defaultValue=""+Theme.DEFAULT_STATUS_LEVELS_COLOR)
    public int statusLevelsColor = Theme.DEFAULT_THEME.statusLevelsColor;

    /** The background colour of the main Whirled UI. */
    @Column(defaultValue=""+Theme.DEFAULT_BACKGROUND_COLOR)
    public int backgroundColor = Theme.DEFAULT_THEME.backgroundColor;

    /** The background colour of title bar, the blue bar below the tabs. */
    @Column(defaultValue=""+Theme.DEFAULT_TITLE_BACKGROUND_COLOR)
    public int titleBackgroundColor = Theme.DEFAULT_THEME.titleBackgroundColor;

    /** A hash of the custom CSS for advanced skinning. Mimetype is assumed to be text/css. */
    @Column(nullable=true)
    public byte[] cssMediaHash;

    public ThemeRecord ()
    {
    }

    public ThemeRecord (int groupId)
    {
        this.groupId = groupId;
    }

    /**
     * Creates a Theme of this record.
     */
    public Theme toTheme (GroupName group)
    {
        return new Theme(group, playOnEnter, toLogo(), toNavButton(), toNavSelButton(),
            navColor, navSelColor, statusLinksColor, statusLevelsColor, backgroundColor,
            titleBackgroundColor, toCssMedia());
    }

    /**
     * Creates a MediaDesc of the theme logo, or returns null if there is none.
     */
    public MediaDesc toLogo ()
    {
        if (logoMediaHash == null) {
            return null;
        }
        return MediaDescFactory.createMediaDesc(logoMediaHash, logoMimeType, logoMediaConstraint);
    }

    /**
     * Creates a MediaDesc of the theme nav button, or returns null if there is none.
     */
    public MediaDesc toNavButton ()
    {
        if (navMediaHash == null) {
            return null;
        }
        return MediaDescFactory.createMediaDesc(navMediaHash, navMimeType, navMediaConstraint);
    }

    /**
     * Creates a MediaDesc of the theme selected nav button, or returns null if there is none.
     */
    public MediaDesc toNavSelButton ()
    {
        if (navSelMediaHash == null) {
            return null;
        }
        return MediaDescFactory
            .createMediaDesc(navSelMediaHash, navSelMimeType, navSelMediaConstraint);
    }

    public MediaDesc toCssMedia ()
    {
        if (cssMediaHash == null) {
            return null;
        }
        return MediaDescFactory.createMediaDesc(cssMediaHash, MediaMimeTypes.TEXT_CSS);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThemeRecord}
     * with the supplied key values.
     */
    public static Key<ThemeRecord> getKey (int groupId)
    {
        return newKey(_R, groupId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(GROUP_ID); }
    // AUTO-GENERATED: METHODS END

}
