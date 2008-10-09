//
// $Id$

package client.person;

/**
 * Enumerates our various gallery related actions.
 */
public class GalleryActions
{
    /** Shows all of a member's galleries. */
    public static final String GALLERIES = "galleries";

    /** Shows a gallery by id. */
    public static final String VIEW = "gallery";

    /** Shows the profile gallery for a member id. */
    public static final String VIEW_PROFILE = "pgallery";

    /** Shows a specific photo in a gallery by gallery id and photo id. */
    public static final String VIEW_PHOTO = "photo";

    /** Edits a particular gallery. */
    public static final String EDIT = "edit";

    /** Creates a new gallery. */
    public static final String CREATE = "create";

    /** Edits a player's profile gallery. */
    public static final String CREATE_PROFILE = "pcreate";
}
