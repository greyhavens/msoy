package {

import flash.events.Event;

import mx.containers.HBox;
import mx.controls.ComboBox;

import com.bogocorp.weather.NOAAWeatherService;

/**
 * Application logic for WeatherBox.
 */
public class WeatherLogic
{
    /**
     * Init- called by the UI after it is ready to go.
     */
    public function init (box :WeatherBox) :void
    {
        _box = box;

        _box.stateBox.addEventListener(Event.CHANGE, handleStatePicked);
        _box.stationBox.addEventListener(Event.CHANGE, handleStationPicked);

        _svc = new NOAAWeatherService();
        _svc.getDirectory(directoryReceived);
    }

    protected function directoryReceived () :void
    {
        _box.stateBox.dataProvider = _svc.getStates();
    }

    protected function handleStatePicked (event :Event) :void
    {
        var state :String = String(_box.stateBox.selectedItem);
        _box.stationBox.dataProvider = _svc.getStations(state);
        _box.stationBox.enabled = true;
    }

    protected function handleStationPicked (event :Event) :void
    {
        var station :Object = _box.stationBox.selectedItem;
        _svc.getWeather(station.station, gotWeatherData);
    }

    protected function gotWeatherData (data :XML) :void
    {
        _box.iconArea.source = String(data.icon_url_base) + data.icon_url_name;
        _box.weatherLabel.text = data.weather;
        _box.locationLabel.text = data.location;
        _box.tempLabel.text = data.temperature_string;
        _box.windLabel.text = "Wind: " + data.wind_string;
        _box.timestampLabel.text = data.observation_time;

        //trace("weather: " + data);
    }

    protected var _svc :NOAAWeatherService;

    protected var _box :WeatherBox;
}
}
