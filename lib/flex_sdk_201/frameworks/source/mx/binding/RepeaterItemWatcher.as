////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.binding
{

import mx.collections.CursorBookmark;
import mx.collections.ICollectionView;
import mx.collections.IViewCursor;
import mx.core.mx_internal;
import mx.events.CollectionEvent;

use namespace mx_internal;

[ExcludeClass]

/**
 *  @private
 */
public class RepeaterItemWatcher extends Watcher
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Constructor.
	 */
    public function RepeaterItemWatcher(dataProviderWatcher:PropertyWatcher)
    {
		super();

        this.dataProviderWatcher = dataProviderWatcher;
    }

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
    private var dataProviderWatcher:PropertyWatcher;

	/**
	 *  @private
	 */
    private var clones:Array;

	/**
	 *  @private
	 */
    private var original:Boolean = true;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods: Watcher
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
    override public function updateParent(parent:Object):void
    {
        dataProviderWatcher = PropertyWatcher(parent);

        var dataProvider:ICollectionView = ICollectionView(dataProviderWatcher.value);

        if (dataProvider)
        {
            if (original)
            {
                dataProvider.addEventListener(CollectionEvent.COLLECTION_CHANGE, changedHandler, false, 0, true);
                updateClones(dataProvider);
            }
            else
            {
                wrapUpdate(function():void
                {
                    var iterator:IViewCursor = dataProvider.createCursor();
                    iterator.seek(CursorBookmark.FIRST, cloneIndex);
                    value = iterator.current;
                    updateChildren();
                });
            }
        }
    }

    /**
     *  @private
     *  Handles "Change" events sent by calls to Collection APIs
     *  on the Repeater's dataProvider.
     */
    private function changedHandler(collectionEvent:CollectionEvent):void
    {
        var dataProvider:ICollectionView = ICollectionView(dataProviderWatcher.value);

        if (dataProvider)
            updateClones(dataProvider);
    }

	/**
	 *  @private
	 */
    override protected function shallowClone():Watcher
    {
        return new RepeaterItemWatcher(dataProviderWatcher);
    }

	/**
	 *  @private
	 */
    private function updateClones(dataProvider:ICollectionView):void
    {
        if (clones)
            clones = clones.splice(0, dataProvider.length);
        else
            clones = [];

        for (var i:int = 0; i < dataProvider.length; i++)
        {
            var clone:RepeaterItemWatcher = RepeaterItemWatcher(clones[i]);
                
            if (!clone)
            {
                clone = RepeaterItemWatcher(deepClone(i));
                clone.original = false;
                clones[i] = clone;
            }

            clone.updateParent(dataProviderWatcher);
        }
    }
}

}
