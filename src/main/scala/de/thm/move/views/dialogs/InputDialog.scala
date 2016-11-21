package de.thm.move.views.dialogs

import javafx.scene.control.Dialog
import de.thm.move.util.StringConverter
import javafx.scene.control.Label
import javafx.scene.control.TextField
import de.thm.move.controllers.implicits.LambdaImplicits._
import de.thm.move.util.StringMarshaller
import javafx.scene.layout.GridPane
import javafx.scene.control.ButtonType
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar.ButtonData

import scala.collection.JavaConverters._

class InputDialog[A: StringMarshaller](names:(String, Option[A])*) extends Dialog[List[A]] {
  //get the marshaller & setup labels, textfields
  private val marshaller = implicitly[StringMarshaller[A]]
  private val inputList =
    names.map { case (name, opt) =>
      val lbl = new Label(name)
      lbl.getStyleClass.add("inputdialog-label")
      val txtField = new TextField(opt.map(marshaller.decode).getOrElse(""))
      txtField.getStyleClass.add("inputdialog-field")
      lbl -> txtField
    }

  private val btns = List(new ButtonType("OK", ButtonData.OK_DONE), new ButtonType("Cancel", ButtonData.CANCEL_CLOSE))
  getDialogPane.getButtonTypes().addAll(btns.asJava)

  //setup pane
  val pane = new GridPane()
  pane.setMaxWidth(Double.MaxValue)
  pane.getStyleClass.add("inputdialog-gridpane")
  
  for(((lbl, txtField), idx) <- inputList.zipWithIndex) {
    pane.add(lbl, 0, idx)
    pane.add(txtField, 1, idx)
  }
  
  //convert ButtonType to List[A] using the marshaller
  this.setResultConverter { bt:ButtonType =>
    (for {
      (_,txtField) <- inputList
      value = marshaller.encode(txtField.getText)
    } yield value).toList
  }
  
  this.getDialogPane.setContent(pane)
}

object InputDialog {
  def fromPairs[A: StringMarshaller](names:(String, Option[A])*):InputDialog[A] = new InputDialog(names:_*)
  def fromNames[A: StringMarshaller](names:String*):InputDialog[A] = {
    val list = names zip (names map(_ => None))
    new InputDialog(list:_*)
  }
}