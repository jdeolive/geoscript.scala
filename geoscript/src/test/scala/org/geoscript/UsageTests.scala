package org.geoscript

import org.opengis.feature.simple.SimpleFeature

import org.specs._

import geometry._
import projection._

class UsageTests extends Specification with GeoScript {
  "geometries" should { 
    "work like on the geoscript homepage" in { 
      var p = Point(-111, 45.7) in Projection("epsg:4326")
      var p2 = p in Projection("epsg:26912")
      var poly = p.buffer(100)

      p2.x must beCloseTo(499999.0, 0.5)
      p2.y must beCloseTo(5060716.0, 0.5)
      poly.area must beCloseTo(31214.45, 0.01)
    }

    "linestrings should be easy" in { 
      LineString((10.0, 10.0), (20.0, 20.0), (30.0, 40.0))
        .length must beCloseTo(36.503, 0.001)

      LineString((10, 10), (20.0, 20.0), (30, 40)) 
        .length must beCloseTo(36.503, 0.001)
    }

    "polygon should be easy" in { 
      Polygon(
        LineString((10, 10), (10, 20), (20, 20), (20, 15), (10, 10)),
        Seq.empty
      ).area must_== 75
    }

    "multi point should be easy" in {
      MultiPoint(Seq((20, 20), (10.0, 10.0))) 
    } 
  }  

  "Layers" should {
    val statesPath = "geoscript/src/test/resources/data/states.shp"
    "be able to read shapefiles" in {
      val shp = layer.Shapefile(statesPath)
      val (xMin, yMin, xMax, yMax, proj) = shp.bounds

      shp.name must_== "states"
      shp.count must_== 49

      xMin must beCloseTo (-124.731422, 1d)
      yMin must beCloseTo (24.955967, 1d)
      xMax must beCloseTo (-66.969849, 1d)
      yMax must beCloseTo (49.371735, 1d)
      proj must_== "EPSG:4326"
    }

    "support search" in {
      val shp = layer.Shapefile(statesPath)
      shp.features.find(_.id == "states.1") must beSome[feature.Feature]
    }

    "provide access to schema information" in {
      val shp = layer.Shapefile(statesPath)
      shp.schema.name must_== "states"
      val field = shp.schema.get("STATE_NAME")
      field.name must_== "STATE_NAME"
      // the type of field.binding is hard to spell,
      // just compare toString's for now
      field.binding.getName must_== classOf[java.lang.String].getName
    }

    "provide access to the containing workspace" in {
      val shp = layer.Shapefile(statesPath)
      shp.workspace must haveSuperClass[workspace.Workspace]
    }
  }

  "Workspaces" should {
    "provide a listing of layers" in {
      val mem = workspace.Memory()
      mem.names must beEmpty
    }

    "allow creating new layers" in {
      val mem = workspace.Memory()
      mem.names must beEmpty
      var dummy = mem.create("dummy", 
        feature.Field("name", classOf[String]),
        feature.Field("geom", classOf[com.vividsolutions.jts.geom.Geometry])
      )
      mem.names.length must_== 1

      dummy += feature.Feature(
        "name" -> "San Francisco",
        "geom" -> Point(37.78, -122.42)
      )

      dummy += feature.Feature(
        "name" -> "New York",
        "geom" -> Point(40.47, -73.58)
      )

      dummy.count must_== 2
      
      dummy.features.find(
        f => f.get[String]("name") == "New York"
      ) must beSome[feature.Feature]
    }
  }
}
