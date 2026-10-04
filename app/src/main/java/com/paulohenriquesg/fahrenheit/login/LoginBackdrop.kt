package com.paulohenriquesg.fahrenheit.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.elements.CoverWall

/**
 * What the login screens sit on (#161, docs/mocks/login-style.html): the
 * player's look rather than flat black.
 *
 * - [covers] from this device: the drifting wall, shaded from the left so
 *   the form over it stays readable.
 * - None (a first run, a different server, or nothing cached): a soft field
 *   in the app's own colours, since nothing about the library is known.
 *
 * Both are drawn from the theme's background, so the light theme gets a
 * light version rather than a dark blob.
 */
@Composable
fun LoginBackdrop(covers: List<ImageBitmap>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    if (covers.isNotEmpty()) {
        Box(modifier.fillMaxSize().background(scheme.background).testTag("login_backdrop_covers")) {
            CoverWall(covers, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to scheme.background.copy(alpha = 0.96f),
                            0.46f to scheme.background.copy(alpha = 0.88f),
                            1f to scheme.background.copy(alpha = 0.35f)
                        )
                    )
            )
        }
    } else {
        Box(
            modifier
                .fillMaxSize()
                .background(scheme.background)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(scheme.primary.copy(alpha = 0.30f), scheme.primary.copy(alpha = 0f)),
                            center = Offset(size.width * 0.75f, size.height * 0.35f),
                            radius = size.width * 0.55f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(scheme.secondary.copy(alpha = 0.22f), scheme.secondary.copy(alpha = 0f)),
                            center = Offset(size.width * 0.9f, size.height * 0.85f),
                            radius = size.width * 0.5f
                        )
                    )
                }
                .testTag("login_backdrop_field")
        )
    }
}
