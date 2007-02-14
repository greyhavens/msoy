package com.bogocorp.weather.events {

import flash.events.Event;

import com.adobe.webapis.events.ServiceEvent;

public class WeatherResultEvent extends ServiceEvent
{
    /**
     * True if the event is the result of a successful call,
     * False if the call failed.
     */
    public var success :Boolean;

    public static const INDEX_AVAILABLE :String = "indexAvail";

    public static const WEATHER_RESULT :String = "weatherRes";

    /**
     * Construct a WeatherResultEvent of the specified type.
     */
    public function WeatherResultEvent (type :String)
    {
        super(type);
    }
}
