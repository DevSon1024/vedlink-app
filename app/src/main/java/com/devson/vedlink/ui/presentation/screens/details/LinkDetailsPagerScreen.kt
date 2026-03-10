package com.devson.vedlink.ui.presentation.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Gesture disambiguation NestedScrollConnection
    val gestureConnection = remember {
        object : NestedScrollConnection {
            // Track cumulative drag per gesture to decide direction
            var cumulativeX = 0f
            var cumulativeY = 0f
            var gestureDecided = false
            var isHorizontalGesture = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                cumulativeX += abs(available.x)
                cumulativeY += abs(available.y)

                if (!gestureDecided && (cumulativeX > 4f || cumulativeY > 4f)) {
                    // Require strictly horizontal: |X| > 1.5 × |Y|
                    isHorizontalGesture = cumulativeX > cumulativeY * 1.5f
                    gestureDecided = true
                }

                // Only consume horizontal scroll if gesture is horizontal
                return if (isHorizontalGesture) available else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Reset per-gesture tracking when motion stops
                if (available == Offset.Zero && consumed == Offset.Zero) {
                    cumulativeX = 0f
                    cumulativeY = 0f
                    gestureDecided = false
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                cumulativeX = 0f
                cumulativeY = 0f
                gestureDecided = false
                return if (isHorizontalGesture) available else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isHorizontalGesture = false
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        //  Pager 
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fill,
                beyondViewportPageCount = 1,          // Preload 1 adjacent page only
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(gestureConnection)
            ) { pageIndex ->
                val linkId = linkIds[pageIndex]
                // Only actively load data when the page is settled or ±1 adjacent.
                val shouldLoad = abs(pagerState.settledPage - pageIndex) <= 1

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
                    onPreviousPage = if (pageIndex > 0) { { scope.launch { pagerState.animateScrollToPage(pageIndex - 1) } } } else null,
                    onNextPage = if (pageIndex < linkIds.lastIndex) { { scope.launch { pagerState.animateScrollToPage(pageIndex + 1) } } } else null
                )
            }
        }
    }
