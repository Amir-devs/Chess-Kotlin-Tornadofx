package chess.core.Pieces

import chess.core.game.*

interface Promotable

abstract class PromotablePiece(override val whitePlayer: Boolean) : ChessPiece(whitePlayer), Promotable

sealed class ChessPiece(open val whitePlayer: Boolean) {
    companion object {
        fun fenToPiece(pieceFen: Char): ChessPiece? {
            return when(pieceFen){
                'P' -> Pawn(true)
                'p' -> Pawn(false)
                'N' -> Knight(true)
                'n' -> Knight(false)
                'B' -> Bishop(true)
                'b' -> Bishop(false)
                'R' -> Rook(true)
                'r' -> Rook(false)
                'Q' -> Queen(true)
                'q' -> Queen(false)
                'K' -> King(true)
                'k' -> King(false)
                else -> null
            }
        }

        fun thereIsBlockadeBetweenStartAndTarget(deltaFile: Int, deltaRank: Int, game: ChessGame, startSquare: Coordinates): Boolean {
            val squaresBetweenTargetAndStart = arrayListOf<Coordinates>()
            val deltaFileSign = if (deltaFile == 0) 0 else deltaFile / Math.abs(deltaFile)
            val deltaRankSign = if (deltaRank == 0) 0 else deltaRank / Math.abs(deltaRank)
            if (deltaFileSign == 0 && deltaRankSign == 0) return false
            val numSquares = if (deltaFileSign == 0) Math.abs(deltaRank) - 1 else Math.abs(deltaFile) - 1
            (1..numSquares).forEach {
                val soughtFile = startSquare.file + it * deltaFileSign
                val soughtRank = startSquare.rank + it * deltaRankSign
                if (soughtFile !in 0..7 || soughtRank !in 0..7) return@forEach
                squaresBetweenTargetAndStart.add(
                    Coordinates(soughtRank, soughtFile))
            }
            if (squaresBetweenTargetAndStart.any {
                    game.board[it.rank, it.file] != null
                }) return true
            return false
        }
    }

    abstract fun toFEN() : Char

    // With pseudo-legal moves, we still can leave our own king in chess
    abstract fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean
}

data class Pawn(override val whitePlayer: Boolean) : ChessPiece(whitePlayer){
    override fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean {
        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val isValidTwoCellsJumpAsWhite = (game.info.whiteTurn && deltaFile == 0 && deltaRank == 2 && move.from.rank == ChessBoard.RANK_2
                && game.board[ChessBoard.RANK_3, move.from.file] == null
                && game.board[ChessBoard.RANK_4, move.from.file] == null)
        val isValidTwoCellsJumpAsBlack = (!game.info.whiteTurn && deltaFile == 0 && deltaRank == -2 && move.from.rank == ChessBoard.RANK_7
                && game.board[ChessBoard.RANK_6, move.from.file] == null
                && game.board[ChessBoard.RANK_5, move.from.file] == null)

        val isValidForwardMoveAsWhite = (game.info.whiteTurn && deltaRank == 1 && deltaFile == 0
                && game.board[move.from.rank+1, move.from.file] == null)

        val isValidForwardMoveAsBlack = (!game.info.whiteTurn && deltaRank == -1 && deltaFile == 0
                && game.board[move.from.rank-1, move.from.file] == null)

        val isValidCaptureMoveAsWhite = (game.info.whiteTurn && deltaRank == 1 && Math.abs(deltaFile) == 1
                && game.board[move.to.rank, move.to.file] != null
                && !game.board[move.to.rank, move.to.file]!!.whitePlayer)
        val isValidCaptureMoveAsBlack = (!game.info.whiteTurn && deltaRank == -1 && Math.abs(deltaFile) == 1
                && game.board[move.to.rank, move.to.file] != null
                && game.board[move.to.rank, move.to.file]!!.whitePlayer)

        val isValidEnPassantCaptureAsWhite = (game.info.whiteTurn
                && move.from.rank == ChessBoard.RANK_5 && move.to.rank == ChessBoard.RANK_6
                && Math.abs(deltaFile) == 1
                && game.board[move.to.rank, move.to.file] == null
                && game.info.enPassantFile == move.to.file)

        val isValidEnPassantCaptureAsBlack = (!game.info.whiteTurn
                && move.from.rank == ChessBoard.RANK_4 && move.to.rank == ChessBoard.RANK_3
                && Math.abs(deltaFile) == 1
                && game.board[move.to.rank, move.to.file] == null
                && game.info.enPassantFile == move.to.file)

        val ownerPlayer = whitePlayer == game.info.whiteTurn
        val followValidLine = isValidTwoCellsJumpAsWhite || isValidTwoCellsJumpAsBlack
                || isValidForwardMoveAsWhite || isValidForwardMoveAsBlack
                || isValidCaptureMoveAsWhite || isValidCaptureMoveAsBlack
                || isValidEnPassantCaptureAsWhite || isValidEnPassantCaptureAsBlack

        return followValidLine && ownerPlayer
    }

