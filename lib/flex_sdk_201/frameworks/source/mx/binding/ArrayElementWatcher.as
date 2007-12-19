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

import mx.core.mx_internal;

use namespace mx_internal;

[ExcludeClass]

/**
 *  @private
 */
public class ArrayElementWatcher extends Watcher
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 *  Constructor
	 */
    public function ArrayElementWatcher(document:Object, accessorFunc:Function)
    {
		super();

        this.document = document;
        this.accessorFunc = accessorFunc;
    }

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	private var document:Object;
    
    /**
	 *  @private
	 */
	private var accessorFunc:Function;

    /**
	 *  @private
	 */
    public var arrayWatcher:Watcher;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
    override public function updateParent(parent:Object):void
    {
        if (arrayWatcher.value != null)
        {
            wrapUpdate(function():void
			{
				value = arrayWatcher.value[accessorFunc.apply(document)];
				updateChildren();
			});
        }
    }

    /**
	 *  @private
	 */
    override protected function shallowClone():Watcher
    {
        return new ArrayElementWatcher(document, accessorFunc);
    }
}

}
