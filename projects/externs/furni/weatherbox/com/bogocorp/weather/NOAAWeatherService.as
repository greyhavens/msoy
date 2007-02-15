package com.bogocorp.weather {

import flash.events.Event;
import flash.events.EventDispatcher;

import flash.net.URLLoader;
import flash.net.URLRequest;

import flash.utils.Dictionary;

import com.adobe.webapis.URLLoaderBase;

public class NOAAWeatherService // extends URLLoaderBase
{
    /** Downlaod the directory. */
    public function getDirectory (hollaback :Function) :void
    {
        getDataFromURL(DIRECTORY_URL, function (data :Object) :void {
                _directory = XML(data);
                hollaback();
            });
    }

    /**
     * Get the list of states that have weather feeds.
     */
    public function getStates () :Array
    {
        if (_directory == null) {
            throw new Error("getDirectory() and wait until the callback.");
        }

        // this'd be easier if there was some sorta xml->array
        var uniquer :Dictionary = new Dictionary();
        for each (var state :XML in _directory..state) {
            uniquer[String(state)] = true;
        }
        var states :Array = [];
        for (var stateCode :String in uniquer) {
            states.push(stateCode);
        }
        states.sort(Array.CASEINSENSITIVE);
        return states;
    }

    /**
     * Get the stations available for the specified state.
     *
     * @param stateCode the two-letter code for the state.
     */
    public function getStations (stateCode :String) :Array
    {
        if (_directory == null) {
            throw new Error("getDirectory() and wait until the callback.");
        }

        var stations :Array = [];
        for each (var station :XML in _directory..station.(state == stateCode)) {
            // create an object describing a few key bits of the station
            stations.push({ station: String(station.station_id),
                            label :String(station.station_name),
                            latitude: String(station.latitude),
                            longitude: String(station.longitude)
                          });
        }
        stations.sortOn("label", Array.CASEINSENSITIVE);
        return stations;
    }

    /**
     * Download the weather for the specified station.
     */
    public function getWeather (stationCode :String, hollaback_XML :Function) :void
    {
        var station :XML = _directory..station.(station_id == stationCode)[0];
        getDataFromURL(station.xml_url, function (data :Object) :void {
            hollaback_XML(XML(data));
        });
    }


    protected function getDataFromURL (url :String, callback :Function) :void
    {
        // Isn't it safest to use a new one for every request?
        // WTF are the 'corelib' classes doing, reusing the loader and
        // potentially confusing the callbacks...? Fuqdup.
        var loader :URLLoader = new URLLoader();
        var fn :Function;
        fn = function () :void {
            loader.removeEventListener(Event.COMPLETE, fn);
            callback(loader.data);
        };
        loader.addEventListener(Event.COMPLETE, fn);
        loader.load(new URLRequest(massageURL(url)));
    }

    /**
     * Massage the specified url so that it
     */
    protected function massageURL (url :String) :String
    {
        // Presently, the urls returned in the directory all begin
        // with "weather.gov", however, making a HTTP/1.1 request to that
        // server will return a 301, permanantly moved, to
        // "www.weather.gov". So: the crossdomain.xml that is loaded applies
        // to that site, and not to "weather.gov". The flash player at that
        // point will not even try to load the data at weather.gov, since
        // it's already tried to load the crossdomain.xml for that site,
        // but at the same time it has no permissions there.
        //
        // So: change any urls. This is super fragile!
        return url.replace(new RegExp("^http\:\/\/weather\.gov", "i"),
            "http://www.weather.gov");
    }

    protected var _directory :XML;

    public static const DIRECTORY_URL :String = "http://www.nws.noaa.gov/data/current_obs/index.xml";
}
}
