/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.jswiff.xml;

import com.jswiff.SWFDocument;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagConstants;
import com.jswiff.util.Base64;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.io.Reader;

import java.util.ArrayList;


/**
 * Converts an XML file to a SWF document.
 */
public class XMLReader {
  private SWFDocument swfDocument = new SWFDocument();
  private ArrayList tags          = new ArrayList();
  private SAXReader saxReader     = new SAXReader();
  {
    addHandlers();
  }

  /**
   * Creates a new XMLReader instance.
   *
   * @param stream XML source stream
   *
   * @throws DocumentException if the XML could not be parsed
   */
  public XMLReader(InputStream stream) throws DocumentException {
    saxReader.read(stream);
  }

  /**
   * Creates a new XMLReader instance.
   *
   * @param reader XML source reader
   *
   * @throws DocumentException if the XML could not be parsed
   */
  public XMLReader(Reader reader) throws DocumentException {
    saxReader.read(reader);
  }

  /**
   * Returns the SWF document generated from the parsed XML.
   *
   * @return SWF document
   */
  public SWFDocument getDocument() {
    swfDocument.addTags(tags);
    return swfDocument;
  }

  private void addHandlers() {
    HeaderHandler headerHandler = new HeaderHandler();
    saxReader.addHandler("/swfdocument/header", headerHandler);
    saxReader.addHandler(
      "/swfdocument/definebits", new TagHandler(TagConstants.DEFINE_BITS));
    saxReader.addHandler(
      "/swfdocument/definebitsjpeg2",
      new TagHandler(TagConstants.DEFINE_BITS_JPEG_2));
    saxReader.addHandler(
      "/swfdocument/definebitsjpeg3",
      new TagHandler(TagConstants.DEFINE_BITS_JPEG_3));
    saxReader.addHandler(
      "/swfdocument/definebitslossless",
      new TagHandler(TagConstants.DEFINE_BITS_LOSSLESS));
    saxReader.addHandler(
      "/swfdocument/definebitslossless2",
      new TagHandler(TagConstants.DEFINE_BITS_LOSSLESS_2));
    saxReader.addHandler(
      "/swfdocument/definebutton", new TagHandler(TagConstants.DEFINE_BUTTON));
    saxReader.addHandler(
      "/swfdocument/definebutton2", new TagHandler(
        TagConstants.DEFINE_BUTTON_2));
    saxReader.addHandler(
      "/swfdocument/definebuttoncxform",
      new TagHandler(TagConstants.DEFINE_BUTTON_C_XFORM));
    saxReader.addHandler(
      "/swfdocument/definebuttonsound",
      new TagHandler(TagConstants.DEFINE_BUTTON_SOUND));
    saxReader.addHandler(
      "/swfdocument/defineedittext",
      new TagHandler(TagConstants.DEFINE_EDIT_TEXT));
    saxReader.addHandler(
      "/swfdocument/definefont", new TagHandler(TagConstants.DEFINE_FONT));
    saxReader.addHandler(
      "/swfdocument/definefont2", new TagHandler(TagConstants.DEFINE_FONT_2));
    saxReader.addHandler(
      "/swfdocument/definefont3", new TagHandler(TagConstants.DEFINE_FONT_3));
    saxReader.addHandler(
      "/swfdocument/definefontinfo",
      new TagHandler(TagConstants.DEFINE_FONT_INFO));
    saxReader.addHandler(
      "/swfdocument/definefontalignment",
      new TagHandler(TagConstants.DEFINE_FONT_ALIGNMENT));
    saxReader.addHandler(
      "/swfdocument/definefontinfo2",
      new TagHandler(TagConstants.DEFINE_FONT_INFO_2));
    saxReader.addHandler(
      "/swfdocument/definemorphshape",
      new TagHandler(TagConstants.DEFINE_MORPH_SHAPE));
    saxReader.addHandler(
      "/swfdocument/definemorphshape2",
      new TagHandler(TagConstants.DEFINE_MORPH_SHAPE_2));
    saxReader.addHandler(
      "/swfdocument/defineshape", new TagHandler(TagConstants.DEFINE_SHAPE));
    saxReader.addHandler(
      "/swfdocument/defineshape2", new TagHandler(TagConstants.DEFINE_SHAPE_2));
    saxReader.addHandler(
      "/swfdocument/defineshape3", new TagHandler(TagConstants.DEFINE_SHAPE_3));
    saxReader.addHandler(
      "/swfdocument/defineshape4", new TagHandler(TagConstants.DEFINE_SHAPE_4));
    saxReader.addHandler(
      "/swfdocument/definesound", new TagHandler(TagConstants.DEFINE_SOUND));
    saxReader.addHandler(
      "/swfdocument/definesprite", new TagHandler(TagConstants.DEFINE_SPRITE));
    saxReader.addHandler(
      "/swfdocument/definetext", new TagHandler(TagConstants.DEFINE_TEXT));
    saxReader.addHandler(
      "/swfdocument/definetext2", new TagHandler(TagConstants.DEFINE_TEXT_2));
    saxReader.addHandler(
      "/swfdocument/definevideostream",
      new TagHandler(TagConstants.DEFINE_VIDEO_STREAM));
    saxReader.addHandler(
      "/swfdocument/doaction", new TagHandler(TagConstants.DO_ACTION));
    saxReader.addHandler(
      "/swfdocument/doinitaction", new TagHandler(TagConstants.DO_INIT_ACTION));
    saxReader.addHandler(
      "/swfdocument/enabledebugger",
      new TagHandler(TagConstants.ENABLE_DEBUGGER));
    saxReader.addHandler(
      "/swfdocument/enabledebugger",
      new TagHandler(TagConstants.ENABLE_DEBUGGER_2));
    saxReader.addHandler(
      "/swfdocument/exportassets", new TagHandler(TagConstants.EXPORT_ASSETS));
    saxReader.addHandler(
      "/swfdocument/flashtypesettings",
      new TagHandler(TagConstants.FLASHTYPE_SETTINGS));
    saxReader.addHandler(
      "/swfdocument/framelabel", new TagHandler(TagConstants.FRAME_LABEL));
    saxReader.addHandler(
      "/swfdocument/freecharacter", new TagHandler(TagConstants.FREE_CHARACTER));
    saxReader.addHandler(
      "/swfdocument/importassets", new TagHandler(TagConstants.IMPORT_ASSETS));
    saxReader.addHandler(
      "/swfdocument/importassets2", new TagHandler(
        TagConstants.IMPORT_ASSETS_2));
    saxReader.addHandler(
      "/swfdocument/jpegtables", new TagHandler(TagConstants.JPEG_TABLES));
    saxReader.addHandler(
      "/swfdocument/malformedtag", new TagHandler(TagConstants.MALFORMED));
    saxReader.addHandler(
      "/swfdocument/metadata", new TagHandler(TagConstants.METADATA));
    saxReader.addHandler(
      "/swfdocument/placeobject", new TagHandler(TagConstants.PLACE_OBJECT));
    saxReader.addHandler(
      "/swfdocument/placeobject2", new TagHandler(TagConstants.PLACE_OBJECT_2));
    saxReader.addHandler(
      "/swfdocument/placeobject3", new TagHandler(TagConstants.PLACE_OBJECT_3));
    saxReader.addHandler(
      "/swfdocument/protect", new TagHandler(TagConstants.PROTECT));
    saxReader.addHandler(
      "/swfdocument/removeobject", new TagHandler(TagConstants.REMOVE_OBJECT));
    saxReader.addHandler(
      "/swfdocument/removeobject2", new TagHandler(
        TagConstants.REMOVE_OBJECT_2));
    saxReader.addHandler(
      "/swfdocument/scale9grid", new TagHandler(TagConstants.SCALE_9_GRID));
    saxReader.addHandler(
      "/swfdocument/scriptlimits", new TagHandler(TagConstants.SCRIPT_LIMITS));
    saxReader.addHandler(
      "/swfdocument/settabindex", new TagHandler(TagConstants.SET_TAB_INDEX));
    saxReader.addHandler(
      "/swfdocument/showframe", new TagHandler(TagConstants.SHOW_FRAME));
    saxReader.addHandler(
      "/swfdocument/soundstreamblock",
      new TagHandler(TagConstants.SOUND_STREAM_BLOCK));
    saxReader.addHandler(
      "/swfdocument/soundstreamhead",
      new TagHandler(TagConstants.SOUND_STREAM_HEAD));
    saxReader.addHandler(
      "/swfdocument/soundstreamhead2",
      new TagHandler(TagConstants.SOUND_STREAM_HEAD_2));
    saxReader.addHandler(
      "/swfdocument/startsound", new TagHandler(TagConstants.START_SOUND));
    saxReader.addHandler(
      "/swfdocument/videoframe", new TagHandler(TagConstants.VIDEO_FRAME));
    saxReader.addHandler("/swfdocument/unknowntag", new UnknownTagHandler());
  }

