package org.mrbear;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeometryParser {
  public static final int SRID = 28992;

  private static final GeometryFactory GEOMETRYFACTORY = new GeometryFactory(new PrecisionModel(), SRID);

  /**
   * Create a geometry object from a String in Well-known text (WKT) format.
   * @see <a href="http://en.wikipedia.org/wiki/Well-known_text">WTK in wikipedia</a>
   * @param wkt String containing geometry in WKT format.
   * @return Geometry
   */
  public static Geometry createGeometryFromWKT(String wkt) throws ParseException {
    Geometry geometry = null;
    if (wkt != null && !wkt.isBlank()) {
      geometry = new WKTReader(GEOMETRYFACTORY).read(wkt);
    }
    return geometry;
  }
}
