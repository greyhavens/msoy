package com.facebook.api;

import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

public class PhotoTag {
  private double _x;
  private double _y;
  private Integer _taggedUserId;
  private String _text;

  public PhotoTag(String text, double x, double y) {
    assert (null != text && !"".equals(text));
    this._text = text;
    this._taggedUserId = null;
    this.setCoordinates(x, y);
  }

  public PhotoTag(int taggedUserId, double x, double y) {
    assert (0 < taggedUserId);
    this._text = null;
    this._taggedUserId = taggedUserId;
    this.setCoordinates(x, y);
  }

  private void setCoordinates(double x, double y) {
    assert (0.0 <= x && x <= 00.0);
    assert (0.0 <= y && y <= 100.0);
    this._x = x;
    this._y = y;
  }

  public boolean hasTaggedUser() {
    return null != this._taggedUserId;
  }

  public double getX() {
    return this._x;
  }

  public double getY() {
    return this._y;
  }

  public String getText() {
    return this._text;
  }

  public Integer getTaggedUserId() {
    return this._taggedUserId;
  }

  public JSONWriter jsonify(JSONWriter writer)
    throws JSONException {
    JSONWriter ret = (null == writer ? new JSONStringer() : writer)
      .object()
      .key("x").value(Double.toString(getX()))
      .key("y").value(Double.toString(getY()));

    return (hasTaggedUser()) ? ret.key("tag_uid").value(getTaggedUserId()).endObject() :
           ret.key("tag_text").value(getText()).endObject();
  }
}
