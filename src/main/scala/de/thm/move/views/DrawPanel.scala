/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.move.views

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.{ObservableValue}
import javafx.event.{EventHandler}
import javafx.scene.input.KeyEvent;
import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape._
import javafx.scene.image.{ImageView, Image}
import javafx.scene.input.{InputEvent}
import scala.collection.JavaConverters._
import de.thm.move.views.shapes._
import de.thm.move.models.CommonTypes._

/** The main-panel which holds all drawn shapes */
class DrawPanel() extends Pane {
  private var shapes = List[Node]()

  getStyleClass.add("draw-pane")

  def drawShape[T <: Node](n:T):Unit = {
    super.getChildren.add(n)

    shapes = n :: shapes
  }

  def remove[A <: Node](shape:A): Unit = {
    shapes = shapes.filterNot(_ == shape)
    shape match {
      case rs:ResizableShape =>
        getChildren.removeAll(rs.getAnchors:_*)
        getChildren.remove(shape)
      case _ => getChildren.remove(shape)
    }
  }

  def removeWhile(pred: Node => Boolean): Unit = {
    val removingShapes = shapes.takeWhile(pred)
    shapes = shapes.dropWhile(pred)

    getChildren.removeAll(removingShapes:_*)
  }


  def removeWhileIdx(pred: (Node, Int) => Boolean): Unit = {
    val shapeWithidx = shapes.zipWithIndex
    val removingShapes = shapeWithidx.takeWhile {
      case (n, idx) => pred(n,idx)
    }.map(_._1)

    shapes = shapeWithidx.dropWhile  {
      case (n, idx) => pred(n,idx)
    }.map(_._1)

    getChildren.removeAll(removingShapes:_*)
  }

  def setSize(p:Point): Unit = {
    val (x,y) = p
    setPrefSize(x, y)
    setMinSize(x, y)
    setMaxSize(x, y)
  }

  def setSize(w:Double,h:Double):Unit = setSize((w,h))

  /** Returns all shapes that should be '''converted into modelica'''-source code.
    *
    * This method '''doesn't include all''' shapes, use getChildren for getting all nodes.
    */
  def getShapes: List[Node] = getChildren.asScala.flatMap {
    case x:SelectionGroup => x.childrens
    case (_:Anchor | _:SelectionRectangle) => Nil
    case x:Node if x.getId == DrawPanel.tmpShapeId => Nil
    case x:Node => List(x)
  }.toList
}
object DrawPanel {
  /** Identifies temporary shapes. These shapes shouldn't get included into the final Modelica-code. */
  val tmpShapeId = "temporary-shape"
}
