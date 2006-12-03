package com.threerings.msoy.world.client.editor {

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoySceneModel;

public class SceneUtils
{
    /**
     * Get the background music of the specified scene.
     */
    public static function getBackgroundAudio (
        sceneModel :MsoySceneModel) :FurniData
    {
        return getBackground(sceneModel, true);
    }

    public static function getBackgroundImage (
        sceneModel :MsoySceneModel) :FurniData
    {
        return getBackground(sceneModel, false);
    }

    protected static function getBackground (
        sceneModel :MsoySceneModel, audio :Boolean) :FurniData
    {
        var fd :FurniData;
        for each (fd in sceneModel.furnis) {
            if ((fd.actionType == FurniData.BACKGROUND) &&
                    (audio == fd.media.isAudio())) {
                return fd;
            }
        }
        return null;
    }
}
}
