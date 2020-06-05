package nl.tudelft.hyperion.plugin.graphs

import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.geom.AffineTransform
import javax.swing.JPanel
import kotlin.math.PI
import kotlin.math.round

/**
 * Short alias for 2D arrays.
 */
typealias Array2D<T> = Array<Array<T>>

/**
 * Represents and x, y tuple of coordinates.
 */
typealias Index2D = Pair<Int, Int>

/**
 * Represents all data necessary for a histogram, is composed of the counts for
 * each box, the list of timestamps and the color of each box
 */
typealias HistogramData = Triple<Array2D<Int>, Array<String>, Array2D<Color>>

class InteractiveHistogram(
    initialVals: Array2D<Int>,
    private val xMargin: Int,
    private var startY: Int,
    private var endY: Int,
    var barSpacing: Int,
    var colors: Array2D<Color>,
    var labels: Array<String>,
    var timestamps: Array<String>
) : JPanel(true) {

    var bars: Array<Bar> = initialVals.map { Bar(it.size) }.toTypedArray()
    var boxCollisions: List<Index2D> = listOf()
    var isCurrentlyColored = false

    private val rotatedFont: Font

    /**
     * A 2D array of the histogram values.
     * Change array size of [bars] if this value changes.
     */
    var vals = initialVals
        set(value) {
            field = value

            // Create array of uninitialized boxes
            bars = value.map { Bar(it.size) }.toTypedArray()
        }

    init {
        this.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent?) {
                boxCollisions = checkCollide(e?.x!!, e.y)

                // Only redraw if the cursor is hovering over any of the boxes
                if (boxCollisions.isNotEmpty()) {
                    // TODO: make it only repaint the rect of what needs to be redrawn
                    //  instead of the entire JPanel
                    this@InteractiveHistogram.repaint()
                    isCurrentlyColored = true
                } else if (isCurrentlyColored) {
                    this@InteractiveHistogram.repaint()
                    isCurrentlyColored = false
                }
            }
        })

        font = Font("Monospaced", Font.PLAIN, 10)

        val affineTransform = AffineTransform()
        affineTransform.rotate(-PI / 2, 0.0, 0.0)
        rotatedFont = font.deriveFont(affineTransform)
    }

    companion object {
        const val Y_LABEL = "count"

        // Color the overlay with a transparent gray
        val OVERLAY_COLOR = Color(0.8F, 0.8F, 0.8F, 0.6F)
    }

    fun update(parameters: HistogramData) {
        vals = parameters.first
        timestamps = parameters.second
        colors = parameters.third
        repaint()
    }

    private fun calculateBoxes() {
        val histogramWidth = width - 2 * xMargin
        val barWidth = histogramWidth / vals.size

        val maxBarTotal = vals.map(Array<Int>::sum).max()
        val barHeightScale = (startY - endY) / maxBarTotal!!.toDouble()

        for ((i, bar) in bars.withIndex()) {
            var prevY = 0
            val leftMargin = xMargin + i * barWidth

            bar.startX = leftMargin + barSpacing / 2
            bar.width = barWidth - barSpacing

            for ((j, box) in bar.boxes.withIndex()) {
                val currentY = round(vals[i][j] * barHeightScale).toInt()

                box.startY = startY - (prevY + currentY)
                box.height = currentY

                prevY += currentY
            }
        }
    }

    @SuppressWarnings("MagicNumber")
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        g.color = Color.GRAY
        g.drawLine(xMargin, startY, this.width - xMargin, startY)
        g.drawLine(xMargin, startY, xMargin, endY)

        val g2 = g as Graphics2D

        // Set anti aliasing
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.font = rotatedFont
        val yLabelLength = g.getFontMetrics(font).stringWidth(Y_LABEL)
        g2.drawString(Y_LABEL, xMargin - 5, endY + (startY - endY) / 2 + yLabelLength / 2)
        g2.font = font

        drawHistogram(g)
    }

    private fun drawHistogram(g: Graphics) {
        // TODO: only recalculate boxes on component show or component resize
        calculateBoxes()

        // TODO: change to some sort of Bar data class to track individual bars
        val barWidth = width / vals.size

        for ((i, bar) in bars.withIndex()) {

            // Draw timestamps
            val xLabelFontMetrics = g.getFontMetrics(font)
            g.color = Color.GRAY
            g.drawString(
                timestamps[i],
                bar.startX + bar.width - xLabelFontMetrics.stringWidth(timestamps[i]) / 2,
                startY + xLabelFontMetrics.height
            )

            // Draw bars
            for ((j, box) in bar.boxes.withIndex()) {
                g.color = colors[i][j]
                g.fillRect(bar.startX, box.startY, bar.width, box.height)

                // Draw relevant information if the user is hovering over this box
                if (boxCollisions.filter { it.first == i && it.second == j }.any()) {
                    drawBoxOverlay(g, bar, box, labels[j], vals[i][j].toString())
                }
            }
        }
    }

    @SuppressWarnings("MagicNumber")
    private fun drawBoxOverlay(g: Graphics, bar: Bar, box: Box, label: String, labelVal: String) {
        // Color the overlay with a transparent gray
        g.color = OVERLAY_COLOR
        g.fillRect(bar.startX, box.startY, bar.width, box.height)

        g.color = Color.GRAY
        g.drawString(label, bar.startX, bar.boxes.last().startY - 20)
        g.drawString("$Y_LABEL=$labelVal", bar.startX, bar.boxes.last().startY - 10)
    }

    private fun checkCollide(x: Int, y: Int): List<Index2D> {
        val results = mutableListOf<Index2D>()

        for ((i, bar) in bars.withIndex()) {
            for ((j, box) in bar.boxes.withIndex()) {
                if (x > bar.startX && x <= bar.startX + bar.width &&
                    y > box.startY && y <= box.startY + box.height) {
                    results.add(Pair(i, j))
                }
            }
        }

        return results
    }
}

/**
 * Represents a single bar in a histogram composed of multiple levels of boxes.
 * Stores the X parameters and the child [Box]-s.
 */
data class Bar(
    var startX: Int = 0,
    var width: Int = 0,
    var boxes: List<Box>
) {
    /**
     * Fills [boxes] with [boxCount] of default boxes.
     */
    constructor(boxCount: Int) : this(boxes = (0 until boxCount).map { Box() })
}

/**
 * Represents a single box which tracks the Y coordinate and height, the X
 * parameters are tracked by the parent [Bar].
 */
data class Box(
    var startY: Int = 0,
    var height: Int = 0
)
