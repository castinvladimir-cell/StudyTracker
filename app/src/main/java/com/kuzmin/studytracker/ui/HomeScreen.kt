package com.kuzmin.studytracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuzmin.studytracker.data.SortMode
import com.kuzmin.studytracker.data.Store
import com.kuzmin.studytracker.data.Subject
import com.kuzmin.studytracker.data.TAB_CURRENT
import com.kuzmin.studytracker.data.sortSubjects
import com.kuzmin.studytracker.ui.theme.Palette
import kotlin.math.roundToInt

private val CardHeight = 108.dp
private val CardShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    store: Store,
    avScope: AnimatedVisibilityScope,
    tab: Int,
    onTabChange: (Int) -> Unit,
    listStates: List<LazyListState>,
    scrollToId: Long?,
    onScrolled: () -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onMove: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var openSwipe by remember { mutableStateOf<Long?>(null) }
    BackHandler(enabled = openSwipe != null) { openSwipe = null }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TabsBar(
                tab = tab,
                counts = intArrayOf(
                    store.subjects.count { it.tab == 0 },
                    store.subjects.count { it.tab == 1 },
                ),
                onTabChange = {
                    openSwipe = null
                    onTabChange(it)
                },
                sortMode = store.sortMode,
                progressDescending = store.progressDescending,
                onSort = store::selectSort,
            )

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(320)) { it * dir / 4 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(320)) { -it * dir / 4 } + fadeOut(tween(200)))
                },
                label = "tabs",
                modifier = Modifier.weight(1f),
            ) { t ->
                val items = remember(store.subjects, t, store.sortMode, store.progressDescending) {
                    sortSubjects(store.subjects.filter { it.tab == t }, store.sortMode, store.progressDescending)
                }
                val listState = listStates[t]

                LaunchedEffect(scrollToId, items) {
                    val idx = items.indexOfFirst { it.id == scrollToId }
                    if (scrollToId != null && idx >= 0) {
                        listState.animateScrollToItem(idx)
                        onScrolled()
                    }
                }

                Crossfade(targetState = items.isEmpty(), label = "empty") { empty ->
                    if (empty) {
                        EmptyState(t)
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 12.dp,
                                bottom = 100.dp + navBottom,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items, key = { it.id }) { s ->
                                SwipeReveal(
                                    open = openSwipe == s.id,
                                    onOpenChange = { isOpen ->
                                        openSwipe = if (isOpen) s.id else if (openSwipe == s.id) null else openSwipe
                                    },
                                    modifier = Modifier.animateItem(),
                                    actions = {
                                        SwipeAction("Изменить") {
                                            openSwipe = null
                                            onEdit(s.id)
                                        }
                                        SwipeActionDivider()
                                        SwipeAction(if (s.tab == TAB_CURRENT) "В долг" else "В текущие") {
                                            openSwipe = null
                                            onMove(s.id)
                                        }
                                        SwipeActionDivider()
                                        SwipeAction("Удалить", Palette.Danger) {
                                            openSwipe = null
                                            onDelete(s.id)
                                        }
                                    },
                                ) {
                                    SubjectCard(
                                        subject = s,
                                        avScope = avScope,
                                        onClick = {
                                            if (openSwipe != null) openSwipe = null else onOpen(s.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Кнопка «плюс»: пружинит при нажатии и улетает при открытии предмета
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
            label = "fab",
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp + navBottom)
                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 2f)
                .then(
                    with(avScope) {
                        Modifier.animateEnterExit(
                            enter = scaleIn(tween(300)) + fadeIn(tween(300)),
                            exit = scaleOut(tween(220)) + fadeOut(tween(220)),
                        )
                    }
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(64.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Palette.Accent)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                ) {
                    openSwipe = null
                    onAdd()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Добавить предмет",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.SubjectCard(
    subject: Subject,
    avScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .sharedBounds(
                rememberSharedContentState("card-${subject.id}"),
                avScope,
                boundsTransform = CardBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(CardShape),
            )
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Card)
            .border(1.dp, Palette.CardBorder, CardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .skipToLookaheadSize()
                .fillMaxWidth()
                .height(CardHeight)
        ) {
            SubjectImage(
                path = subject.image,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState("img-${subject.id}"),
                        avScope,
                        boundsTransform = CardBoundsTransform,
                        zIndexInOverlay = 1f,
                    )
                    .size(CardHeight)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 13.dp)
            ) {
                Text(
                    text = subject.name,
                    color = Palette.Text,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                    maxLines = if (subject.teacher.isBlank()) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subject.teacher.isNotBlank()) {
                    Text(
                        text = subject.teacher,
                        color = Palette.Sub,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                val deadline = subject.nearestDeadline
                when {
                    subject.isFinished -> Text("Всё сдано", color = Palette.Fill, fontSize = 13.sp)
                    deadline != null -> DeadlineText(deadline)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    ProgressLine(
                        progress = subject.progress,
                        modifier = Modifier
                            .weight(1f)
                            .sharedElement(
                                rememberSharedContentState("prog-${subject.id}"),
                                avScope,
                                boundsTransform = CardBoundsTransform,
                            ),
                    )
                    val pct by animateIntAsState((subject.progress * 100).roundToInt(), label = "pct")
                    Text(
                        text = "$pct%",
                        color = Palette.Sub,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabsBar(
    tab: Int,
    counts: IntArray,
    onTabChange: (Int) -> Unit,
    sortMode: SortMode,
    progressDescending: Boolean,
    onSort: (SortMode) -> Unit,
) {
    val density = LocalDensity.current
    val bounds = remember { mutableStateListOf(0f to 0f, 0f to 0f) }
    val indicatorX by animateFloatAsState(
        bounds[tab].first,
        spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "ix",
    )
    val indicatorW by animateFloatAsState(
        bounds[tab].second,
        spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "iw",
    )
    var menu by remember { mutableStateOf(false) }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("Текущий предмет", "Долг").forEachIndexed { i, label ->
                    val color by animateColorAsState(
                        if (i == tab) Palette.Text else Palette.TabIdle,
                        label = "tabColor",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .onGloballyPositioned { c ->
                                bounds[i] = c.positionInParent().x to c.size.width.toFloat()
                            }
                            .clickable { onTabChange(i) }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        if (counts[i] > 0) {
                            Text(
                                text = counts[i].toString(),
                                color = Palette.Sub,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(indicatorX.roundToInt(), 0) }
                    .width(with(density) { indicatorW.toDp() })
                    .height(3.dp)
                    .background(Palette.Text)
            )

            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
            ) {
                IconButton(onClick = { menu = true }) {
                    SortIcon(Palette.TabIdle)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    SortItem("По дате добавления", sortMode == SortMode.ADDED) {
                        onSort(SortMode.ADDED); menu = false
                    }
                    SortItem(
                        if (sortMode == SortMode.PROGRESS && !progressDescending) "По прогрессу: меньше сверху"
                        else "По прогрессу: больше сверху",
                        sortMode == SortMode.PROGRESS,
                    ) {
                        onSort(SortMode.PROGRESS); menu = false
                    }
                    SortItem("По сроку: ближайшие сверху", sortMode == SortMode.DEADLINE) {
                        onSort(SortMode.DEADLINE); menu = false
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Palette.Divider)
        )
    }
}

@Composable
private fun SortItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(text, color = if (selected) Palette.Text else Palette.TextDim, fontSize = 15.sp)
        },
        onClick = onClick,
        trailingIcon = {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.Fill)
        },
    )
}

@Composable
private fun EmptyState(tab: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PencilArt(
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Text(
            text = if (tab == TAB_CURRENT) "Здесь пока пусто" else "Долгов нет",
            color = Palette.Text,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = "Нажми +, чтобы добавить предмет",
            color = Palette.Sub,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
