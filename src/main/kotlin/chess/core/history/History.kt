package chess.core.history

import chess.core.game.ChessGame


class HistoryNode(val relatedPosition: ChessGame, val parentNode: HistoryNode?,
                  val moveLeadingToThisNodeFAN: String?) {
    init {
        parentNode?.addChild(this)
        if (parentNode != null) require(moveLeadingToThisNodeFAN != null)
        {"Only the root node can bypass the moveLeadingToThisNodeFAN parameter."}
    }

    private var _comment: String? = null

    private var _mainLineChild : HistoryNode? = null

    private val _variantsChildren = mutableListOf<HistoryNode>()

    fun setComment(comment: String) {
        _comment = comment
    }

    fun removeComment() {
        _comment = null
    }

    val comment: String?
        get() = _comment


    fun addChild(child: HistoryNode) {
        if (_mainLineChild == null) {
            _mainLineChild = child
            return
        }
        val childMoveNotAlreadyAdded = _mainLineChild!!.moveLeadingToThisNodeFAN != child.moveLeadingToThisNodeFAN
                && _variantsChildren.all { it.moveLeadingToThisNodeFAN != child.moveLeadingToThisNodeFAN }

        if (childMoveNotAlreadyAdded) _variantsChildren.add(child)
    }


    fun deleteThisLine() {
        val lineRoot = findLineRoot(this)
        val belongsToRootMainLine = lineRoot.parentNode == null
        if (belongsToRootMainLine) {
            lineRoot._variantsChildren.clear()
            lineRoot._mainLineChild = null
        } else {
            val lineRootChildIndexForThisLine = findLineRootChildIndexContainingThisNode()
            lineRoot._variantsChildren.removeAt(lineRootChildIndexForThisLine!!)
        }
    }


    private fun findLineRootChildIndexContainingThisNode(): Int? {
        fun searchForThisNodeInMainLineOf(place: HistoryNode): Boolean {
            if (place == this) return true
            if (place._mainLineChild == this) return true
            if (place._mainLineChild == null) return false
            return searchForThisNodeInMainLineOf(place._mainLineChild!!)
        }

        val lineRoot = findLineRoot()
        if (lineRoot._variantsChildren.isEmpty()) return null
        val lineRootChildIndexForThisLine = lineRoot._variantsChildren.map { searchForThisNodeInMainLineOf(it) }
            .withIndex().filter { (_, value) -> value }[0].index
        return lineRootChildIndexForThisLine
    }

    fun findLineRoot(): HistoryNode = findLineRoot(this)

    private fun findLineRoot(node: HistoryNode): HistoryNode {
        if (node.parentNode == null) return node
        if (node in node.parentNode._variantsChildren) return node.parentNode
        return findLineRoot(node.parentNode)
    }

    fun promoteThisLine() {
        val lineRoot = findLineRoot()
        if (lineRoot.parentNode == null) return

        val lineRootChildContainingThisNodeIndex = findLineRootChildIndexContainingThisNode()
        if (lineRootChildContainingThisNodeIndex == null) return


        val temp = lineRoot._variantsChildren[lineRootChildContainingThisNodeIndex]
        lineRoot._variantsChildren[lineRootChildContainingThisNodeIndex] = lineRoot._mainLineChild!!
        lineRoot._mainLineChild = temp
    }

    val mainLine: HistoryNode?
        get() = _mainLineChild

    val variants: List<HistoryNode>
        get() = _variantsChildren
}