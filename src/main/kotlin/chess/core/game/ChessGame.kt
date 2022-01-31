package chess.core.game

import chess.core.Pieces.*

data class ChessGame(val board: ChessBoard, val info: GameInfo){
    companion object {
        fun fenToGame(fen: String): ChessGame {
            return ChessGame(board = ChessBoard.fenToChessBoard(fen),
                info = GameInfo.fenToGameInfo(fen))
        }
        var INITIAL_POSITION = ChessGame.fenToGame("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    fun toFEN(): String = "${board.toFEN()} ${info.toFEN()}"

    fun getSANForMove(move: Move, promotionPiece: PromotablePiece = Queen(info.whiteTurn)) : String {
        if (!isValidMove(move)) throw IllegalMoveException()

        val builder = StringBuilder()

        if (isWhiteKingSideCastle(move) || isBlackKingSideCastle(move)) builder.append("O-O")
        else if (isWhiteQueenSideCastle(move) || isBlackQueenSideCastle(move)) builder.append("O-O-O")
        else {
            val pieceAtStartSquare = board[move.from.rank, move.from.file]
            if (pieceAtStartSquare == null) throw NoPieceAtStartCellException()

            if (pieceAtStartSquare is Pawn) {
                val isCapturingMove = move.from.file != move.to.file
                val isPromotionMove = isPromotionMove(move)

                if (isCapturingMove) {
                    builder.append(('a'.toInt() + move.from.file).toChar())
                    builder.append('x')
                }
                builder.append(('a'.toInt() + move.to.file).toChar())
                builder.append(('1'.toInt() + move.to.rank).toChar())

                if (isPromotionMove){
                    builder.append('=')
                    builder.append(promotionPiece.toFEN().toUpperCase())
                }
            }
            else {
                val allMovesWithSameEndSquareAndSameMovingPiece = getAllPossibleMoves().filter { it.to == move.to }.filter {
                    val soughtMovePieceAtStartSquare = board[it.from.rank, it.from.file]
                    soughtMovePieceAtStartSquare == pieceAtStartSquare
                }.filter { it != move }

                val samePieceWithSameRank = allMovesWithSameEndSquareAndSameMovingPiece.any{ it.from.rank == move.from.rank }
                val samePieceWithSameFile = allMovesWithSameEndSquareAndSameMovingPiece.any{ it.from.file == move.from.file }

                builder.append(pieceAtStartSquare.toFEN().toUpperCase())
                if (allMovesWithSameEndSquareAndSameMovingPiece.isNotEmpty()){
                    if (samePieceWithSameFile || samePieceWithSameRank){
                        if (samePieceWithSameRank) builder.append(('a'.toInt() + move.from.file).toChar())
                        if (samePieceWithSameFile) builder.append(('1'.toInt() + move.from.rank).toChar())
                    }
                    else {
                        builder.append(('a'.toInt() + move.from.file).toChar())
                    }
                }

                val pieceAtEndSquare = board[move.to.rank, move.to.file]
                val isCapturingMove = pieceAtEndSquare?.whitePlayer == !info.whiteTurn
                if (isCapturingMove) builder.append('x')

                builder.append(('a'.toInt() + move.to.file).toChar())
                builder.append(('1'.toInt() + move.to.rank).toChar())
            }
        }

        val positionAfterMove = doMoveWithValidation(move)
        if (positionAfterMove.playerIsMate()) builder.append('#')
        else if (positionAfterMove.playerKingIsAttacked()) builder.append('+')

        return builder.toString()
    }

    fun getFANForMove(move: Move, promotionPiece: PromotablePiece = Queen(info.whiteTurn)) : String {
        val moveSAN = getSANForMove(move = move, promotionPiece = promotionPiece)
        return moveSAN.replace('N', if (info.whiteTurn) '\u2658' else '\u265E')
            .replace('B', if (info.whiteTurn) '\u2657' else '\u265D')
            .replace('R', if (info.whiteTurn) '\u2656' else '\u265C')
            .replace('Q', if (info.whiteTurn) '\u2655' else '\u265B')
            .replace('K', if (info.whiteTurn) '\u2654' else '\u265A')
    }

    fun searchForKingCoordinates(whiteTurn: Boolean) : Coordinates? {
        var playerKingPosition:Coordinates? = null

        kingPositionOuterLoop@ for (rank in 0..7){
            for (file in 0..7){
                if (board[rank, file] == King(whiteTurn)) {
                    playerKingPosition = Coordinates(rank, file)
                    break@kingPositionOuterLoop
                }
            }
        }

        return playerKingPosition
    }

    fun playerKingIsAttacked() : Boolean {
        val gameWithTurnInverse = copy(info = info.copy(whiteTurn = !this.info.whiteTurn, enPassantFile = null))
        var isAttacked = false

        val currentPlayerTurnKingPosition:Coordinates? = gameWithTurnInverse.searchForKingCoordinates(info.whiteTurn)
        if (currentPlayerTurnKingPosition == null) throw PlayerHasNoKingException()

        attackSearchOuterLoop@ for(rank in 0..7){
            for (file in 0..7){
                /*
                We can't test with kings because of infinite recursive calls between methods
                But anyway, that does not matter as a king can't attack the other king.
                 */
                if (gameWithTurnInverse.board[rank, file] !is King
                    && gameWithTurnInverse.isValidPseudoMove(
                        Move(from = Coordinates(rank = rank, file = file), to = currentPlayerTurnKingPosition))){
                    isAttacked = true
                    break@attackSearchOuterLoop
                }
            }
        }

        return isAttacked
    }

    fun playerIsMate(): Boolean {
        if (!playerKingIsAttacked()) return false

        val possiblesMoves = getAllPossibleMoves()
        return possiblesMoves.all {
            val positionAfterMove = doMoveWithValidation(it)
            val positionAfterMoveButWithCurrentTurn = positionAfterMove.copy(info = info.copy(whiteTurn = this.info.whiteTurn) )
            return positionAfterMoveButWithCurrentTurn.playerKingIsAttacked()
        }
    }

    fun getAllPossibleMoves(): List<Move> {
        val movesList = mutableListOf<Move>()

        for (startCellRank in 0..7){
            for (startCellFile in 0..7){
                val startCell = Coordinates(rank = startCellRank, file = startCellFile)
                for (endCellRank in 0..7){
                    for (endCellFile in 0..7){
                        val endCell = Coordinates(rank = endCellRank, file = endCellFile)
                        if (startCell == endCell) continue

                        val move = Move(from = startCell, to = endCell)

                        if (isValidMove(move)) movesList.add(move)
                    }
                }
            }
        }

        return movesList
    }

    fun playerKingIsUnderAttackAfterMove(move: Move) : Boolean {
        val positionAfterMove = this.copy().doMoveWithoutValidation(move)
        val positionAfterMoveButWithCurrentPlayerTurn_GameInfo = positionAfterMove.info.copy(whiteTurn = this.info.whiteTurn)
        val positionAfterMoveButWithCurrentPlayerTurn = positionAfterMove.copy(info = positionAfterMoveButWithCurrentPlayerTurn_GameInfo)

        return positionAfterMoveButWithCurrentPlayerTurn.playerKingIsAttacked()
    }

    fun isValidMove(move: Move): Boolean {
        return isValidPseudoMove(move)
                && !playerKingIsUnderAttackAfterMove(move)
    }

    fun isValidPseudoMove(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file]
        if (pieceAtStartSquare?.whitePlayer != info.whiteTurn) return false
        return pieceAtStartSquare.isValidPseudoLegalMove(this, move)
    }

    fun isWhiteKingSideCastle(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: return false

        return info.whiteTurn
                && WhiteKingSideCastle in info.castles
                && pieceAtStartSquare == King(whitePlayer = true)
                && board[ChessBoard.RANK_1, ChessBoard.FILE_H] == Rook(whitePlayer = true)
                && move.from == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_E)
                && move.to == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_G)
                && board[ChessBoard.RANK_1, ChessBoard.FILE_F] == null
                && board[ChessBoard.RANK_1, ChessBoard.FILE_G] == null
    }

    fun isWhiteQueenSideCastle(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: return false

        return info.whiteTurn
                && WhiteQueenSideCastle in info.castles
                && pieceAtStartSquare == King(whitePlayer = true)
                && board[ChessBoard.RANK_1, ChessBoard.FILE_A] == Rook(whitePlayer = true)
                && move.from == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_E)
                && move.to == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_C)
                && board[ChessBoard.RANK_1, ChessBoard.FILE_D] == null
                && board[ChessBoard.RANK_1, ChessBoard.FILE_C] == null
                && board[ChessBoard.RANK_1, ChessBoard.FILE_B] == null
    }

    fun isBlackKingSideCastle(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: return false

        return !info.whiteTurn
                && BlackKingSideCastle in info.castles
                && pieceAtStartSquare == King(whitePlayer = false)
                && board[ChessBoard.RANK_8, ChessBoard.FILE_H] == Rook(whitePlayer = false)
                && move.from == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_E)
                && move.to == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_G)
                && board[ChessBoard.RANK_8, ChessBoard.FILE_F] == null
                && board[ChessBoard.RANK_8, ChessBoard.FILE_G] == null
    }

    fun isBlackQueenSideCastle(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: return false

        return !info.whiteTurn
                && BlackQueenSideCastle in info.castles
                && pieceAtStartSquare == King(whitePlayer = false)
                && board[ChessBoard.RANK_8, ChessBoard.FILE_A] == Rook(whitePlayer = false)
                && move.from == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_E)
                && move.to == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_C)
                && board[ChessBoard.RANK_8, ChessBoard.FILE_D] == null
                && board[ChessBoard.RANK_8, ChessBoard.FILE_C] == null
                && board[ChessBoard.RANK_8, ChessBoard.FILE_B] == null
    }

    fun isEnPassantMove(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: return false
        val pieceAtEndSquare = board[move.to.rank, move.to.file]
        val isWhiteEnPassantMove = info.whiteTurn && pieceAtStartSquare == Pawn(whitePlayer = true)
                && pieceAtEndSquare == null && info.enPassantFile == move.to.file
                && move.from.rank == ChessBoard.RANK_5 && move.to.rank == ChessBoard.RANK_6
        val isBlackEnPassantMove = !info.whiteTurn && pieceAtStartSquare == Pawn(whitePlayer = false)
                && pieceAtEndSquare == null && info.enPassantFile == move.to.file
                && move.from.rank == ChessBoard.RANK_4 && move.to.rank == ChessBoard.RANK_3

        return isWhiteEnPassantMove || isBlackEnPassantMove
    }

    fun isPromotionMove(move: Move): Boolean {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: return false

        val promotionAsWhite = info.whiteTurn && pieceAtStartSquare == Pawn(whitePlayer = true)
                && move.to.rank == ChessBoard.RANK_8
        val promotionAsBlack = !info.whiteTurn && pieceAtStartSquare == Pawn(whitePlayer = false)
                && move.to.rank == ChessBoard.RANK_1

        return promotionAsWhite || promotionAsBlack
    }

    fun doMoveWithValidation(move: Move,
                             promotionPiece: PromotablePiece = Queen(info.whiteTurn)): ChessGame {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: throw NoPieceAtStartCellException()
        if (!pieceAtStartSquare.isValidPseudoLegalMove(this, move)) throw IllegalMoveException()

        return doMoveWithoutValidation(move, promotionPiece)
    }

    fun doMoveWithoutValidation(move: Move,
                                promotionPiece: PromotablePiece = Queen(info.whiteTurn)): ChessGame {
        val pieceAtStartSquare = board[move.from.rank, move.from.file] ?: throw NoPieceAtStartCellException()

        val capturingMove = board[move.to.rank, move.to.file] != null

        val deltaFile = move.to.file - move.from.file
        val deltaRank = move.to.rank - move.from.rank

        val modifiedBoardArray = copyBoardIntoArray()
        val newMoveNumber = if (!info.whiteTurn) info.moveNumber+1 else info.moveNumber
        var modifiedGameInfo = info.copy(whiteTurn = !info.whiteTurn, moveNumber = newMoveNumber)

        if (isEnPassantMove(move)) {
            modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null, nullityCount = 0)
            modifiedBoardArray[move.from.rank][move.from.file] = null
            modifiedBoardArray[move.to.rank][move.to.file] = pieceAtStartSquare
            modifiedBoardArray[if (info.whiteTurn) (move.to.rank-1) else (move.to.rank+1)][move.to.file] = null
        }
        else if (isPromotionMove(move)) {
            modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null, nullityCount = 0)
            if (promotionPiece.whitePlayer != info.whiteTurn) throw WrongPromotionPieceColor()
            modifiedBoardArray[move.from.rank][move.from.file] = null
            modifiedBoardArray[move.to.rank][move.to.file] = promotionPiece
        }
        else if (isWhiteKingSideCastle(move)) {
            modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null, nullityCount = modifiedGameInfo.nullityCount+1)
            val pathEmpty = board[ChessBoard.RANK_1, ChessBoard.FILE_F] == null
                    && board[ChessBoard.RANK_1, ChessBoard.FILE_G] == null
            if (pathEmpty){
                // update king
                modifiedBoardArray[move.from.rank][move.from.file] = null
                modifiedBoardArray[move.to.rank][move.to.file] = pieceAtStartSquare

                // update rook
                modifiedBoardArray[ChessBoard.RANK_1][ChessBoard.FILE_H] = null
                modifiedBoardArray[ChessBoard.RANK_1][ChessBoard.FILE_F] = Rook(whitePlayer = true)

                // update game info
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(WhiteKingSideCastle)
                newCastlesRight.remove(WhiteQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            }
            else throw IllegalMoveException()
        } else if (isWhiteQueenSideCastle(move)) {
            modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null, nullityCount = modifiedGameInfo.nullityCount+1)
            val pathEmpty = board[ChessBoard.RANK_1, ChessBoard.FILE_D] == null
                    && board[ChessBoard.RANK_1, ChessBoard.FILE_C] == null
                    && board[ChessBoard.RANK_1, ChessBoard.FILE_B] == null
            if (pathEmpty){
                // update king
                modifiedBoardArray[move.from.rank][move.from.file] = null
                modifiedBoardArray[move.to.rank][move.to.file] = pieceAtStartSquare

                // update rook
                modifiedBoardArray[ChessBoard.RANK_1][ChessBoard.FILE_A] = null
                modifiedBoardArray[ChessBoard.RANK_1][ChessBoard.FILE_D] = Rook(whitePlayer = true)

                // update game info
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(WhiteKingSideCastle)
                newCastlesRight.remove(WhiteQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            }
            else throw IllegalMoveException()
        } else if (isBlackKingSideCastle(move)) {
            modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null, nullityCount = modifiedGameInfo.nullityCount+1)
            val pathEmpty = board[ChessBoard.RANK_8, ChessBoard.FILE_F] == null
                    && board[ChessBoard.RANK_8, ChessBoard.FILE_G] == null
            if (pathEmpty){
                // update king
                modifiedBoardArray[move.from.rank][move.from.file] = null
                modifiedBoardArray[move.to.rank][move.to.file] = pieceAtStartSquare

                // update rook
                modifiedBoardArray[ChessBoard.RANK_8][ChessBoard.FILE_H] = null
                modifiedBoardArray[ChessBoard.RANK_8][ChessBoard.FILE_F] = Rook(whitePlayer = false)

                // update game info
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(BlackKingSideCastle)
                newCastlesRight.remove(BlackQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            }
            else throw IllegalMoveException()
        } else if (isBlackQueenSideCastle(move)) {
            modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null, nullityCount = modifiedGameInfo.nullityCount+1)
            val pathEmpty = board[ChessBoard.RANK_8, ChessBoard.FILE_D] == null
                    && board[ChessBoard.RANK_8, ChessBoard.FILE_C] == null
                    && board[ChessBoard.RANK_8, ChessBoard.FILE_B] == null
            if (pathEmpty){
                // update king
                modifiedBoardArray[move.from.rank][move.from.file] = null
                modifiedBoardArray[move.to.rank][move.to.file] = pieceAtStartSquare

                // update rook
                modifiedBoardArray[ChessBoard.RANK_8][ChessBoard.FILE_A] = null
                modifiedBoardArray[ChessBoard.RANK_8][ChessBoard.FILE_D] = Rook(whitePlayer = false)

                // update game info
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(BlackKingSideCastle)
                newCastlesRight.remove(BlackQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            }
            else throw IllegalMoveException()
        } else { // regular move
            modifiedBoardArray[move.from.rank][move.from.file] = null
            modifiedBoardArray[move.to.rank][move.to.file] = pieceAtStartSquare

            val isPawnTwoCellsJump = (pieceAtStartSquare == Pawn(whitePlayer = true)
                    && deltaFile == 0 && deltaRank == 2 && move.from.rank == ChessBoard.RANK_2)
                    || (pieceAtStartSquare == Pawn(whitePlayer = false)
                    && deltaFile == 0 && deltaRank == -2 && move.from.rank == ChessBoard.RANK_7)

            if (isPawnTwoCellsJump) {
                modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = move.from.file, nullityCount = 0)
            } else {
                modifiedGameInfo = modifiedGameInfo.copy(enPassantFile = null)
            }

            if (pieceAtStartSquare::class == Pawn::class || capturingMove){
                modifiedGameInfo = modifiedGameInfo.copy(nullityCount = 0)
            } else {
                modifiedGameInfo = modifiedGameInfo.copy(nullityCount = modifiedGameInfo.nullityCount+1)
            }

            if (pieceAtStartSquare == King(whitePlayer = true)) {
                // update game info
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(WhiteKingSideCastle)
                newCastlesRight.remove(WhiteQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            } else if (pieceAtStartSquare == King(whitePlayer = false)) {
                // update game info
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(BlackKingSideCastle)
                newCastlesRight.remove(BlackQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            } else if (pieceAtStartSquare == Rook(whitePlayer = true)
                && move.from == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_H)) {
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(WhiteKingSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            } else if (pieceAtStartSquare == Rook(whitePlayer = true)
                && move.from == Coordinates(rank = ChessBoard.RANK_1, file = ChessBoard.FILE_A)) {
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(WhiteQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            } else if (pieceAtStartSquare == Rook(whitePlayer = false)
                && move.from == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_H)) {
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(BlackKingSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            } else if (pieceAtStartSquare == Rook(whitePlayer = false)
                && move.from == Coordinates(rank = ChessBoard.RANK_8, file = ChessBoard.FILE_A)) {
                val newCastlesRight = mutableListOf(*info.castles.toTypedArray())
                newCastlesRight.remove(BlackQueenSideCastle)
                modifiedGameInfo = modifiedGameInfo.copy(castles = newCastlesRight)
            }
        }

        val modifiedBoard = ChessBoard(modifiedBoardArray)

        return ChessGame(modifiedBoard, modifiedGameInfo)
    }

    fun copyBoardIntoArray() : Array<Array<ChessPiece?>> {
        val pieces = Array(8, { Array<ChessPiece?>(8, {null})})

        for (rank in 0..7){
            for (file in 0..7){
                pieces[rank][file] = board[rank, file]
            }
        }

        return pieces
    }
}

data class Move(val from: Coordinates, val to: Coordinates)

class NoPieceAtStartCellException : Exception()
class IllegalMoveException : Exception()
class WrongPromotionPieceColor: Exception()
class PlayerHasNoKingException: Exception()