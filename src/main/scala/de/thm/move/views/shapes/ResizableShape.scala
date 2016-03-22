package de.thm.move.views.shapes

import javafx.scene.Node
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import de.thm.move.views.Anchor
import de.thm.move.Global
import de.thm.move.controllers.implicits.FxHandlerImplicits._

trait ResizableShape extends Node {

  val resizeProportionalProperty = new SimpleBooleanProperty(false)
  val selectionRectangle = new SelectionRectangle(this)

  private def ifKeyMatches(code:KeyCode, fileKey:String)(fn : => Unit): Unit = {
    Global.shortcuts.getKeyCode(fileKey).
    filter(_==code).foreach { _ => fn }
  }

  setOnKeyPressed { ke:KeyEvent =>
      ifKeyMatches(ke.getCode(), "resize-proportional") {
          resizeProportionalProperty.set(true)
      }
  }

  setOnKeyReleased { ke:KeyEvent =>
    ifKeyMatches(ke.getCode(), "resize-proportional") {
      resizeProportionalProperty.set(false)
    }
  }


  def getAnchors: List[Anchor]

  def getX: Double
  def getY: Double
  def getWidth: Double
  def getHeight: Double

  def setX(x:Double): Unit
  def setY(y:Double): Unit
  def setWidth(w:Double): Unit
  def setHeight(h:Double): Unit
}
