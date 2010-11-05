//
// $Id: $


package com.threerings.msoy.web.server;

import java.util.Date;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

/**
 *
 */
public class CloudfrontEventWriter
{
    public CloudfrontEventWriter (XMLEventWriter writer)
    {
        _writer = writer;
    }

    public void startDocument ()
        throws XMLStreamException
    {
        _writer.add(_factory.createStartDocument());
    }

    public void endDocument ()
        throws XMLStreamException
    {
        _writer.add(_factory.createEndDocument());
    }

    public void startElement (String elementName)
        throws XMLStreamException
    {
        _writer.add(_factory.createStartElement(new QName(elementName), null, null));
    }

    public void endElement (String elementName)
        throws XMLStreamException
    {
        _writer.add(_factory.createEndElement(new QName(elementName), null));
    }

    public void writeString (String element, String str)
        throws XMLStreamException
    {
        startElement(element);
        _writer.add(_factory.createCharacters(str));
        endElement(element);
    }

    public void writeBoolean (String element, boolean bool)
        throws XMLStreamException
    {
        writeString(element, Boolean.toString(bool));
    }

    public void writeInt (String element, int bool)
        throws XMLStreamException
    {
        writeString(element, Integer.toString(bool));
    }

    public void writeDate (String element, Date date)
        throws XMLStreamException
    {
        writeString(element, CloudfrontEventReader.RFC8601_DATE_FORMAT.format(date));
    }

    protected final XMLEventFactory _factory = XMLEventFactory.newInstance();
    protected final XMLEventWriter _writer;
}
