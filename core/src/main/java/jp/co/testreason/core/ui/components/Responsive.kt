package jp.co.testreason.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Readable line length on tablets and landscape phones. */
val MaxContentWidth: Dp = 720.dp

/** From this width a screen may show two panes side by side. */
val TwoPaneMinWidth: Dp = 720.dp

private val ScreenPadding = 16.dp
private val SectionSpacing = 16.dp

/**
 * Vertically scrolling screen body, centred and capped at [MaxContentWidth] so that wide screens do
 * not stretch text and buttons across the whole display.
 */
@Composable
fun ResponsiveScreenColumn(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            content = content
        )
    }
}

/**
 * Shows [primary] and [secondary] side by side when the available width allows it
 * (each pane scrolls on its own), otherwise stacked in one scrolling column.
 */
@Composable
fun ResponsiveTwoPane(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    primary: @Composable ColumnScope.() -> Unit,
    secondary: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (maxWidth >= TwoPaneMinWidth) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(SectionSpacing)
            ) {
                PaneColumn(Modifier.weight(1f), primary)
                PaneColumn(Modifier.weight(1f), secondary)
            }
        } else {
            ResponsiveScreenColumn(contentPadding = PaddingValues(0.dp)) {
                primary()
                secondary()
            }
        }
    }
}

@Composable
private fun PaneColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        content = content
    )
}
