package {

import flash.events.Event;

import mx.containers.HBox;
import mx.controls.ComboBox;

import com.bogocorp.weather.NOAAWeatherService;

public class WeatherLogic
{
    public function init (states :ComboBox, stations :ComboBox, panel :HBox) :void
    {
        _states = states;
        _stations = stations;
        _panel = panel;

        // in the station list, display the name of the object
        _stations.labelField = "name";

        _states.addEventListener(Event.CHANGE, handleStatePicked);
        _stations.addEventListener(Event.CHANGE, handleStationPicked);

        _svc = new NOAAWeatherService();
        _svc.getDirectory(directoryReceived);
    }

    protected function directoryReceived () :void
    {
        _states.dataProvider = _svc.getStates();
    }

    protected function handleStatePicked (event :Event) :void
    {
        var state :String = String(_states.selectedItem);
        _stations.dataProvider = _svc.getStations(state);
    }

    protected function handleStationPicked (event :Event) :void
    {
        var station :Object = _stations.selectedItem;
        _svc.getWeather(station.station, gotWeatherData);
    }

    protected function gotWeatherData (data :XML) :void
    {
        trace("weather: " + data);
    }

    protected var _svc :NOAAWeatherService;

    protected var _states :ComboBox;
    protected var _stations :ComboBox;
    protected var _panel :HBox;
}

}
