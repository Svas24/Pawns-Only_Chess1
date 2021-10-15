package chess

import kotlin.math.absoluteValue

class Position(val col: Int, val row: Int)

class Side(val color: String, val startRow: Int, val moveDirection: Int, val playerName: String) {
    val pawnChar: Char = color[0]
    lateinit var opposite: Side
    private var activeFirstMovePawnColumn = -1
    fun setFirstMoveColumn(col: Int) {activeFirstMovePawnColumn = col}
    fun resetFirstMoveColumn() { activeFirstMovePawnColumn = -1}
    fun isFirstMoveColumn(col: Int) = col == activeFirstMovePawnColumn
    init { Board.fillRow(startRow, this) }
}

enum class Condition{ WRONG_MOVE, MOVE, FIRST_MOVE, CAPTURE, CAPTURE_ALL, EN_PASSENT, STALEMATE, PROMOTION }

object Board {
    private val squares = CharArray(64){' '}
    private fun idxOf(col: Int, row:Int): Int = row * 8 + col
    fun pawnCharAt(col: Int, row: Int): Char = squares[idxOf(col, row)]
    fun isEpmty(col: Int, row: Int): Boolean = squares[idxOf(col, row)] == ' '
    fun hasPawns(side: Side): Boolean = squares.contains(side.pawnChar)
    fun fillRow(row: Int, side: Side) { squares.fill(side.pawnChar, row * 8, (row + 1) * 8) }
    fun remove(col: Int, row: Int){ squares[idxOf(col, row)] = ' '}

    fun move(from: Position, to: Position) {
        squares[idxOf(to.col, to.row)] = squares[idxOf(from.col, from.row)]
        squares[idxOf(from.col, from.row)] = ' '
    }

    fun getPawns(side: Side): MutableSet<Position> {
        val result = mutableSetOf<Position>()
        for (idx in 0..63)
            if (squares[idx] == side.pawnChar)
                result.add(Position(idx % 8, idx / 8))
        return result
    }

    fun print() {
        for (row in 7 downTo 0)
            println("  +---+---+---+---+---+---+---+---+\n${row + 1}${squares.slice((row * 8)..(row * 8 + 7))
                .joinToString(" | ", " | ", " | ")}")
        println("  +---+---+---+---+---+---+---+---+ \n    a   b   c   d   e   f   g   h \n")
    }
}

object Rules{

    fun move(side: Side, from: Position, to: Position): Condition {
        if(from.col == to.col && Board.isEpmty(from.col, from.row + side.moveDirection)) {  // may be move
            if (to.row == from.row + side.moveDirection) {                                       // move
                Board.move(from, to)
                side.opposite.resetFirstMoveColumn()
                if (to.row == side.startRow + 6 * side.moveDirection) return Condition.PROMOTION
                if (!hasMove(side.opposite)) return Condition.STALEMATE
                return Condition.MOVE
            }
            if (from.row == side.startRow && from.row + side.moveDirection * 2 == to.row
                && Board.isEpmty(to.col, to.row)) {                                             // first move
                side.setFirstMoveColumn(from.col)
                Board.move(from, to)
                side.opposite.resetFirstMoveColumn()
                if (!hasMove(side.opposite)) return Condition.STALEMATE
                return Condition.FIRST_MOVE
            }
        }
        if((to.col - from.col).absoluteValue == 1 && to.row == from.row + side.moveDirection) { // may be capture
            if (Board.pawnCharAt(to.col, to.row) == side.opposite.pawnChar) {                   // capture
                Board.move(from, to)
                side.opposite.resetFirstMoveColumn()
                if (!Board.hasPawns(side.opposite)) return Condition.CAPTURE_ALL
                if (!hasMove(side.opposite)) return Condition.STALEMATE
                return Condition.CAPTURE
            }
            if (to.row == side.opposite.startRow + side.opposite.moveDirection
                && side.opposite.isFirstMoveColumn(to.col)) {                                   // en passent
                Board.move(from, to)
                side.opposite.resetFirstMoveColumn()
                Board.remove(to.col, to.row - side.opposite.moveDirection)
                if (!Board.hasPawns(side.opposite)) return Condition.CAPTURE_ALL
                if (!hasMove(side.opposite)) return Condition.STALEMATE
                return Condition.EN_PASSENT
            }
        }
        return Condition.WRONG_MOVE
    }

    private fun hasMove(side: Side): Boolean {
        fun hasTarget(opposite: Side, targetCol: Int, targetRow: Int): Boolean =
            Board.pawnCharAt(targetCol, targetRow) == opposite.pawnChar ||                          //target pawn
                    (Board.isEpmty(targetCol, targetRow) && opposite.isFirstMoveColumn(targetCol))  //en passent

        for (pos in Board.getPawns(side)) {
            val nextRow = pos.row + side.moveDirection
            if (Board.isEpmty(pos.col, nextRow)) return true                                          // can step
            if (pos.col != 7 && hasTarget(side.opposite, pos.col + 1, nextRow)) return true   // right target
            if (pos.col != 0 && hasTarget(side.opposite, pos.col - 1, nextRow)) return true   // left target
        }
        return false
    }
}


fun main() {
    println("Pawns-Only Chess")
    println ("First Player's name:")
    val whites = Side("White", 1, 1, readLine()!!)
    println ("Second Player's name:")
    val blacks = Side("Black", 6, -1, readLine()!!)
    whites.opposite = blacks
    blacks.opposite = whites
    var currentSide = whites
    Board.print()

    while (true) {
        println("${currentSide.playerName}'s turn:")
        val input = readLine()!!.lowercase()
        if (input == "exit") { println("Bye!"); return }
        if (!input.matches("[a-h][2-7][a-h][1-8]".toRegex())) { println("Invalid Input"); continue }
        val from = Position(input[0] - 'a', input[1] - '1')
        val to = Position(input[2] - 'a', input[3] - '1')
        if (Board.pawnCharAt(from.col, from.row) != currentSide.pawnChar) {
            println("No ${currentSide.color.lowercase()} pawn at ${input.substring(0..1)}")
            continue
        }
        when(Rules.move(currentSide, from, to)) {
            Condition.WRONG_MOVE -> {
                println("Invalid Input")
                continue
            }
            Condition.PROMOTION, Condition.CAPTURE_ALL -> {
                Board.print()
                println("${currentSide.color} Wins!\nBye!")
                return
            }
            Condition.STALEMATE -> {
                Board.print()
                println("Stalemate!\nBye!")
                return
            }
            else -> {     // MOVE, FIRST_MOVE, CAPTURE, EN_PASSENT
                Board.print()
                currentSide = currentSide.opposite
            }
        }
    }
}

