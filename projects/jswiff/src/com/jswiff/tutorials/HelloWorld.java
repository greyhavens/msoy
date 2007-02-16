package com.jswiff.tutorials;

import com.jswiff.SWFDocument;
import com.jswiff.SWFWriter;
import com.jswiff.swfrecords.Matrix;
import com.jswiff.swfrecords.RGBA;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.tags.DefineEditText;
import com.jswiff.swfrecords.tags.DefineFont2;
import com.jswiff.swfrecords.tags.PlaceObject2;
import com.jswiff.swfrecords.tags.ShowFrame;

import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Simple tutorial class showing how you can output some text in Flash.
 */
public class HelloWorld {
  /**
   * Main method. Calls other methods to create a SWF containing some colored
   * text.
   *
   * @param args command-line arguments are ignored
   */
  public static void main(String[] args) {
    String fileName      = "helloworld.swf";
    SWFDocument document = createDocument();
    try {
      writeDocument(document, fileName);
    } catch (IOException e) {
      System.out.println("An error occured while writing " + fileName + ":");
      e.printStackTrace();
    }
  }

  private static SWFDocument createDocument() {
    // create a new SWF document
    SWFDocument document    = new SWFDocument();

    // first we define a font for the text
    // get a character ID for the font
    int fontId              = document.getNewCharacterId();

    // use a standard font (e.g. Arial), we don't want to define shapes for each glyph
    DefineFont2 defineFont2 = new DefineFont2(fontId, "Arial", null, null);
    document.addTag(defineFont2);
    // get a character ID for our text
    int textId                    = document.getNewCharacterId();

    // dynamic text is a good way to go, we use DefineEditText for this
    // we don't care about bounds and variables
    DefineEditText defineEditText = new DefineEditText(
        textId, new Rect(0, 0, 0, 0), null);

    // we have set the text bounds to a zero rectangle;
    // to see the whole text, we set the autosize flag
    defineEditText.setAutoSize(true);
    // assign the font defined above to the text, set font size to 24 px (in twips!)
    defineEditText.setFont(fontId, 20 * 24);
    // set text color to red
    defineEditText.setTextColor(new RGBA(255, 0, 0, 255));
    // don't let viewers mess around with our text
    defineEditText.setReadOnly(true);
    // finally set the text
    defineEditText.setInitialText("Hello world!");
    document.addTag(defineEditText);
    // place our text at depth 1
    PlaceObject2 placeObject2 = new PlaceObject2(1);
    placeObject2.setCharacterId(textId);
    // place text at position (45; 10) (in twips!)
    placeObject2.setMatrix(new Matrix(20 * 45, 20 * 10));
    document.addTag(placeObject2); // place text
    document.addTag(new ShowFrame()); // show frame
    return document;
  }

  private static void writeDocument(SWFDocument document, String fileName)
    throws IOException {
    SWFWriter writer = new SWFWriter(document, new FileOutputStream(fileName));
    writer.write();
  }
}
