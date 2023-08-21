package com.mrbear;

import java.util.List;

public record Polygon(List<Coordinate> coordinates) implements Geometry {
}
