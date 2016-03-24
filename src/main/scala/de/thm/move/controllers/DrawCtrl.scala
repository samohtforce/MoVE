/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */


package de.thm.move.controllers

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.input.{InputEvent, MouseEvent}
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

import de.thm.move.Global
import de.thm.move.controllers.factorys.ShapeFactory
import de.thm.move.history.History
import de.thm.move.history.History.Command
import de.thm.move.models.CommonTypes._
import de.thm.move.models.SelectedShape
import de.thm.move.models.SelectedShape._
import de.thm.move.views.shapes.{BoundedAnchors, ColorizableShape, ResizableLine, ResizableShape}
import de.thm.move.views.{Anchor, DrawPanel}

import scala.collection.JavaConversions._

class DrawCtrl(drawPanel: DrawPanel, shapeInputHandler:InputEvent => Unit) {

  private var selectedShape:Option[ResizableShape] = None

  private val tmpShapeId = "temporary-shape"

  def getDrawHandler: (SelectedShape, MouseEvent) => (Color, Color, Int) => Unit = {
    var points = List[Point]()
    var drawingShape: ResizableShape = null

    def drawHandler(shape:SelectedShape, mouseEvent:MouseEvent)(fillColor:Color, strokeColor:Color, selectedThickness:Int): Unit = {
      shape match {
        case SelectedShape.Polygon =>

          if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
            val newX = mouseEvent.getX()
            val newY = mouseEvent.getY()
            //test if polygon is finish by checking if last clicked position is 1st clicked point
            points.reverse.headOption match {
              case Some((x, y)) if Math.abs(x - newX) <= 10 && Math.abs(y - newY) <= 10 =>
                //draw the polygon
                drawPolygon(points)(fillColor, strokeColor, selectedThickness)
                points = List() //clear points list

                //remove temporary shape(s)
                removeTmpShapes(drawPanel, tmpShapeId)
              case _ =>
                //draw tmp line between last anchor and mouse point
                (points, drawingShape) match {
                  case ( (hdX,hdY)::_, l:ResizableLine ) =>
                        //draw line between last anchor (head of list) and mouse point
                        l.setEndX(newX)
                        l.setEndY(newY)
                  case _ =>
                }
                //create new line with this mouse point as start point
                drawingShape = createTmpShape(SelectedShape.Line, (mouseEvent.getX, mouseEvent.getY), strokeColor, drawPanel)

                points = (newX, newY) :: points
                drawAnchor(points.head)
            }
          }
        case _ =>
          if (mouseEvent.getEventType == MouseEvent.MOUSE_PRESSED) {
            drawingShape = createTmpShape(shape, (mouseEvent.getX, mouseEvent.getY), strokeColor, drawPanel)
            points = (mouseEvent.getX(), mouseEvent.getY()) :: points
          } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            val deltaX = mouseEvent.getX - drawingShape.getX
            val deltaY = mouseEvent.getY - drawingShape.getY

            drawingShape match {
              case ba:BoundedAnchors =>
                ba.setWidth(deltaX)
                ba.setHeight(deltaY)
              case l:ResizableLine =>
                l.setEndX(mouseEvent.getX)
                l.setEndY(mouseEvent.getY)
              case _ => //ignore other shapes
            }
          } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {

            //remove temporary shape(s)
            removeTmpShapes(drawPanel, tmpShapeId)

            points = (mouseEvent.getX(), mouseEvent.getY()) :: points

            points match {
              case end :: start :: _ => drawCustomShape(shape, start, end)(fillColor, strokeColor, selectedThickness)
              case _ => //ignore
            }
            points = List()
          }
      }
    }

    drawHandler
  }


  /**Creates a temporary shape and adds it to the given node for displaying during drawing a shape.*/
  private def createTmpShape(selectedShape:SelectedShape.SelectedShape, start:Point, stroke:Color, node:Pane, shapeId:String = tmpShapeId): ResizableShape = {
    val shape = ShapeFactory.createTemporaryShape(selectedShape, start)(stroke)
    shape.setId(shapeId)
    node.getChildren.add(shape)
    shape
  }

  /**Removes all temporary shapes (identified by temporaryId) from the given node.*/
  private def removeTmpShapes(node:Pane, temporaryId:String): Unit = {
    val removingNodes = node.getChildren.zipWithIndex.filter {
      case (n,_) => n.getId() == temporaryId
    }.map(_._1)

    node.getChildren.removeAll(removingNodes)
  }

  def setSelectedShape(shape:ResizableShape): Unit = {
    selectedShape match {
      case Some(oldShape) =>
        drawPanel.getChildren.remove(oldShape.selectionRectangle)
        drawPanel.getChildren.add(shape.selectionRectangle)
        selectedShape = Some(shape)
      case _ =>
        selectedShape = Some(shape)
        drawPanel.getChildren.add(shape.selectionRectangle)
    }
  }

  def removeSelectedShape: Unit = {
    selectedShape match {
      case Some(shape) =>
        drawPanel.getChildren.remove(shape.selectionRectangle)
        selectedShape = None
      case _ => //ignore
    }
  }

  def getMoveHandler: (MouseEvent => Unit) = {
    var deltaX = -1.0
    var deltaY = -1.0

    var command: (=> Unit) => Command = x => { History.emptyAction }

    def moveElement(mv:MouseEvent): Unit = {
      //move selected element
      mv.getEventType match {
        case MouseEvent.MOUSE_PRESSED =>
          mv.getSource match {
            case shape:ResizableShape =>
              //save old coordinates for un-/redo
              val oldX = shape.getX
              val oldY = shape.getY

              command = History.partialAction {
                shape.setX(oldX)
                shape.setY(oldY)
              }

              deltaX = oldX - mv.getSceneX
              deltaY = oldY - mv.getSceneY
            case _:Anchor => //ignore, will be repositioned when moving the shape
            case _ => throw new IllegalStateException("shapeInputHandler: source isn't a shape")
          }
        case MouseEvent.MOUSE_DRAGGED =>
          //translate from original to new position
          mv.getSource match {
            case shape:ResizableShape =>
              shape.setX(deltaX + mv.getSceneX)
              shape.setY(deltaY + mv.getSceneY)
            case _:Anchor => //ignore, will be repositioned when moving the shape
            case _ => throw new IllegalStateException("shapeInputHandler: source isn't a shape")
          }
        case MouseEvent.MOUSE_RELEASED =>
          mv.getSource match {
            case shape:ResizableShape =>
              val newX = shape.getX
              val newY = shape.getY
              val cmd = command {
                shape.setX(newX)
                shape.setY(newY)
              }

              Global.history.save(cmd)

            case _ => //ignore
          }
        case _ => //unknown event
      }
    }
    moveElement
  }

  def addToPanel[T <: Node](shape:T*): Unit = {
    shape foreach { x =>
      //add eventhandler
      x.addEventHandler(InputEvent.ANY, new EventHandler[InputEvent]() {
        override def handle(event: InputEvent): Unit = shapeInputHandler(event)
      })
      drawPanel.drawShape(x)
    }
  }

  def drawAnchor(p:Point): Unit = {
    val anchor = ShapeFactory.newAnchor(p)
    drawPanel.drawShape(anchor)
  }

  def drawPolygon(points:List[Point])(fillColor:Color, strokeColor:Color, selectedThickness: Int) = {
    val polygon = ShapeFactory.newPolygon(points)(fillColor, strokeColor, selectedThickness)
    removeDrawnAnchors(points.size)
    addToPanel(polygon)
    addToPanel(polygon.getAnchors:_*)
  }

  def drawImage(img:Image): Unit = {
    val imgview = ShapeFactory.newImage(img)
    addToPanel(imgview)
    addToPanel(imgview.getAnchors:_*)
  }

  def drawCustomShape(shape:SelectedShape, start:Point, end:Point)(fillColor:Color, strokeColor:Color, selectedThickness:Int) = {
    val (startX, startY) = start
    val (endX, endY) = end

    val newShapeOpt:Option[ResizableShape] = shape match {
      case SelectedShape.Rectangle =>
        val width = endX - startX
        val height = endY - startY
        Some( ShapeFactory.newRectangle(start, width, height)(fillColor, strokeColor, selectedThickness) )
      case SelectedShape.Circle =>
        val width = (endX - startX)/2
        val height = (endY - startY)/2
        Some( ShapeFactory.newCircle(start, width, height)(fillColor, strokeColor, selectedThickness) )
      case SelectedShape.Line => Some( ShapeFactory.newLine(start, end, selectedThickness)(fillColor, strokeColor, selectedThickness) )
      case _ => None
    }

    newShapeOpt foreach { x =>
      addToPanel(x)
      addToPanel(x.getAnchors:_*)
      //add shapes to focus-chain for getting keyboard-events
      x.setFocusTraversable(true)
      x.requestFocus()
    }
  }

  private def removeDrawnAnchors(cnt:Int):Unit =
    drawPanel.removeWhileIdx {
      case (shape, idx) => shape.isInstanceOf[Anchor] && idx<cnt
    }


  def setVisibilityOfAnchors(flag:Boolean): Unit = {
    drawPanel.getChildren.filter(_.isInstanceOf[Anchor]) foreach (  _.setVisible(flag) )
  }

  def changeColorForSelectedShape(fillColor:Option[Color], strokeColor:Option[Color]): Unit = {
    selectedShape flatMap {
      case x:ColorizableShape => Some(x)
      case _ => None
    } foreach { x =>
      fillColor foreach x.setFillColor
      strokeColor foreach x.setStrokeColor
    }
  }

  def changeStrokeWidthForSelectedShape(width:Int): Unit = {
    selectedShape flatMap {
      case x:ColorizableShape => Some(x)
      case _ => None
    } foreach ( _.setStrokeWidth(width) )
  }
}
