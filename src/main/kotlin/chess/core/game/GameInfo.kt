package chess.core.game

sealed class Castle
object WhiteKingSideCastle : Castle()
object WhiteQueenSideCastle: Castle()
object BlackKingSideCastle: Castle()
object BlackQueenSideCastle: Castle()


data class GameInfo(val whiteTurn: Boolean, val castles: List<Castle>,
                    val enPassantFile: Int?, val nullityCount: Int, val moveNumber: Int) {
    companion object{
        fun fenToGameInfo(fen: String) : GameInfo {
            val parts = fen.split("""\s+""".toRegex())

            val whiteTurn = parts[1] == "w"

            var castles = listOf<Castle>()
            if (parts[2].contains('K')) castles += WhiteKingSideCastle
            if (parts[2].contains('Q')) castles += WhiteQueenSideCastle
            if (parts[2].contains('k')) castles += BlackKingSideCastle
            if (parts[2].contains('q')) castles += BlackQueenSideCastle

            val enPassantFile = if (parts[3][0].toInt() in ('a'.toInt()..'h'.toInt()))
                parts[3][0].toInt() - 'a'.toInt()
            else null

            val nullityCount = Integer.parseInt(parts[4])
            if (nullityCount < 0) throw IllegalArgumentException("nullityCount($nullityCount) must be >= 0.")

            val moveNumber = Integer.parseInt(parts[5])
            if (moveNumber < 1) throw IllegalArgumentException("moveNumber($moveNumber) must be >= 1.")

            return GameInfo(whiteTurn, castles, enPassantFile, nullityCount, moveNumber)
        }
    }

    fun toFEN() : String {
        val builder = StringBuilder()

        if (whiteTurn) builder.append("w ") else builder.append("b ")

        val castlesBuilder = StringBuilder()
        if (castles.contains(WhiteKingSideCastle)) castlesBuilder.append("K")
        if (castles.contains(WhiteQueenSideCastle)) castlesBuilder.append("Q")
        if (castles.contains(BlackKingSideCastle)) castlesBuilder.append("k")
        if (castles.contains(BlackQueenSideCastle)) castlesBuilder.append("q")
        if (castlesBuilder.isEmpty()) builder.append("- ") else builder.append("$castlesBuilder ")

        if (enPassantFile == null){
            builder.append("- ")
        }
        else {
            val enPassantBuilder = StringBuilder()
            val fileASCII = enPassantFile + 'a'.toInt()
            enPassantBuilder.append(fileASCII.toChar())
            enPassantBuilder.append(if (whiteTurn) "6 " else "3 ")
            builder.append(enPassantBuilder)
        }

        builder.append("$nullityCount $moveNumber")

        return builder.toString()
    }
}
