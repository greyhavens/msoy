//
// $Id: $

package com.threerings.msoy.web.server;

import java.text.SimpleDateFormat;
import java.text.DateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

public class CloudfrontEventReader
{
    public CloudfrontEventReader (XMLEventReader reader)
    {
        _reader = reader;
    }

    public XMLEvent peek ()
        throws XMLStreamException
    {
        return _reader.peek();
    }

    public void expectType (int eventType)
        throws XMLStreamException
    {
        XMLEvent event = _reader.nextEvent();
        if (event.getEventType() != eventType) {
            throw new XMLStreamException("Expecting event type [" + eventType + "], got " + event);
        }
    }

    public void expectElementStart (String elementName)
        throws XMLStreamException
    {
        XMLEvent event = _reader.nextEvent();
        if ((event instanceof StartElement) &&
            ((StartElement) event).getName().getLocalPart().equals(elementName)) {
            // log.info("Expected and found element: " + elementName);
            return;
        }
        throw new XMLStreamException("Expecting start of element [" + elementName + "], got " + event);
    }

    public void expectElementEnd (String elementName)
        throws XMLStreamException
    {
        XMLEvent event = _reader.nextEvent();
        if ((event instanceof EndElement) &&
            ((EndElement) event).getName().getLocalPart().equals(elementName)) {
            return;
        }
        throw new XMLStreamException("Expecting end of element [" + elementName + "], got " + event);
    }

    public boolean peekForElement (String element)
        throws XMLStreamException
    {
        XMLEvent event = _reader.peek();
        return ((event instanceof StartElement) &&
                element.equals(((StartElement) event).getName().getLocalPart()));
    }

    public boolean maybeSkip (String... elements)
        throws XMLStreamException
    {
        XMLEvent event = _reader.peek();
        if (event instanceof StartElement) {
            String name = ((StartElement) event).getName().getLocalPart();
            if (Arrays.asList(elements).contains(name)) {
                expectElementStart(name);
                if (_reader.peek() instanceof Characters) {
                    _reader.nextEvent();
                }
                expectElementEnd(name);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns null if the specified element is not in fact the next thing in front of our cursor;
     * returns empty string for elements that match, but which are empty.
     */
    public String maybeString (String element)
        throws XMLStreamException
    {
        if (!peekForElement(element)) {
            return null;
        }
        _reader.nextEvent();

        String result;
        XMLEvent event = _reader.peek();
        if (event instanceof Characters) {
            result = ((Characters) event).getData();
            event = _reader.nextEvent();
        } else {
            result = "";
        }
        expectElementEnd(element);
        // log.info("Returning character content: " + result);
        return result;
    }

    /**
     * Returns null if the specified element is not in fact the next thing in front of our cursor;
     * returns the specified default integer for elements that match, but which are empty.
     */
    public Integer maybeInt (String element)
        throws XMLStreamException
    {
        String stringResult = maybeString(element);
        if (stringResult != null) {
            if (stringResult.length() == 0) {
                throw new XMLStreamException("Can't handle empty integere elements.");
            }
            return Integer.valueOf(stringResult);
        }
        return null;
    }

    /**
     * Returns null if the specified element is not in fact the next thing in front of our cursor;
     * there is no default, and thus we throw an error if the element exists but is empty.
     */
    public Date maybeDate (String element)
        throws XMLStreamException
    {
        String stringResult = maybeString(element);
        if (stringResult != null) {
            if (stringResult.length() == 0) {
                throw new XMLStreamException("Can't handle empty date elements.");
            }
            try {
                return RFC8601_DATE_FORMAT.parse(stringResult);
            } catch (Exception e) {
                throw new XMLStreamException("Failed to parse date [" + stringResult + "]", e);
            }
        }
        return null;
    }

    /**
     * Returns null if the specified element is not in fact the next thing in front of our cursor;
     * returns the specified default integer for elements that match, but which are empty.
     */
    public Boolean maybeBoolean (String element)
        throws XMLStreamException
    {
        String stringResult = maybeString(element);
        if (stringResult != null) {
            if (stringResult.length() == 0) {
                throw new XMLStreamException("Can't handle empty boolean elements.");
            }
            return Boolean.valueOf(stringResult);
        }
        return null;
    }

    protected XMLEventReader _reader;

    protected static final DateFormat RFC8601_DATE_FORMAT;
    static {
        RFC8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        RFC8601_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}

