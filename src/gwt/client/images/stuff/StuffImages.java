//
// $Id$

package client.images.stuff;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Contains images for My Stuff navigation.
 * Rollover images == Selected Images
 */
public interface StuffImages extends ImageBundle
{
    @Resource("music_default.png")
    AbstractImagePrototype audio ();

    @Resource("music_selected.png")
    AbstractImagePrototype audio_s ();
    
    @Resource("music_down.png")
    AbstractImagePrototype audio_d ();

    @Resource("avatars_default.png")
    AbstractImagePrototype avatar ();

    @Resource("avatars_selected.png")
    AbstractImagePrototype avatar_s ();

    @Resource("avatars_down.png")
    AbstractImagePrototype avatar_d ();

    @Resource("decor_default.png")
    AbstractImagePrototype decor ();

    @Resource("decor_selected.png")
    AbstractImagePrototype decor_s ();

    @Resource("decor_down.png")
    AbstractImagePrototype decor_d ();

    @Resource("furni_default.png")
    AbstractImagePrototype furniture ();

    @Resource("furni_selected.png")
    AbstractImagePrototype furniture_s ();

    @Resource("furni_down.png")
    AbstractImagePrototype furniture_d ();

    @Resource("games_default.png")
    AbstractImagePrototype game ();

    @Resource("games_selected.png")
    AbstractImagePrototype game_s ();
    
    @Resource("games_down.png")
    AbstractImagePrototype game_d ();

    @Resource("pets_default.png")
    AbstractImagePrototype pet ();

    @Resource("pets_selected.png")
    AbstractImagePrototype pet_s ();

    @Resource("pets_down.png")
    AbstractImagePrototype pet_d ();

    @Resource("images_default.png")
    AbstractImagePrototype photo ();

    @Resource("images_selected.png")
    AbstractImagePrototype photo_s ();

    @Resource("images_down.png")
    AbstractImagePrototype photo_d ();

    @Resource("toys_default.png")
    AbstractImagePrototype toy ();

    @Resource("toys_selected.png")
    AbstractImagePrototype toy_s ();

    @Resource("toys_down.png")
    AbstractImagePrototype toy_d ();

    @Resource("videos_default.png")
    AbstractImagePrototype video ();

    @Resource("videos_selected.png")
    AbstractImagePrototype video_s ();

    @Resource("videos_down.png")
    AbstractImagePrototype video_d ();
}
