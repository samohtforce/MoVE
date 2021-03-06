/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.thm.move.controllers

import de.thm.move.views.shapes.ResizableShape

/** Base trait for all functions that work with selected elements */
trait SelectionCtrlLike {
  /** Returns the currently selected shapes */
  def getSelectedShapes: List[ResizableShape]
}