    override fun toFEN(): Char {
        return if (whitePlayer) 'P' else 'p'
    }


}
data class Knight(override val whitePlayer: Boolean) : PromotablePiece(whitePlayer) {
    override fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean {
        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val absDeltaFile = Math.abs(deltaFile)
        val absDeltaRank = Math.abs(deltaRank)

        val pieceAtEndCell = game.board[move.to.rank, move.to.file]
        val endSquarePieceIsEnemy = pieceAtEndCell?.whitePlayer != whitePlayer
        val followValidLine = (absDeltaFile == 1 && absDeltaRank == 2) || (absDeltaFile == 2 && absDeltaRank == 1)

        val ownerPlayer = whitePlayer == game.info.whiteTurn

        return followValidLine && endSquarePieceIsEnemy && ownerPlayer
    }

    override fun toFEN(): Char {
        return if (whitePlayer) 'N' else 'n'
    }
}
data class Bishop(override val whitePlayer: Boolean) : PromotablePiece(whitePlayer) {
    override fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean {
        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val absDeltaFile = Math.abs(deltaFile)
        val absDeltaRank = Math.abs(deltaRank)

        val pieceAtEndCell = game.board[move.to.rank, move.to.file]
        val endSquarePieceIsEnemy = pieceAtEndCell?.whitePlayer != whitePlayer
        val followValidLine = absDeltaFile == absDeltaRank

        val ownerPlayer = whitePlayer == game.info.whiteTurn

        if (thereIsBlockadeBetweenStartAndTarget(deltaFile, deltaRank, game, move.from)) return false

        return followValidLine && endSquarePieceIsEnemy && ownerPlayer
    }

    override fun toFEN(): Char {
        return if (whitePlayer) 'B' else 'b'
    }
}
data class Rook(override val whitePlayer: Boolean) : PromotablePiece(whitePlayer) {
    override fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean {
        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val absDeltaFile = Math.abs(deltaFile)
        val absDeltaRank = Math.abs(deltaRank)

        val pieceAtEndCell = game.board[move.to.rank, move.to.file]
        val endSquarePieceIsEnemy = pieceAtEndCell?.whitePlayer != whitePlayer
        val followValidLine = (absDeltaFile == 0 && absDeltaRank > 0) || (absDeltaFile > 0 && absDeltaRank == 0)

        if (thereIsBlockadeBetweenStartAndTarget(deltaFile, deltaRank, game, move.from)) return false

        val ownerPlayer = whitePlayer == game.info.whiteTurn

        return followValidLine && endSquarePieceIsEnemy && ownerPlayer
    }

    override fun toFEN(): Char {
        return if (whitePlayer) 'R' else 'r'
    }
}
data class Queen(override val whitePlayer: Boolean) : PromotablePiece(whitePlayer) {
    override fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean {
        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val absDeltaFile = Math.abs(deltaFile)
        val absDeltaRank = Math.abs(deltaRank)

        val pieceAtEndCell = game.board[move.to.rank, move.to.file]
        val endSquarePieceIsEnemy = pieceAtEndCell?.whitePlayer != whitePlayer
        val followValidLine = (absDeltaFile == 0 && absDeltaRank > 0) || (absDeltaFile > 0 && absDeltaRank == 0) ||
                absDeltaFile == absDeltaRank

        if (thereIsBlockadeBetweenStartAndTarget(deltaFile, deltaRank, game, move.from)) return false

        val ownerPlayer = whitePlayer == game.info.whiteTurn

        return followValidLine && endSquarePieceIsEnemy && ownerPlayer
    }

