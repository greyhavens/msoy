package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.ui.List;

import com.threerings.msoy.data.SceneBookmarkEntry;

public class SceneBookmarkList extends List
{
    public function SceneBookmarkList (ctx :MsoyContext)
    {
        super(ctx);
        includeInLayout = false;
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            updateListData();
        } else {
            dataProvider = null;
        }
    }

    protected function updateListData () :void
    {
        var entries :Array = _ctx.getClientObject().recentScenes.toArray();
        entries.sort(function (o1 :Object, o2 :Object) :int {
            var sb1 :SceneBookmarkEntry = (o1 as SceneBookmarkEntry);
            var sb2 :SceneBookmarkEntry = (o2 as SceneBookmarkEntry);
            return int(sb1.lastVisit - sb2.lastVisit);
        });
        dataProvider = entries;
    }

    override protected function itemClicked (obj :Object) :void
    {
        var sbe :SceneBookmarkEntry = (obj as SceneBookmarkEntry);
        // this is a little tricky: we have to dispatch the go scene
        // prior to popping down, or our the go scene doesn't get to the right
        // place
        CommandEvent.dispatch(this, MsoyController.GO_SCENE, sbe.sceneId);
        CommandEvent.dispatch(this, MsoyController.SHOW_RECENT_SCENES, false);
    }
}
}
