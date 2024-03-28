package com.mrbear;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {

  public static final float CANVAS_WIDTH = 900.0f;
  public static final float CANVAS_HEIGHT = 900.0f;
  public static final float CANVAS_MARGINX = 71.0f;
  public static final float CANVAS_MARGINY = 71.0f;
  public static final int POINT_DIAMETER = 6;
  public static final int HALF_POINT_DIAMETER = POINT_DIAMETER / 3;

  private GraphicsContext gc;

  private double minx = 111111100;
  private double miny = 111111100;
  private double maxx = 0;
  private double maxy = 0;
  private boolean toggleCoordinates = true;
  private boolean toggleDots = true;

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("Geoviewer");

    var javaVersion = SystemInfo.javaVersion();
    var javafxVersion = SystemInfo.javafxVersion();

    GridPane root = new GridPane();
    root.setAlignment(Pos.CENTER);
    root.setHgap(10);
    root.setVgap(10);
    root.setPadding(new Insets(25, 25, 25, 25));

    var label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
    root.add(label, 0, 0, 2, 1);

    var label2 = new Label("WKT:");
    root.add(label2, 0, 1, 2, 1);

    TextArea wkt = new TextArea();
    root.add(wkt, 0, 2, 2, 5);

    Button add = new Button("Add");
    add.setOnAction(e -> add(e, wkt.getParagraphs()));
    root.add(add, 0, 7, 1, 1);

    Button clear = new Button("Clear");
    clear.setOnAction(this::clear);
    root.add(clear, 1, 7, 1, 1);

    ToggleButton showCoordinates = new ToggleButton("Show coordinates");
    showCoordinates.setSelected(toggleCoordinates);
    showCoordinates.setOnAction(this::toggleCoordinates);
    root.add(showCoordinates, 0, 8, 1, 1);

    ToggleButton showDots = new ToggleButton("Show dots");
    showDots.setSelected(toggleDots);
    showDots.setOnAction(this::toggleDots);
    root.add(showDots, 0, 9, 1, 1);

    var label3 = new Label("Draws in sequence BLUE, RED, YELLOW, GREEN, CYAN, MAGENTA");
    root.add(label3, 0, 10, 2, 1);

    root.add(getCanvas(), 2, 0, 20, 20);

    var scene = new Scene(root, 640, 480);
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private void toggleCoordinates(ActionEvent actionEvent) {
    toggleCoordinates = !toggleCoordinates;
  }

  private void toggleDots(ActionEvent actionEvent) {
    toggleDots = !toggleDots;
  }

  private Canvas getCanvas() {
    Canvas canvas = new Canvas(CANVAS_WIDTH + CANVAS_MARGINX * 2, CANVAS_HEIGHT + CANVAS_MARGINY * 2);
    // graphics context
    gc = canvas.getGraphicsContext2D();
    gc.setTextAlign(TextAlignment.CENTER);
    gc.setTextBaseline(VPos.TOP);

    return canvas;
  }

  private void clear(ActionEvent e) {
    gc.clearRect(0, 0, CANVAS_WIDTH + CANVAS_MARGINX * 2, CANVAS_HEIGHT + CANVAS_MARGINY * 2);
    minx = 111111100;
    miny = 111111100;
    maxx = 0;
    maxy = 0;
  }

  private void add(ActionEvent e, ObservableList<CharSequence> content) {
    List<Geometry> polygons = content.stream()
        .map(CharSequence::toString)
        .map(String::trim)
        .map(this::createGeometry)
        .filter(Objects::nonNull)
        .toList();
    computeMaxMin(polygons);
    List<Color> list = Arrays.asList(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA);
    var index = 0;
    for (Geometry polygon : polygons) {
      draw(polygon, list.get(index));
      index = (index + 1) % list.size();
    }

    //    // set fill for rectangle
    //    gc.setFill(Color.RED);
    //    gc.fillRect(20, 20, 70, 70);
    //
    //    // set fill for oval
    //    gc.setFill(Color.BLUE);
    //    gc.fillOval(30, 30, 70, 70);

  }

  private void computeMaxMin(List<Geometry> polygons) {
    for (Geometry polygon : polygons) {
      List<Coordinate> coordinates = Arrays.asList(polygon.getCoordinates());
      for (Coordinate coordinate : coordinates) {
        if (minx > coordinate.x) {
          minx = coordinate.x;
        }
        if (miny > coordinate.y) {
          miny = coordinate.y;
        }
        if (maxx < coordinate.x) {
          maxx = coordinate.x;
        }
        if (maxy < coordinate.y) {
          maxy = coordinate.y;
        }
      }

    }
  }

  private void draw(Geometry geometry, Color color) {
    if (geometry instanceof MultiPolygon) {
      var multiPolygon = (MultiPolygon) geometry;
      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        draw(multiPolygon.getGeometryN(i), color);
      }
    }
    if (geometry instanceof Polygon) {
      var polygon = (Polygon) geometry;
      draw(polygon.getExteriorRing(), color);
      for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
        draw(polygon.getInteriorRingN(i), color);
      }
    }
    if (geometry instanceof LinearRing) {
      var linearRing = (LinearRing) geometry;
      Coordinate lastCoordinate = null;
      for (Coordinate coordinate : linearRing.getCoordinates()) {
        if (lastCoordinate != null) {
          gc.setStroke(color);
          gc.setFill(Color.RED);
          // 0 .. canvas_width
          // 0 .. 200
          // minx      x          maxx
          // 101096    101100     421000

          var x1 = ((lastCoordinate.x - minx) / (maxx - minx)) * CANVAS_WIDTH;
          var y1 = ((lastCoordinate.y - miny) / (maxy - miny)) * CANVAS_HEIGHT;
          var x2 = ((coordinate.x - minx) / (maxx - minx)) * CANVAS_WIDTH;
          var y2 = ((coordinate.y - miny) / (maxy - miny)) * CANVAS_HEIGHT;
          System.out.println(x1 + "," + y1 + "->" + x2 + "," + y2);
          gc.strokeLine(x1 + CANVAS_MARGINX, CANVAS_HEIGHT - (y1),
              x2 + CANVAS_MARGINX, CANVAS_HEIGHT - (y2));
          // set fill for oval
          gc.setFill(Color.BLACK);
          if (toggleDots) {
            gc.fillOval(x2 + CANVAS_MARGINX - HALF_POINT_DIAMETER,
                CANVAS_HEIGHT - (y2 - HALF_POINT_DIAMETER),
                POINT_DIAMETER, POINT_DIAMETER);
          }
          if (toggleCoordinates) {
            gc.fillText(coordinate.x + ", " + coordinate.y,
                x2 + CANVAS_MARGINX - HALF_POINT_DIAMETER,
                CANVAS_HEIGHT - (y2 - HALF_POINT_DIAMETER + 6));
          }
        }
        lastCoordinate = coordinate;
      }
    }
    if (geometry instanceof Point) {
      var point = (Point) geometry;
      Coordinate coordinate = point.getCoordinate();
      gc.setStroke(color);
      gc.setFill(Color.RED);
      // 0 .. canvas_width
      // 0 .. 200
      // minx      x          maxx
      // 101096    101100     421000

      var x = ((coordinate.x - minx) / (maxx - minx)) * CANVAS_WIDTH;
      var y = ((coordinate.y - miny) / (maxy - miny)) * CANVAS_HEIGHT;
      // set fill for oval
      gc.setFill(Color.BLACK);
      gc.fillOval(x + CANVAS_MARGINX - HALF_POINT_DIAMETER, CANVAS_HEIGHT - (y - HALF_POINT_DIAMETER),
          POINT_DIAMETER,
          POINT_DIAMETER);
      gc.fillText(coordinate.x + ", " + coordinate.y,
          x + CANVAS_MARGINX - HALF_POINT_DIAMETER,
          CANVAS_HEIGHT - (y - HALF_POINT_DIAMETER + 6));
    }
  }

  private Geometry createGeometry(String x) {
    if (x.startsWith("--")) {
      System.out.println("COMMENTS detected");
      return null;
    }
    try {
      return GeometryParser.createGeometryFromWKT(x);
    }
    catch (ParseException e) {
      e.printStackTrace();
      return null;
    }

  }

  public static void main(String[] args) {
    launch();
  }

}