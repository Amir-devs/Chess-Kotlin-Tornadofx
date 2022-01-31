package chess.view

import chess.core.Pieces.*
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.util.Callback
import tornadofx.*

class PromotionDialog(whiteTurn: Boolean) : Dialog<PromotablePiece>() {
    companion object {
        private val picturesSize = 75.0
        private val wantedSize = 60.0
        private val picturesScales = wantedSize / picturesSize
    }

    private val promotablePieces = mapOf(
        Queen(whiteTurn) to if (whiteTurn) "chess_ql.png" else "chess_qd.png",
        Rook(whiteTurn) to if (whiteTurn) "chess_rl.png" else "chess_rd.png",
        Bishop(whiteTurn) to if (whiteTurn) "chess_bl.png" else "chess_bd.png",
        Knight(whiteTurn) to if (whiteTurn) "chess_nl.png" else "chess_nd.png"
    )

    private val selectedPieceProperty: ObjectProperty<PromotablePiece> = SimpleObjectProperty(promotablePieces.keys.first())
    val selectedPiece: PromotablePiece get() = selectedPieceProperty.value

    init {
        title = "Choosing the promotion piece"
        headerText = "Choose your promotion piece"
        graphic = ImageView(if (whiteTurn) "chess_pl.png" else "chess_pd.png")
        dialogPane.buttonTypes.add(ButtonType.OK)

        dialogPane.content = vbox {
            hbox {
                promotablePieces.forEach { piece, image ->
                    button {
                        graphic = imageview(image, lazyload = false).scaled()
                        setOnAction { selectedPieceProperty.value = piece }
                    }
                }
            }
            hbox {
                label("Selected piece")
                imageview(promotablePieces[selectedPiece], lazyload = false) {
                    imageProperty().bind(objectBinding(selectedPieceProperty) { Image(promotablePieces[value]) })
                    scaled()
                }
            }
        }

        resultConverter = Callback { selectedPiece }
    }

    fun ImageView.scaled() = apply {
        scaleX = picturesScales
        scaleY = picturesScales
    }
}