/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.thm.move.views.shapes

import javafx.scene.shape.Line


import de.thm.move.types._

class ResizableLine(
         start:Point,
         end:Point,
         strokeSize:Int)
   extends Line(start._1, start._2, end._1, end._2)
   with ResizableShape
   with ColorizableShape
   with PathLike {

  setStrokeWidth(strokeSize)
  override lazy val edgeCount: Int = 2

  override def copy: ResizableLine = {
    val duplicate = new ResizableLine(
      (getStartX,getStartY),
      (getEndX,getEndY),
      strokeSize)
    duplicate.copyColors(this)
    duplicate.setRotate(getRotate)
    duplicate
  }

  override def getEdgePoint(idx: Int): (Double, Double) = idx match {
    case 0 => (getStartX,getStartY)
    case 1 => (getEndX,getEndY)
    case _ => throw new IllegalArgumentException(s"There is now edge with given idx $idx")
  }

  override def resize(idx: Int, delta: (Double, Double)): Unit = idx match {
    case 0 =>
      setStartX(getStartX+delta.x)
      setStartY(getStartY+delta.y)
    case 1 =>
      setEndX(getEndX+delta.x)
      setEndY(getEndY+delta.y)
    case _ => throw new IllegalArgumentException(s"There is now edge with given idx $idx")
  }
}
