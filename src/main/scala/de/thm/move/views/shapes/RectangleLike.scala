/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.move.views.shapes

import javafx.geometry.Bounds
import javafx.scene.input.MouseEvent
import javafx.scene.shape.Ellipse

import de.thm.move.Global
import de.thm.move.controllers.implicits.FxHandlerImplicits._
import de.thm.move.util.JFxUtils._
import de.thm.move.util.PointUtils._
import de.thm.move.history.History
import de.thm.move.history.History.Command
import de.thm.move.models.CommonTypes._
import de.thm.move.views.Anchor
import de.thm.move.Global._

/** Represents shapes with rectangular boundings.
  * For Example Rectangles, Circles, Images
  */
trait RectangleLike {
  self: ResizableShape =>

  protected val adjustCoordinates:Boolean = true

  //resize anchors at edges
  val topLeftAnchor = new Anchor(getTopLeft)
  val topRightAnchor = new Anchor(getTopRight)
  val bottomLeftAnchor = new Anchor(getBottomLeft)
  val bottomRightAnchor = new Anchor(getBottomRight)
  override val getAnchors: List[Anchor] =
    List(topLeftAnchor, topRightAnchor, bottomLeftAnchor, bottomRightAnchor)

  /** Gets the untransformed upper-left point */
  def getTopLeft:Point = (getBoundsInLocal.getMinX, getBoundsInLocal.getMinY)
  /** Gets the untransformed upper-right point */
  def getTopRight:Point = (getBoundsInLocal.getMaxX, getBoundsInLocal.getMinY)
  /** Gets the untransformed bottom-left point */
  def getBottomLeft:Point = (getBoundsInLocal.getMinX, getBoundsInLocal.getMaxY)
  /** Gets the untransformed bottom-right point */
  def getBottomRight:Point = (getBoundsInLocal.getMaxX,getBoundsInLocal.getMaxY)

  /** Transforms the point src from local into parent's coordinate space */
  private def transformedPoint(src:Point):Point = {
    val (x,y) = src
    val point2D = localToParent(x,y)
    (point2D.getX, point2D.getY)
  }

  /** Gets the transformed upper-left point */
  def getTransformedTopLeft:Point = transformedPoint(getTopLeft)
  /** Gets the transformed upper-right point */
  def getTransformedTopRight:Point = transformedPoint(getTopRight)
  /** Gets the transformed bottom-left point */
  def getTransformedBottomLeft:Point = transformedPoint(getBottomLeft)
  /** Gets the transformed bottom-right point */
  def getTransformedBottomRight:Point = transformedPoint(getBottomRight)

  def adjustCenter(e:Ellipse, newPoint:Point): Unit = {
    val (x,y) = newPoint
    e.setCenterX(x)
    e.setCenterY(y)
  }

  /** Copies the position of '''other''' onto this element */
  def copyPosition( other:RectangleLike with ResizableShape ): Unit = {
      this.setXY(other.getXY)
      this.setWidth(other.getWidth)
      this.setHeight(other.getHeight)
      this.setRotate(other.getRotate)
  }

  def getX: Double
  def getY: Double
  final def getXY: Point = (getX, getY)

  def move(delta:Point):Unit = setXY(delta+getXY)
  def setX(x:Double): Unit
  def setY(y:Double): Unit
  final def setXY(p:Point): Unit = {
    setX(p._1)
    setY(p._2)
  }
  def getWidth: Double
  def getHeight: Double
  def setWidth(w:Double): Unit
  def setHeight(h:Double): Unit

  private val checkValue = 5.0

  /** Only sets the width if it's > a specific value */
  private def setCheckedWidth(w:Double): Unit = {
    if(w>checkValue)
      setWidth(w)
  }
  private def setCheckedHeight(h:Double):Unit = {
    if(h>checkValue)
      setHeight(h)
  }

  private def withCheckedBounds(w:Double,h:Double)(fn: => Unit): Unit = {
    if(w>checkValue && h>checkValue) {
      setCheckedWidth(w)
      setCheckedHeight(h)
      fn
    }
  }

  def boundsChanged(): Unit = {
    adjustCenter(topLeftAnchor, getTransformedTopLeft)
    adjustCenter(topRightAnchor, getTransformedTopRight)
    adjustCenter(bottomLeftAnchor, getTransformedBottomLeft)
    adjustCenter(bottomRightAnchor, getTransformedBottomRight)
  }

  //adjust the anchors to the bounding-box
  boundsInLocalProperty().addListener { (_:Bounds, _:Bounds) =>
    boundsChanged()
  }
  //element got rotated; adjust anchors
  rotateProperty().addListener { (_:Number, _:Number) =>
    boundsChanged()
  }

