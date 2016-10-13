/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.thm.move.views.dialogs

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}

/** Dialog for the Source-code format. */
class SrcFormatDialog extends Alert(AlertType.CONFIRMATION) {
  val onelineBtn = new ButtonType("One liner")
  val prettyBtn = new ButtonType("Pretty")

  setTitle("Modelica Sourcecode")
  setHeaderText("Pretty formatted code or one-line code?")
  getButtonTypes.addAll(onelineBtn, prettyBtn)
}
