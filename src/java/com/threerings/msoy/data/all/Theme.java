//
// $Id: $

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.orth.data.MediaDesc;

/**
 *  Contains the definition of a Theme.
 */
public class Theme extends SimpleStreamableObject
    implements IsSerializable
{
    /** Enables or disables many of the Theme UI fragments. */
    public static final boolean isLive () {
        return true;
    }

    /** An instantiated Theme representing the settings for the default Whirled. */
    public static final Theme DEFAULT_THEME = createDefaultTheme();

    /** The default color of the nav button text. */
    public static final int DEFAULT_NAV_COLOR = 0x216ca3;

    /** The default color of the nav selected button text. */
    public static final int DEFAULT_NAV_SEL_COLOR = 0xffffff;

    /** The default color of the links in the status panel. */
    public static final int DEFAULT_STATUS_LINKS_COLOR = 0x216ca3;

    /** The default color of the levels in the status panel. */
    public static final int DEFAULT_STATUS_LEVELS_COLOR = 0x474b4d;

    /** The default colour of the web header background. */
    public static final int DEFAULT_BACKGROUND_COLOR = 0xffffff;

    /** The default background color of the title bar. */
    public static final int DEFAULT_TITLE_BACKGROUND_COLOR = 0x8dc5e9;

    /** Identifies the logo media. */
    public static final String LOGO_MEDIA = "logo";

    /** Identifies the facebook logo media. */
    public static final String FACEBOOK_LOGO_MEDIA = "facebook_logo";

    /** Identifies the nav button media. */
    public static final String NAV_MEDIA = "nav";

    /** Identifies the nav button media. */
    public static final String NAV_SEL_MEDIA = "navsel";

    /** Identifies the custom CSS media. */
    public static final String CSS = "css";

    /** The group of this theme. */
    public GroupName group;

    /** Whether or not we start playing this group's associated AVRG upon room entry. */
    public boolean playOnEnter;

    /** The media of the theme's Whirled logo replacement image. */
    public MediaDesc logo;

    /** The media of the theme's Whirled nav button replacement image. */
    public MediaDesc navButton;

    /** The media of the theme's Whirled nav selected button replacement image. */
    public MediaDesc navSelButton;

    /** The color of the nav button text. */
    public int navColor;

    /** The color of the nav selected button text. */
    public int navSelColor;

    /** The color of the links in the status panel. */
    public int statusLinksColor;

    /** The color of the levels in the status panel. */
    public int statusLevelsColor;

    /** The background colour of the main Whirled UI. */
    public int backgroundColor;

    /** The background color of the title bar. */
    public int titleBackgroundColor;

    /** Custom CSS. */
    public MediaDesc cssMedia;

    /**
     * An empty constructor for deserialization
     */
    public Theme ()
    {
    }

    /**
     * An initialization constructor.
     */
    public Theme (GroupName group, boolean playOnEnter, MediaDesc logo, MediaDesc navButton,
        MediaDesc navSelButton, int navColor, int navSelColor, int statusLinksColor,
        int statusLevelsColor, int backgroundColor, int titleBackgroundColor, MediaDesc cssMedia)
    {
        this.group = group;
        this.playOnEnter = playOnEnter;
        this.logo = logo;
        this.navButton = navButton;
        this.navSelButton = navSelButton;
        this.navColor = navColor;
        this.navSelColor = navSelColor;
        this.statusLinksColor = statusLinksColor;
        this.statusLevelsColor = statusLevelsColor;
        this.backgroundColor = backgroundColor;
        this.titleBackgroundColor = titleBackgroundColor;
        this.cssMedia = cssMedia;
    }

    /**
     * Returns this group's logo, or the default.
     */
    public MediaDesc getLogo ()
    {
        return (logo != null) ? logo : getDefaultThemeLogoMedia();
    }

    /**
     * Returns this group's facebook logo, or the default.
     */
    public MediaDesc getFacebookLogo ()
    {
        return getDefaultThemeFacebookLogoMedia();
    }

    /**
     * Returns this group's nav button, or the default.
     */
    public MediaDesc getNavButton ()
    {
        return (navButton != null) ? navButton : getDefaultThemeNavButtonMedia();
    }

    /**
     * Returns this group's nav selected button, or the default.
     */
    public MediaDesc getNavSelButton ()
    {
        return (navSelButton != null) ? navSelButton : getDefaultThemeNavSelButtonMedia();
    }

    public int getGroupId ()
    {
        return (group != null) ? group.getGroupId() : 0;
    }

    @Override
    public int hashCode ()
    {
        return getGroupId();
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof Theme)) {
            return false;
        }
        Theme other = (Theme)o;
        if (playOnEnter != other.playOnEnter) {
            return false;
        }
        return ((group != null) ? group.equals(other.group) : (other.group == null));
    }

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    protected static MediaDesc getDefaultThemeLogoMedia ()
    {
        return new InternalMediaDesc(DEFAULT_LOGO_URL, MediaMimeTypes.IMAGE_PNG,
            MediaDesc.HORIZONTALLY_CONSTRAINED);
    }

    /**
     * Creates a default facebook logo for use with groups that have no facebook logo.
     */
    protected static MediaDesc getDefaultThemeFacebookLogoMedia ()
    {
        return new InternalMediaDesc(DEFAULT_FACEBOOK_LOGO_URL, MediaMimeTypes.IMAGE_PNG,
            MediaDesc.HORIZONTALLY_CONSTRAINED);
    }

    /**
     * Creates a default nav button for use with groups that have none.
     */
    protected static MediaDesc getDefaultThemeNavButtonMedia ()
    {
        return new InternalMediaDesc(DEFAULT_NAV_URL, MediaMimeTypes.IMAGE_PNG,
            MediaDesc.HORIZONTALLY_CONSTRAINED);
    }

    /**
     * Creates a default nav button for use with groups that have none.
     */
    protected static MediaDesc getDefaultThemeNavSelButtonMedia ()
    {
        return new InternalMediaDesc(DEFAULT_NAV_SEL_URL, MediaMimeTypes.IMAGE_PNG,
            MediaDesc.HORIZONTALLY_CONSTRAINED);
    }

    protected static Theme createDefaultTheme ()
    {
        return new Theme(null, false, getDefaultThemeLogoMedia(),
            getDefaultThemeNavButtonMedia(), getDefaultThemeNavSelButtonMedia(),
            DEFAULT_NAV_COLOR, DEFAULT_NAV_SEL_COLOR, DEFAULT_STATUS_LINKS_COLOR,
            DEFAULT_STATUS_LEVELS_COLOR, DEFAULT_BACKGROUND_COLOR,
            DEFAULT_TITLE_BACKGROUND_COLOR, null);
    }

    // The internal paths for various themable assets.
    protected static final String DEFAULT_LOGO_URL = "images/header/header_logo";
    protected static final String DEFAULT_FACEBOOK_LOGO_URL = "images/facebook/logo";
    protected static final String DEFAULT_NAV_URL = "images/header/navi_button_bg";
    protected static final String DEFAULT_NAV_SEL_URL = "images/header/navi_button_selected_bg";
}
