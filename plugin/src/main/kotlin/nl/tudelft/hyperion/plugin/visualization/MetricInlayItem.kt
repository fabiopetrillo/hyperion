package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

/**
 * Class that represents a metric visualized as an inlay.
 * It keeps track of the Inlay being shown and the RangeHighlighter that is used to track the beginning of the line.
 */
class MetricInlayItem(
    var inlay: Inlay<MetricTooltipRenderer>,
    val highlighter: RangeHighlighter
) {
    val isValid
        get() = highlighter.isValid

    val isProperlyPlaced
        get() = inlay.offset == highlighter.startOffset

    /**
     * Completely removes this Inlay visually.
     */
    fun remove() {
        if (highlighter.isValid) {
            highlighter.dispose()
        }

        if (inlay.isValid) {
            Disposer.dispose(inlay)
        }
    }
}

private val formatter = PeriodFormatterBuilder()
    .appendWeeks().appendSuffix(" w").appendSeparator(" ")
    .appendDays().appendSuffix(" d").appendSeparator(" ")
    .appendHours().appendSuffix(" h").appendSeparator(" ")
    .appendMinutes().appendSuffix(" min").appendSeparator(" ")
    .appendSeconds().appendSuffix(" s").appendSeparator(" ")
    .toFormatter()

/**
 * Converts an interval: count map into a string that represents this datapoint
 * for use in the block inlay label.
 */
fun countsToLabel(counts: Map<Int, Int>): String {
    return counts.toList().sortedBy { it.first }.map {
        val prettyTime = formatter.print(Period(it.first * 1000L).normalizedStandard())
        "[${it.second} last $prettyTime]"
    }.joinToString(" ")
}

/**
 * Creates and adds a new inlay with the specified trigger counts at the specified
 * line in the specified editor.
 */
fun createInlayForLine(editor: Editor, line: Int, counts: Map<Int, Int>): MetricInlayItem {
    val inlayOffset = calculateInlayOffset(editor, line)

    // Create a highlighter attached to this first element.
    val highlighter = editor.markupModel.addRangeHighlighter(
        inlayOffset,
        inlayOffset + 1,
        HighlighterLayer.HYPERLINK,
        TextAttributes(),
        HighlighterTargetArea.EXACT_RANGE
    )

    // Subtract 1 from the visible line number because the logical line is required
    highlighter.gutterIconRenderer = MetricGutterIconRenderer(line - 1)

    // And attach an inlay to that highlighter
    val inlay = createInlay(
        editor,
        inlayOffset,
        countsToLabel(
            counts
        ),
        highlighter
    )

    return MetricInlayItem(inlay, highlighter)
}

/**
 * Calculates at what offset the line starts in the given editor and returns this value.
 * @return the offset of the specified line in the specified editor.
 */
private fun calculateInlayOffset(editor: Editor, line: Int): Int {
    // Figure out the first character on the line.
    val startOffset = editor.document.getLineStartOffset(line - 1)
    val endOffset = editor.document.getLineEndOffset(line - 1)
    val lineText = editor.document.getText(TextRange(startOffset, endOffset))
    val inlayOffset = startOffset + lineText.indexOf(lineText.trim())
    return inlayOffset
}

/**
 * Creates and attaches a new BlockElement (Inlay) to the given editor at the given offset.
 * The optional text is displayed in the inlay using the given highlighter.
 *
 * @param editor The editor to attach the Inlay to.
 * @param offset The line offset where the inlay should be placed.
 * @param text Optional String that is displayed in the inlay.
 * @param highlighter The highlighter (renderer) that is used to render the inlay.
 *
 * @return the inlay that was created and attached to the editor.
 */
private fun createInlay(
    editor: Editor,
    offset: Int,
    text: String?,
    highlighter: RangeHighlighter
): Inlay<MetricTooltipRenderer> {
    return editor.inlayModel.addBlockElement(
        offset,
        false,
        true,
        1,
        MetricTooltipRenderer(text, highlighter)
    )!!
}

/**
 * Potentially recreates the inlay that belongs to the attached item,
 * depending on whether or not is is placed on the correct position.
 *
 * It is expected that the item is valid when this function is called.
 */
fun updateMetricInlayItem(editor: Editor, item: MetricInlayItem): MetricInlayItem {
    if (isFullyValid(item)) return item

    // Recreate the inlay at the current highlighter offset. We cannot
    // move it, as intellij does not support the movement of inlays.
    item.inlay = createInlay(
        editor,
        item.highlighter.startOffset,
        item.inlay.renderer.text,
        item.highlighter
    )

    return item
}

/**
 * Checks if the given MetricInlayItem is still valid or not.
 * @return Boolean value specifying if the item is valid or not.
 */
private fun isFullyValid(item: MetricInlayItem): Boolean {
    if (!item.isValid) {
        throw IllegalStateException("All items should be valid at this stage")
    }

    // Nothing to do.
    if (item.isProperlyPlaced) {
        return true
    }

    if (item.inlay.isValid) {
        Disposer.dispose(item.inlay)
    }
    return false
}