  //undo-/redo command
  private var command: (=> Unit) => Command = x => { History.emptyAction }

  getAnchors.foreach { anchor =>
    anchor.setOnMouseReleased { _:MouseEvent =>
      val oldP = getTopLeft
      val oldHeight = getHeight
      val oldWidth = getWidth

      history.save(command {
        if(adjustCoordinates) {
          setXY(oldP)
        }

        setWidth(oldWidth)
        setHeight(oldHeight)
      })
    }
  }

  topLeftAnchor.setOnMousePressed(withConsumedEvent { _: MouseEvent =>
    val (oldX, oldY) = getTopLeft
    val oldHeight = getHeight
    val oldWidth = getWidth

    command = History.partialAction {
      if(adjustCoordinates) {
        setX(oldX)
        setY(oldY)
      }
      setWidth(oldWidth)
      setHeight(oldHeight)
    }
  })

  topLeftAnchor.setOnMouseDragged(withConsumedEvent { me: MouseEvent =>
    val (oldX, oldY) = getTopLeft
    val (newX, newY) = (me.getX, me.getY)
    val boundWidth = getWidth
    val boundHeight = getHeight

    val deltaX = if(oldX > newX) (oldX-newX) + boundWidth else boundWidth - (newX-oldX)
    val deltaY = if(newY < oldY) (oldY - newY)  + boundHeight else boundHeight - (newY-oldY)

    withCheckedBounds(deltaX,deltaY) {
      if(adjustCoordinates) {
        //use the new height & width for calculating the new x/y position
        setX(oldX - (getWidth-boundWidth))
        setY(oldY - (getHeight-boundHeight))
      }
    }
  })

  topRightAnchor.setOnMousePressed(withConsumedEvent { _: MouseEvent =>
    val (oldX, oldY) = getTopLeft
    val oldHeight = getHeight
    val oldWidth = getWidth

    command = History.partialAction {
      if(adjustCoordinates) {
        setY(oldY)
      }
      setWidth(oldWidth)
      setHeight(oldHeight)
    }
  })

  topRightAnchor.setOnMouseDragged(withConsumedEvent { me: MouseEvent =>
    val (oldX, oldY) = getTopRight
    val (newX, newY) = (me.getX, me.getY)

    val boundWidth = getWidth
    val boundHeight = getHeight

    val deltaX = if (newX > oldX) (newX - oldX) + boundWidth else boundWidth - (oldX - newX)
    val deltaY = if (newY < oldY) oldY - newY + boundHeight else boundHeight - (newY - oldY)

    withCheckedBounds(deltaX,deltaY) {
      if (adjustCoordinates) {
        setY(oldY - (getHeight - boundHeight))
      }
    }
  })

  bottomRightAnchor.setOnMousePressed(withConsumedEvent { _: MouseEvent =>
    val oldHeight = getHeight
    val oldWidth = getWidth

    command = History.partialAction {
      setWidth(oldWidth)
      setHeight(oldHeight)
    }
  })

  bottomRightAnchor.setOnMouseDragged(withConsumedEvent { me: MouseEvent =>
    val (oldX, oldY) = getBottomRight
    val (newX, newY) = (me.getX, me.getY)

    val boundWidth = getWidth
    val boundHeight = getHeight

    val deltaX = newX - oldX + boundWidth
    val deltaY = newY - oldY + boundHeight

    withCheckedBounds(deltaX,deltaY) {}
  })

  bottomLeftAnchor.setOnMousePressed(withConsumedEvent { me: MouseEvent =>
    val (oldX, oldY) = getTopLeft
    val oldHeight = getHeight
    val oldWidth = getWidth

    command = History.partialAction {
      if (adjustCoordinates) {
        setX(oldX)
      }
      setWidth(oldWidth)
      setHeight(oldHeight)
    }
  })

  bottomLeftAnchor.setOnMouseDragged(withConsumedEvent { me: MouseEvent =>
    val (oldX, oldY) = getBottomLeft
    val (newX, newY) = (me.getX, me.getY)

    val boundWidth = getWidth
    val boundHeight = getHeight

    val deltaX = if (newX < oldX) (oldX - newX) + boundWidth else boundWidth - (newX - oldX)
    val deltaY = if (newY > oldY) (newY - oldY) + boundHeight else boundHeight - (oldY - newY)

    withCheckedBounds(deltaX,deltaY) {
    if (adjustCoordinates) {
      setX(oldX - (getWidth-boundWidth))
    }}
  })
}
