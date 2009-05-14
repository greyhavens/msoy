//
// $Id$

package com.threerings.msoy.game.util {

/**
 * Codes and utility methods for games.
 */
public class GameUtil
{
    /** Value of groupId when there is no associated group */
    public static const NO_GROUP :int = 0;

    /**
     * Returns true if the specified game is a developer's in-progress original game rather than
     * one listed in the catalog.
     */
    public static function isDevelopmentVersion (gameId :int) :Boolean
    {
        return (gameId < 0);
    }
}
}
