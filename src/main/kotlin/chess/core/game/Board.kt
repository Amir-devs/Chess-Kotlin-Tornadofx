package chess.core.game

import chess.core.Pieces.ChessPiece

class ChessBoard(val pieces: Array<Array<ChessPiece?>>) {

    companion object {
        val FILE_A = 0
        val FILE_B = 1
        val FILE_C = 2
        val FILE_D = 3
        val FILE_E = 4
        val FILE_F = 5
        val FILE_G = 6
        val FILE_H = 7

        val RANK_1 = 0
        val RANK_2 = 1
        val RANK_3 = 2
        val RANK_4 = 3
        val RANK_5 = 4
        val RANK_6 = 5
        val RANK_7 = 6
        val RANK_8 = 7


        fun fenToChessBoard(fen: String):ChessBoard{
            val pieces = Array(8, { Array<ChessPiece?>(8, {null})})

            val boardPart = fen.split("""\s+""".toRegex())[0]
            val lines = boardPart.split("/").reversed()

            for (rank in 0..7){
                val currentLine = lines[rank]
                var file = 0
                for (currentChar in currentLine){
                    if (currentChar.isDigit()){
                        file += currentChar.toInt() - '0'.toInt()
                    }
                    else {
                        pieces[rank][file++] = ChessPiece.fenToPiece(currentChar)
                    }
                }
            }

            return ChessBoard(pieces)
        }
    }

    operator fun get(rank: Int, file: Int):ChessPiece? = pieces[rank][file]

    fun toFEN():String {
        val builder = StringBuilder()

        for (rank in 7.downTo(0)){
            var currentGap = 0
            (0..7).forEach{ file ->
                val currentPiece = pieces[rank][file]
                if (currentPiece == null){
                    currentGap++
                }
                else {
                    if (currentGap > 0) builder.append("$currentGap")
                    currentGap = 0
                    builder.append(currentPiece.toFEN())
                }
            }
            if (currentGap > 0) builder.append("$currentGap")
            if (rank > 0) builder.append('/')
        }

        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is ChessBoard){
            return other.toFEN() == toFEN()
        }
        return false
    }

    override fun hashCode(): Int {
        return toFEN().hashCode()
    }

    override fun toString(): String {
        return toFEN()
    }
}

data class Coordinates(val rank: Int, val file: Int){
    init {
        require(rank in 0..7)
        require(file in 0..7)
    }
}