    override fun toFEN(): Char {
        return if (whitePlayer) 'Q' else 'q'
    }
}
data class King(override val whitePlayer: Boolean) : ChessPiece(whitePlayer){
    override fun isValidPseudoLegalMove(game: ChessGame, move: Move): Boolean {
        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val absDeltaFile = Math.abs(deltaFile)
        val absDeltaRank = Math.abs(deltaRank)

        val pieceAtEndCell = game.board[move.to.rank, move.to.file]
        val followValidLine = absDeltaFile <= 1 && absDeltaRank <= 1
        val endPieceIsEnemy = pieceAtEndCell?.whitePlayer != whitePlayer

        val whiteKingCrossingF1Pieces = copyGamePiecesIntoArray(game)
        whiteKingCrossingF1Pieces[ChessBoard.RANK_1][ChessBoard.FILE_E] = null
        whiteKingCrossingF1Pieces[ChessBoard.RANK_1][ChessBoard.FILE_F] = King(whitePlayer = true)
        val whiteKingCrossingF1Info = game.info.copy(whiteTurn = true)
        val whiteKingCrossingF1Position = game.copy(board = ChessBoard(whiteKingCrossingF1Pieces), info = whiteKingCrossingF1Info)
        val isLegalKingSideCastleAsWhite = game.info.whiteTurn
                && WhiteKingSideCastle in game.info.castles
                && deltaFile == 2 && deltaRank == 0
                && move.from == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_E)
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_H] == Rook(whitePlayer = true)
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_F] == null
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_G] == null
                && !game.playerKingIsAttacked()
                && !whiteKingCrossingF1Position.playerKingIsAttacked()

        val whiteKingCrossingD1Pieces = copyGamePiecesIntoArray(game)
        whiteKingCrossingD1Pieces[ChessBoard.RANK_1][ChessBoard.FILE_E] = null
        whiteKingCrossingD1Pieces[ChessBoard.RANK_1][ChessBoard.FILE_D] = King(whitePlayer = true)
        val whiteKingCrossingD1Info = game.info.copy(whiteTurn = true)
        val whiteKingCrossingD1Position = game.copy(board = ChessBoard(whiteKingCrossingD1Pieces), info = whiteKingCrossingD1Info)
        val isLegalQueenSideCastleAsWhite = game.info.whiteTurn
                && WhiteQueenSideCastle in game.info.castles
                && deltaFile == -2 && deltaRank == 0
                && move.from == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_E)
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_A] == Rook(whitePlayer = true)
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_D] == null
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_C] == null
                && game.board[ChessBoard.RANK_1, ChessBoard.FILE_B] == null
                && !game.playerKingIsAttacked()
                && !whiteKingCrossingD1Position.playerKingIsAttacked()

        val blackKingCrossingF8Pieces = copyGamePiecesIntoArray(game)
        blackKingCrossingF8Pieces[ChessBoard.RANK_8][ChessBoard.FILE_E] = null
        blackKingCrossingF8Pieces[ChessBoard.RANK_8][ChessBoard.FILE_F] = King(whitePlayer = false)
        val blackKingCrossingF8Info = game.info.copy(whiteTurn = false)
        val blackKingCrossingF8Position = game.copy(board = ChessBoard(blackKingCrossingF8Pieces), info = blackKingCrossingF8Info)
        val isLegalKingSideCastleAsBlack = !game.info.whiteTurn
                && BlackKingSideCastle in game.info.castles
                && deltaFile == 2 && deltaRank == 0
                && move.from == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_E)
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_H] == Rook(whitePlayer = false)
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_F] == null
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_G] == null
                && !game.playerKingIsAttacked()
                && !blackKingCrossingF8Position.playerKingIsAttacked()

        val blackKingCrossingD8Pieces = copyGamePiecesIntoArray(game)
        blackKingCrossingD8Pieces[ChessBoard.RANK_8][ChessBoard.FILE_E] = null
        blackKingCrossingD8Pieces[ChessBoard.RANK_8][ChessBoard.FILE_D] = King(whitePlayer = false)
        val blackKingCrossingD8Info = game.info.copy(whiteTurn = false)
        val blackKingCrossingD8Position = game.copy(board = ChessBoard((blackKingCrossingD8Pieces)), info = blackKingCrossingD8Info)
        val isLegalQueenSideCastleAsBlack = !game.info.whiteTurn
                && BlackQueenSideCastle in game.info.castles
                && deltaFile == -2 && deltaRank == 0
                && move.from == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_E)
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_A] == Rook(whitePlayer = false)
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_D] == null
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_C] == null
                && game.board[ChessBoard.RANK_8, ChessBoard.FILE_B] == null
                && !game.playerKingIsAttacked()
                && !blackKingCrossingD8Position.playerKingIsAttacked()

        val isLegalCastle = isLegalKingSideCastleAsWhite || isLegalQueenSideCastleAsWhite
                || isLegalKingSideCastleAsBlack || isLegalQueenSideCastleAsBlack

        val ownerPlayer = whitePlayer == game.info.whiteTurn

        return ((followValidLine && endPieceIsEnemy) || isLegalCastle) && ownerPlayer
    }

    private fun copyGamePiecesIntoArray(game: ChessGame): Array<Array<ChessPiece?>> {
        val array = Array(8, { Array<ChessPiece?>(8, { null }) })
        for (rank in 0..7) {
            for (file in 0..7) {
                array[rank][file] = game.board[rank, file]
            }
        }
        return array
    }

    override fun toFEN(): Char {
        return if (whitePlayer) 'K' else 'k'
    }
}
