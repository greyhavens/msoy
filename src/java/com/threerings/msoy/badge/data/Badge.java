//
// $Id$

package com.threerings.msoy.badge.data;

import java.util.zip.CRC32;

import com.samskivert.util.HashIntMap;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.stats.Log;

import com.google.gwt.user.client.rpc.IsSerializable;

public abstract class Badge extends SimpleStreamableObject
    implements IsSerializable
{
    /**
     * Defines the various badge categories, which can be used to suggest to the user
     * which badges to pursue next, based on their past activity.
     */
    public static enum Category
    {
        SOCIAL, GAME, CREATION, EXPLORATION
    };

    /** Defines the various badge types. */
    public static enum Type
    {
        FRIEND_1(Category.SOCIAL, 1000),

        ;

        /**
         * Badge types can override this to apply constraints to Badges (e.g., only unlocked when
         * another badge is earned.)
         */
        public boolean isUnlocked (MemberObject user) {
            return true;
        }

        /**
         * Overridden by badge types to indicate whether the specified user qualifies
         * for this badge.
         */
        public String getProgress (MemberObject user) {
            return "";
        }

        /**
         * Returns the unique code for this badge type, which is a function of its name.
         */
        public final int getCode() {
            return _code;
        }

        /**
         * Returns the Category this badge falls under.
         */
        public Category getCategory () {
            return _category;
        }

        /**
         * Returns the number of coins awarded to a player who completes this badge.
         */
        public int getCoinValue () {
            return _coinValue;
        }

        Type (Category category, int coinValue) {
            _category = category;
            _coinValue = coinValue;

            // compute a code for this Type using the CRC32 hash of its name
            _code = CRCUtil.crc32(name());

            if (_codeToType == null) {
                _codeToType = new HashIntMap<Type>();
            }
            if (_codeToType.containsKey(_code)) {
                Log.log.warning("Badge type collision! " + this + " and " + _codeToType.get(_code) +
                    " both map to '" + _code + "'.");
            } else {
                _codeToType.put(_code, this);
            }
        }

        protected Category _category;
        protected int _coinValue;
        protected int _code;

        /**
         * Helper class to calculate CRC hashes for Badge types.
         */
        protected static class CRCUtil
        {
            public static int crc32 (String value)
            {
                _crc.reset();
                _crc.update(value.getBytes());
                return (int) _crc.getValue();
            }

            protected static CRC32 _crc = new CRC32();
        }
    };

    /** The unique code representing the type of this badge. */
    public int badgeCode;

    /**
     * A badge becomes suppressed when the user decides they aren't interested in pursuing it.
     * Suppressed badges cannot be acquired, and will not be shown on a user's Passport page.
     * Badges that depend on suppressed Badges to be unlocked will also, therefore, never be
     * shown or acquired.
     */
    public boolean isSuppressed;

    /** Returns the URL where the badge's image is stored */
    //public String getImageUrl (); TODO

    /** Returns this Badge's Type */
    public Type getType ()
    {
        return getType(badgeCode);
    }

    /**
     * Maps a {@link Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        return _codeToType.get(code);
    }

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<Type> _codeToType;
}
