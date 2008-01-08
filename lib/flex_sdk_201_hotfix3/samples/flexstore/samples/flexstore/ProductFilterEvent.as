////////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
// All Rights Reserved.
// The following is Sample Code and is subject to all restrictions on such code
// as contained in the End User License Agreement accompanying this product.
// If you have received this file from a source other than Adobe,
// then your use, modification, or distribution of it requires
// the prior written permission of Adobe.
//
////////////////////////////////////////////////////////////////////////////////
package samples.flexstore
{

import flash.events.Event;

public class ProductFilterEvent extends Event
{
    public static const FILTER:String = "filter";
    
    public var live:Boolean;
    public var filter:ProductFilter;
    
    public function ProductFilterEvent(filter:ProductFilter, live:Boolean)
    {
        super(FILTER);
        this.filter = filter;
        this.live = live;
    }
}

}