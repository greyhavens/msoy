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
package samples.restaurant
{

import mx.controls.Alert;
import mx.rpc.IResponder;
import mx.rpc.events.FaultEvent;

/**
 * A simple responder that will call the result function specified but
 * handles any fault by simply raising an Alert with the specified title.
 */
public class TokenResponder implements IResponder
{
    private var resultHandler:Function;
    private var faultTitle:String;

    public function TokenResponder(result:Function, faultTitle:String=null)
    {
        super();
        resultHandler = result;
        this.faultTitle = faultTitle;
    }
    public function result(data:Object):void
    {
        resultHandler(data);
    }

    public function fault(info:Object):void
    {
        //the info object from an AsyncToken is always a FaultEvent
        Alert.show(FaultEvent(info).fault.toString(), faultTitle);
    }

}

}