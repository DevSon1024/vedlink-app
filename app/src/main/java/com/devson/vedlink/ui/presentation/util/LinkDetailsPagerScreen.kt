package com.devson.vedlink.ui.presentation.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.vedlink.ui.presentation.screens.LinkDetailsScreen
import com.devson.vedlink.ui.viewmodel.LinkDetailsViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkDetailsPagerScreen(
    initialLinkId: Int,
    linkIds: List<Int>,
    onNavigateBack: () -> Unit
) {
    val initialPage = linkIds.indexOf(initialLinkId).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { linkIds.size }
    )
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        //  Pager 
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fill,
            beyondViewportPageCount = 1,          // Preload 1 adjacent page only
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val linkId = linkIds[pageIndex]
            
            // Only actively load data when the page is settled or ±1 adjacent.
            // Use derivedStateOf to avoid recomposing the entire pager page when settledPage changes during swipes.
            val shouldLoad by remember(pagerState) {
                derivedStateOf {
                    abs(pagerState.settledPage - pageIndex) <= 1
                }
            }

            // Each page gets its own ViewModel instance keyed to linkId
            val pageViewModel: LinkDetailsViewModel =
                hiltViewModel(key = "details_$linkId")

            LaunchedEffect(linkId, shouldLoad) {
                if (shouldLoad) {
                    pageViewModel.loadLink(linkId)
                }
            }

            LinkDetailsScreen(
                linkId = linkId,
                onNavigateBack = onNavigateBack,
                viewModel = pageViewModel,
                pageText = "${pageIndex + 1} / ${linkIds.size}",
                onPreviousPage = if (pageIndex > 0) {
                    { scope.launch { pagerState.animateScrollToPage(pageIndex - 1) } }
                } else null,
                onNextPage = if (pageIndex < linkIds.lastIndex) {
                    { scope.launch { pagerState.animateScrollToPage(pageIndex + 1) } }
                } else null
            )
        }
    }
}
