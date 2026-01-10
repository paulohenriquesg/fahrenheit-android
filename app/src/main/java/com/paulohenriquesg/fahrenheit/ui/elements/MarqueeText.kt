package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A text composable that automatically scrolls horizontally (marquee) when focused
 * and the text is too long to fit in the available space.
 *
 * @param text The text to display
 * @param isFocused Whether the text should be scrolling (typically when the parent is focused)
 * @param style The text style to apply
 * @param color The text color
 * @param maxLines Maximum number of lines to display
 * @param scrollSpeed Scroll speed in dp per second (default 40dp/s for readability)
 * @param modifier Modifier to be applied to the text
 */
@Composable
fun MarqueeText(
    text: String,
    isFocused: Boolean,
    style: TextStyle,
    color: Color,
    maxLines: Int = 1,
    scrollSpeed: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var fullTextWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }

    // Determine if text overflows
    val textOverflows = remember(fullTextWidth, containerWidth, text) {
        fullTextWidth > containerWidth && containerWidth > 0
    }

    // Only animate if focused and text overflows
    val shouldAnimate = isFocused && textOverflows

    // Create new transition when animation should start/stop
    val infiniteTransition = rememberInfiniteTransition(
        label = "marquee_$shouldAnimate"  // Key changes when shouldAnimate changes
    )

    // Calculate animation duration based on text width and scroll speed
    // Duration = distance / speed (in milliseconds)
    val animationDuration = remember(fullTextWidth, scrollSpeed) {
        if (fullTextWidth > 0 && scrollSpeed.value > 0) {
            ((fullTextWidth / scrollSpeed.value) * 1000).toInt()
        } else {
            5000 // default 5 seconds
        }
    }

    // Animated offset
    val offsetX by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -fullTextWidth.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = animationDuration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "marquee_offset"
        )
    } else {
        remember { mutableStateOf(0f) }  // Static value when not animating
    }

    Box(
        modifier = modifier.clipToBounds()
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = if (shouldAnimate) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { result ->
                textLayoutResult = result
            },
            modifier = Modifier
                .layout { measurable, constraints ->
                    // Store container width for overflow detection
                    containerWidth = constraints.maxWidth

                    // ALWAYS measure with infinite width to get true fullTextWidth for overflow detection
                    val unconstrainedPlaceable = measurable.measure(
                        constraints.copy(maxWidth = Int.MAX_VALUE)
                    )
                    fullTextWidth = unconstrainedPlaceable.width

                    // Measure again with appropriate constraints for rendering
                    val renderConstraints = if (shouldAnimate) {
                        constraints.copy(maxWidth = Int.MAX_VALUE)  // Full width when animating
                    } else {
                        constraints  // Constrained when not animating
                    }
                    val placeable = measurable.measure(renderConstraints)

                    // Layout width: full when animating, constrained when not
                    val layoutWidth = if (shouldAnimate) {
                        placeable.width
                    } else {
                        constraints.maxWidth.coerceAtMost(placeable.width)
                    }

                    // Apply horizontal offset for animation
                    layout(layoutWidth, placeable.height) {
                        placeable.place(offsetX.toInt(), 0)
                    }
                }
        )

        // Render second copy of text for seamless loop (only when animating and offset is negative)
        if (shouldAnimate && offsetX < 0) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = maxLines,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        // Measure with infinite width for seamless loop
                        val placeable = measurable.measure(
                            constraints.copy(maxWidth = Int.MAX_VALUE)
                        )

                        // Position second text right after the first one with spacing
                        val secondTextOffset = offsetX + fullTextWidth + 32 // 32dp spacing

                        layout(placeable.width, placeable.height) {
                            placeable.place(secondTextOffset.toInt(), 0)
                        }
                    }
            )
        }
    }
}
