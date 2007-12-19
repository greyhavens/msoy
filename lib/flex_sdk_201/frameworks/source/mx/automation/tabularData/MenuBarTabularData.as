////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.tabularData
{

import mx.automation.AutomationManager;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationTabularData;
import mx.controls.MenuBar;
import mx.core.mx_internal;
use namespace mx_internal;

/**
 *  @private
 */
public class MenuBarTabularData
    implements IAutomationTabularData
{

    private var menuBar:MenuBar;
    private var delegate:IAutomationObject;
 
    /**
     *  @private
     */
    public function MenuBarTabularData(delegate:IAutomationObject)
    {
		super();

        this.delegate = delegate;
        this.menuBar = delegate as MenuBar;
    }

    /**
     *  @inheritDoc
     */
    public function get firstVisibleRow():int
    {
    	return 0;
    }
    
    /**
     *  @inheritDoc
     */
    public function get lastVisibleRow():int
    {
        return delegate.numAutomationChildren-1;
    }

    /**
     *  @inheritDoc
     */
    public function get numRows():int
    {
        return delegate.numAutomationChildren;
    }


    /**
     *  @inheritDoc
     */
    public function get numColumns():int
    {
        return 1;
    }

    /**
     *  @inheritDoc
     */
    public function get columnNames():Array
    {
        return ["MenuItems"];
    }

    /**
     *  @inheritDoc
     */
    public function getValues(start:uint = 0, end:uint = 0):Array
    {
    	var _values:Array = [];
		var i:int;
    	for(i = start; i <= end; ++i)
    	{
			var values:Array = delegate.getAutomationChildAt(i).automationValue;
			_values.push([ values.join("|") ]);
    	}
    	
        return _values;
    }
    
    /**
     *  @inheritDoc
     */
    public function getAutomationValueForData(data:Object):Array
    {
    	return [];
    }
}
}
