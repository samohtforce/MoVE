package de.thm.move.loader

import javafx.scene.paint.Color

import de.thm.move.models.LinePattern
import de.thm.move.views.shapes._
import org.junit.Assert._
import org.junit.Test

import de.thm.move.loader.parser.PropertyParser._
import de.thm.move.loader.parser.ast._

class ConverterTest {

  @Test
  def convertCoordinationSystem: Unit = {

    val extent = ( ((-100.0),(-50.0)),((100.0),(50.0)) )
    val ast = Model("ölkj",
    List(Icon(Some(CoordinateSystem(extent)), List())
    ))
    val conv = new ShapeConverter(4, ShapeConverter.gettCoordinateSystemSizes(ast).head)

    val exp = (200,100)
    assertEquals(List(exp), ShapeConverter.gettCoordinateSystemSizes(ast) )

    val ast2 = Model("ög",
      List.fill(100)(Icon(Some(CoordinateSystem(extent)), List()))
    )

    ShapeConverter.gettCoordinateSystemSizes(ast).foreach( x =>
        assertEquals(exp, x)
    )

    val extent2 = ( ((-100.0),(-50.0)),((0.0),(-20.0)) )
    val ast3 = Model("ölkj",
    List(Icon(Some(CoordinateSystem(extent2)), List())
    ))
    val exp2 = (100.0, 30)
    assertEquals(List(exp2), ShapeConverter.gettCoordinateSystemSizes(ast3) )
  }

  @Test
  def convertLine:Unit = {
    //without origin
    val extent = ( ((0.0),(0.0)),((100.0),(200.0)) )
    val ast = Model("ölkj",
    List(Icon(Some(CoordinateSystem(extent)),
      List(
          PathElement(
            GraphicItem(),
            List( (10.0,10.0),(50.0,30.0) ),
            Color.BLACK,
            1.0,
            "LinePattern.Dash"
            )
        )
    )
    ))

    val conv = new ShapeConverter(1, ShapeConverter.gettCoordinateSystemSizes(ast).head)

    val convertedLine = conv.getShapes(ast).head.asInstanceOf[ResizableLine]
    val startAnchor = convertedLine.getAnchors.head
    val endAnchor = convertedLine.getAnchors.tail.head

    assertEquals( 10.0, startAnchor.getCenterX, 0.01 )
    assertEquals( 200.0-10.0, startAnchor.getCenterY, 0.01 )
    assertEquals( 50.0, endAnchor.getCenterX, 0.01 )
    assertEquals( 200.0-30.0, endAnchor.getCenterY, 0.01 )
    assertEquals( Color.BLACK, convertedLine.getStrokeColor )
    assertEquals( 1.0, convertedLine.getStrokeWidth, 0.01 )
    assertEquals( LinePattern.Dash, convertedLine.linePattern.get )


    val points2 = List( (10.0,10.0),(50.0,30.0), (60.0,80.0),(30.0,30.0) )
    val ast2 = Model("ölkj",
      List(Icon(Some(CoordinateSystem(extent)),
        List(
          PathElement(
            GraphicItem(),
            points2,
            Color.BLACK,
            1.0,
            "LinePattern.Dash"
          )
        )
      )
      ))

    val expectedPoints = points2.map {
      case (x,y) => (x, 200-y)
    }
    val convPath = conv.getShapes(ast2).head.asInstanceOf[ResizablePath]
    convPath.getAnchors.zip(expectedPoints).foreach {
      case (p1,p2) => assertEquals((p1.getCenterX,p1.getCenterY),p2)
    }
  }
}