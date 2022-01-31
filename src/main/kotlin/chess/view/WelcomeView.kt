package chess.view

import chess.core.game.ChessGame
import javafx.geometry.Pos
import javafx.scene.image.Image
import tornadofx.*
import java.io.FileReader
import java.lang.Exception
import java.util.concurrent.Executors

class WelcomeView : View("Welcome !") {

    val l1 = label {

        text = "Welcome to Chess Game "
        style = "-fx-font-size: 50px ; -fx-font-family: \"Times New Roman\", Times, serif;-fx-text-fill: white;"
        resizeRelocate(230.0 , 30.0 , 0.0 , 0.0)
    }

    val n1 = label {

        text = "Player 1 :"
        style = "-fx-font-size: 40px ; -fx-font-family: \"Times New Roman\", Times, serif;-fx-text-fill: white;"
        resizeRelocate(150.0 , 200.0 , 0.0 , 0.0)
    }

    val n2 = label {

        text = "Player 1 :"
        style = "-fx-font-size: 40px ; -fx-font-family: \"Times New Roman\", Times, serif;-fx-text-fill: white;"
        resizeRelocate(570.0 , 200.0 , 0.0 , 0.0)
    }

    val n20 = textfield {

        style = "-fx-font-size: 20px ; -fx-font-family: \"Times New Roman\", Times, serif; -fx-background-radius: 10px;" +
                "-fx-background-width: 10px"
        resizeRelocate(540.0 , 250.0 , 0.0 , 0.0)
        setPrefSize(210.0 , 20.0)
    }

    val n10 = textfield {

        style = "-fx-font-size: 20px ; -fx-font-family: \"Times New Roman\", Times, serif; -fx-background-radius: 10px;" +
                "-fx-background-width: 10px"
        resizeRelocate(120.0 , 250.0 , 0.0 , 0.0)
        setPrefSize(210.0 , 20.0)
    }

    val b1 = button {

        text = "Start new game"
        style = "-fx-font-size: 30px ; -fx-font-family: \"Times New Roman\", Times, serif ; -fx-background-radius: 20px  "
        resizeRelocate(290.0 , 380.0 , 0.0 , 0.0)
        setPrefSize(310.0 , 50.0)
        action {
            try {
                this@WelcomeView.close()
                val m = MainView()
                m.openWindow()
            } catch ( e : Exception ) {}
        }
    }

    val b2 = button {

        text = "Load game"
        style = "-fx-font-size: 30px ; -fx-font-family: \"Times New Roman\", Times, serif ; -fx-background-radius: 20px  "
        resizeRelocate(290.0 , 440.0 , 0.0 , 0.0)
        setPrefSize(310.0 , 50.0)
        action {
            try {
                val fenReader = FileReader("Log.txt")
                val loadFen = fenReader.readText()
                ChessGame.INITIAL_POSITION = ChessGame.fenToGame(loadFen)
                this@WelcomeView.close()
                val m = MainView()
                m.openWindow()
            } catch ( e : Exception ) {}
        }
    }

    override val root = anchorpane{

        setPrefSize(950.0 , 600.0 )
        style = "-fx-background-image: url(\"/dark_background.jpg\"); -fx-background-position: center; -fx-background-size: cover"
        add(l1)
        add(n1)
        add(n10)
        add(n2)
        add(n20)
        add(b1)
        add(b2)
    }
}
