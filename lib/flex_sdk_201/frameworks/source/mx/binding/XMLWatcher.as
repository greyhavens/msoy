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
import mx.utils.XMLNotifier;
import mx.utils.IXMLNotifiable;

use namespace mx_internal;

[ExcludeClass]

/**
 *  @private
 */
public class XMLWatcher extends Watcher implements IXMLNotifiable
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  Constructor.
	 */
	public function XMLWatcher(propertyName:String)
    {
		super();

        _propertyName = propertyName;
    }

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
     *  The parent object of this property.
     */
    private var parentObj:Object;

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  propertyName
	//----------------------------------

	/**
     *  Storage for the propertyName property.
     */
    private var _propertyName:String;

    /**
     *  The name of the property this Watcher is watching.
     */
    public function get propertyName():String
    {
        return _propertyName;
    }

	//--------------------------------------------------------------------------
	//
	//  Overridden methods: Watcher
	//
	//--------------------------------------------------------------------------

    /**
     *  If the parent has changed we need to update ourselves
     */
    override public function updateParent(parent:Object):void
    {
        if (parentObj && (parentObj is XML || parentObj is XMLList))
            XMLNotifier.getInstance().unwatchXML(parentObj, this);

        if (parent is Watcher)
            parentObj = parent.value;
        else
            parentObj = parent;

        if (parentObj && (parentObj is XML || parentObj is XMLList))
            XMLNotifier.getInstance().watchXML(parentObj, this);

		// Now get our property.
        wrapUpdate(updateProperty);
    }

	/**
	 *  @private
	 */
    override protected function shallowClone():Watcher
    {
        return new XMLWatcher(_propertyName);
    }

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Gets the actual property then updates
	 *  the Watcher's children appropriately.
     */
    private function updateProperty():void
    {
        if (parentObj)
        {
            if (_propertyName == "this")
                value = parentObj;
            else
                value = parentObj[_propertyName];
        }
        else
        {
            value = null;
        }

        updateChildren();
    }

	/**
	 *  @private
	 */
    public function xmlNotification(currentTarget:Object, type:String,
							   target:Object, value:Object, detail:Object):void
    {
        updateProperty();

        notifyListeners(true);
    }
}

}
