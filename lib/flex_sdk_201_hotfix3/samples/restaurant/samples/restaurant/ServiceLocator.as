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
import mx.rpc.soap.mxml.WebService;
import mx.rpc.AbstractService;
import mx.rpc.events.ResultEvent;
import mx.rpc.events.FaultEvent;
import mx.controls.Alert;

/*
    In this application, a number of classes could require access to a service.
    The role of ServiceLocator is to create a single instance of a WebService
    and to provide that instance to other objects in the application as needed.
    The goal is to avoid repeating <mx:WebServive wsdl="..."/> in all the classes
    that require access to the service.
    There are a couple of benefits to this approach:
    1. Using the WebService tag in each class would tie the view to the way you
       access data.
    2. Even though the WebService object is not heavyweight, there might be a
       performance impact in re-instantiating it (pointing to the same service)
       in many different classes.
    There are other and more sophisticated approaches to achieve the same
    result, including the formal Service Locator pattern implemented for instance
    in the Cairngorm framework.
*/
public class ServiceLocator
{
    private static var restaurantService:AbstractService;
    private static var categoryService:AbstractService;

    public static function getRestaurantService():AbstractService
    {
       if (restaurantService == null) {
            var ws:WebService = new WebService();
            ws.wsdl = "http://www.adobe.com/go/flex_restaurant_restaurant_service?wsdl";
            /* if you want to use  your own WebService adjust the following url
               to match your setup and comment out the one above */
            //ws.wsdl = "http://{server.name}:{server.port}/{context.root}/services/RestaurantWS?wsdl";
            ws.useProxy = false;
            ws.showBusyCursor = true;
            ws.loadWSDL();
            restaurantService = ws;
        }
        return restaurantService;
    }

    public static function getCategoryService():AbstractService
    {
        if (categoryService == null) {
            var ws:WebService = new WebService();
            ws.wsdl = "http://www.adobe.com/go/flex_restaurant_category_service?wsdl";
            /* if you want to use  your own WebService adjust the following url
               to match your setup and comment out the one above */
            //ws.wsdl = "http://{server.name}:{server.port}/{context.root}/services/CategoryWS?wsdl";
            ws.useProxy = false;
            ws.showBusyCursor = true;
            ws.loadWSDL();
            categoryService = ws;
        }
        return categoryService;
    }

}
}
