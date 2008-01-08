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

public class ProductListEvent extends Event
{
    public static const ADD_PRODUCT:String = "addProduct";
    public static const DUPLICATE_PRODUCT:String = "duplicateProduct";
    public static const REMOVE_PRODUCT:String = "removeProduct";
    public static const PRODUCT_QTY_CHANGE:String = "productQtyChange";
    
    public var product:Product;
    
    //making the default bubbles behavior of the event to true since we want
    //it to bubble out of the ProductListItem and beyond
    public function ProductListEvent(type:String, bubbles:Boolean=true, cancelable:Boolean=false)
    {
        super(type, bubbles, cancelable);
    }
    
}

}