  private void parseHeader(Element headerElement) {
    short swfVersion = RecordXMLReader.getShortAttribute(
        "swfversion", headerElement);
    swfDocument.setVersion(swfVersion);
    swfDocument.setCompressed(
      RecordXMLReader.getBooleanAttribute("compressed", headerElement));
    Element framesElement = RecordXMLReader.getElement("frames", headerElement);
    swfDocument.setFrameCount(
      RecordXMLReader.getIntAttribute("count", framesElement));
    swfDocument.setFrameRate(
      RecordXMLReader.getShortAttribute("rate", framesElement));
    Element sizeElement = RecordXMLReader.getElement("size", framesElement);
    swfDocument.setFrameSize(RecordXMLReader.readRect(sizeElement));
    Element backgroundColorElement = RecordXMLReader.getElement(
        "bgcolor", headerElement);
    swfDocument.setBackgroundColor(
      RecordXMLReader.readRGB(backgroundColorElement));
    if (swfVersion >= 8) {
      String access = RecordXMLReader.getStringAttribute(
          "access", headerElement);
      if (access.equals("local")) {
        swfDocument.setAccessMode(SWFDocument.ACCESS_MODE_LOCAL);
      } else if (access.equals("network")) {
        swfDocument.setAccessMode(SWFDocument.ACCESS_MODE_NETWORK);
      }
      Element metadata = headerElement.element("metadata");
      if (metadata != null) {
        swfDocument.setMetadata(Base64.decodeString(metadata.getText()));
      }
    }
  }

  private class TagHandler implements ElementHandler {
    private int tagCode;

    public TagHandler(int tagCode) {
      this.tagCode = tagCode;
    }

    public void onEnd(ElementPath path) {
      Element tagElement = path.getCurrent();
      Tag tag;
      tag = TagXMLReader.readTag(tagElement, tagCode);
      tags.add(tag);
      tagElement.detach(); // prune element from tree
      tagElement = null;
    }

    public void onStart(ElementPath path) {
    }
  }

  private class UnknownTagHandler implements ElementHandler {
    public void onEnd(ElementPath path) {
      Element tagElement = path.getCurrent();
      Tag tag            = TagXMLReader.readUnknownTag(tagElement);
      tags.add(tag);
      tagElement.detach(); // prune element from tree
    }

    public void onStart(ElementPath path) {
    }
  }

  private class HeaderHandler implements ElementHandler {
    public void onEnd(ElementPath path) {
      Element headerElement = path.getCurrent();
      parseHeader(headerElement);
      headerElement.detach();
      headerElement = null;
    }

    public void onStart(ElementPath path) {
    }
  }
}
