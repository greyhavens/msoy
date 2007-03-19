package com.threerings.msoy.game.data {

public class GameDefinition
{
    public function GameDefinition (config :String) 
    {
        _configXML = XML(config);
        _gameType = parseInt(_configXML..match.@type);
        _minSeats = parseInt(_configXML..match.min_seats[0]);
        _maxSeats = parseInt(_configXML..match.max_seats[0]);
        _unwatchable = _configXML..match.unwatchable != undefined;
    }

    public function get config () :XML
    {
        return _configXML;
    }

    public function get minSeats () :int 
    {
        return _minSeats;
    }

    public function get maxSeats () :int
    {
        return _maxSeats;
    }

    public function get gameType () :int
    {
        return _gameType;
    }

    public function get unwatchable () :Boolean
    {
        return _unwatchable;
    }

    protected var _configXML :XML;
    protected var _minSeats :int;
    protected var _maxSeats :int;
    protected var _gameType :int;
    protected var _unwatchable :Boolean;
}
}
