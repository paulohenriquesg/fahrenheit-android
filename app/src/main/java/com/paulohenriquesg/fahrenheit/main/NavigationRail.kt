package com.paulohenriquesg.fahrenheit.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.paulohenriquesg.fahrenheit.navigation.MenuItem
import com.paulohenriquesg.fahrenheit.ui.Space

/** The test id for a section, so a device script can name it instead of counting presses. */
fun menuItemTestTag(id: String): String = "menu_item_$id"

/**
 * The sections, always on screen.
 *
 * The phone's ModalNavigationDrawer was invisible until opened and covered the
 * screen when it was, so there was nothing at the left edge for a D-pad to
 * reach (#58). The TV drawer keeps a rail of icons in the layout and widens to
 * show labels when focus enters it, which is what gives LEFT somewhere to go.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NavigationRail(
    items: List<MenuItem>,
    selectedId: String,
    onSelect: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    secondary: List<MenuItem> = emptyList(),
    firstFocus: FocusRequester? = null,
    onRailFocusChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // The drawer widens itself when focus *enters* the sheet, which does not
    // happen when focus starts there: on the device the labels stayed hidden
    // and the sections were navigated blind. So say it outright - the rail is
    // open while it holds focus.
    var railHasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(railHasFocus) {
        drawerState.setValue(if (railHasFocus) DrawerValue.Open else DrawerValue.Closed)
    }

    NavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = { drawerValue ->
            Column(
                modifier = Modifier
                    .onFocusChanged {
                        railHasFocus = it.hasFocus
                        onRailFocusChanged(it.hasFocus)
                    }
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Space.gap),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item -> Section(item, selectedId, drawerValue, onSelect, firstFocus) }
                if (secondary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Space.gap))
                    secondary.forEach { item ->
                        Section(item, selectedId, drawerValue, onSelect, firstFocus)
                    }
                }
            }
        },
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerScope.Section(
    item: MenuItem,
    selectedId: String,
    drawerValue: DrawerValue,
    onSelect: (MenuItem) -> Unit,
    firstFocus: FocusRequester?
) {
    val selected = item.id == selectedId
    NavigationDrawerItem(
        selected = selected,
        onClick = { onSelect(item) },
        leadingContent = { Icon(imageVector = item.icon, contentDescription = null) },
        modifier = Modifier
            .testTag(menuItemTestTag(item.id))
            // Arrival focus goes to the section you are in: the one target
            // every view is guaranteed to have.
            .then(if (selected && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier)
    ) {
        // The label only exists while the rail is open, so a closed rail is
        // icons and nothing is clipped.
        AnimatedVisibility(visible = drawerValue == DrawerValue.Open) {
            Text(text = item.label)
        }
    }
}
