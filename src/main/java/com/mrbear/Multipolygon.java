package com.mrbear;

import java.util.List;

public record Multipolygon(List<Polygon> polygons) implements Geometry {
}
