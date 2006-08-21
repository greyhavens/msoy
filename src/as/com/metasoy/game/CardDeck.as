package com.metasoy.game {

/**
 * A simple card deck that encodes cards as a string like "Ac" for the
 * ace of clubs, or "Td" for the 10 of diamonds.
 */
public class CardDeck
{
    public function CardDeck (gameObj :GameObject, deckName :String = "deck")
    {
        _gameObj = gameObj;
        _deckName = deckName;

        var deck :Array = new Array();
        for each (var rank :String in ["2", "3", "4", "5", "6", "7", "8",
                "9", "T", "J", "Q", "K", "A"]) {
            for each (var suit :String in ["c", "d", "h", "s"]) {
                deck.push(rank + suit);
            }
        }

        _gameObj.setCollection(_deckName, deck);
    }

    public function dealToPlayer (
        playerIdx :int, count :int, msgName :String) :void
    {
        // TODO: support the callback
        _gameObj.dealFromCollection(_deckName, count, msgName, null, playerIdx);
    }

    public function dealToData (count :int, propName :String) :void
    {
        _gameObj.dealFromCollection(_deckName, count, propName, null);
    }

    /** The game object. */
    protected var _gameObj :GameObject;

    /** The name of our deck. */
    protected var _deckName :String;
}
}
