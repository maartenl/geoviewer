package com.mrbear;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JavaFX App
 */
public class App extends Application {

  public static final float CANVAS_WIDTH = 900.0f;
  public static final float CANVAS_HEIGHT = 900.0f;
  public static final float CANVAS_MARGINX = 11.0f;
  public static final float CANVAS_MARGINY = 11.0f;

  private GraphicsContext gc;

  private float minx = 111111100;
  private float miny = 111111100;
  private float maxx = 0;
  private float maxy = 0;

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

    root.add(getCanvas(), 2, 0, 20, 20);

    var scene = new Scene(root, 640, 480);
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private Canvas getCanvas() {
    Canvas canvas = new Canvas(CANVAS_WIDTH + CANVAS_MARGINX * 2, CANVAS_HEIGHT + CANVAS_MARGINY * 2);
    // graphics context
    gc = canvas.getGraphicsContext2D();

    return canvas;
  }

  private void clear(ActionEvent e) {
    gc.clearRect(0, 0, CANVAS_WIDTH + CANVAS_MARGINX * 2, CANVAS_HEIGHT + CANVAS_MARGINY * 2);
  }

  private void add(ActionEvent e, ObservableList<CharSequence> content) {
    List<Geometry> polygons = content.stream()
        .map(CharSequence::toString)
        .map(String::trim)
        .map(this::createGeometry)
        .filter(Objects::nonNull)
        .toList();
    computeMaxMin(polygons);
    List<Color> list = Arrays.asList(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN);
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
      List<Coordinate> coordinates  = polygon instanceof Polygon ? ((Polygon) polygon).coordinates() : Collections.emptyList();
      for (Coordinate coordinate : coordinates) {
        if (minx > coordinate.x()) {
          minx = coordinate.x();
        }
        if (miny > coordinate.y()) {
          miny = coordinate.y();
        }
        if (maxx < coordinate.x()) {
          maxx = coordinate.x();
        }
        if (maxy < coordinate.y()) {
          maxy = coordinate.y();
        }
      }

    }
    //    minx -= 100;
    //    miny -= 100;
    //    maxx += 100;
    //    maxy += 100;
  }

  private void draw(Geometry geometry, Color color) {
    if (geometry instanceof Polygon) {
      var polygon = (Polygon) geometry;
      Coordinate lastCoordinate = null;
      for (Coordinate coordinate : polygon.coordinates()) {
        if (lastCoordinate != null) {
          gc.setStroke(color);
          gc.setFill(Color.RED);
          // 0 .. canvas_width
          // 0 .. 200
          // minx      x          maxx
          // 101096    101100     421000

          var x1 = ((lastCoordinate.x() - minx) / (maxx - minx)) * CANVAS_WIDTH;
          var y1 = ((lastCoordinate.y() - miny) / (maxy - miny)) * CANVAS_HEIGHT;
          var x2 = ((coordinate.x() - minx) / (maxx - minx)) * CANVAS_WIDTH;
          var y2 = ((coordinate.y() - miny) / (maxy - miny)) * CANVAS_HEIGHT;
          System.out.println(x1 + "," + y1 + "->" + x2 + "," + y2);
          gc.strokeLine(x1 + CANVAS_MARGINX, y1 + CANVAS_MARGINY, x2 + CANVAS_MARGINX, y2 + CANVAS_MARGINY);
          // set fill for oval
          gc.setFill(Color.BLACK);
          gc.fillOval(x2 + CANVAS_MARGINX - 3, y2 + CANVAS_MARGINY - 3, 6, 6);
        }
        lastCoordinate = coordinate;
      }
    }
  }

  private Geometry createGeometry(String x) {
    if (x.startsWith("--")) {
      System.out.println("COMMENTS detected");
      return null;
    }
    if (x.startsWith("POLYGON ((")) {
      System.out.println("POLYGON detected");
      var cleaned = x.replace("POLYGON", "").replace("(", "").replace(")", "").replace(", ", ",").split(",");
      System.out.println(cleaned);
      return getPolygon(cleaned);
    }
    return null;
  }

  private static Polygon getPolygon(String[] cleaned) {
    List<Coordinate> coordinates = new ArrayList<>();
    Arrays.stream(cleaned).forEach(coor -> {
      System.out.println(coor.trim());
      var sep = coor.trim().split(" ");
      if (sep != null && sep[0] != null && sep[1] != null) {
        coordinates.add(new Coordinate(Float.parseFloat(sep[0]), Float.parseFloat(sep[1])));
      }
    });
    return new Polygon(coordinates);
  }

  public static void main(String[] args) {
    launch();
  }

}