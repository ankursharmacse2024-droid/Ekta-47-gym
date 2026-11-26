package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MonthlyRevenueRecord
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EktaNavy
import com.example.ui.theme.EktaRoyalBlue
import com.example.ui.theme.EktaSkyBlue
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusExpiredAccent
import com.example.ui.theme.StatusExpiredBg
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TabularMetricStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

private enum class RevenueChartMode {
    AREA,
    BARS
}

@Composable
fun MonthlyRevenueTrendChartCard(
    records: List<MonthlyRevenueRecord>,
    onSyncCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        EmptyStatePanel(
            title = "No Historical Revenue Yet",
            body = "Sync your current month snapshot to start tracking multi-month Firestore revenue trends.",
            actionLabel = "Sync Current Month",
            onAction = onSyncCurrentMonth,
            modifier = modifier
        )
        return
    }

    var chartMode by remember { mutableStateOf(RevenueChartMode.AREA) }
    var selectedIndex by remember(records.size) {
        mutableIntStateOf((records.size - 1).coerceAtLeast(0))
    }
    val safeIndex = selectedIndex.coerceIn(0, records.lastIndex)
    val selectedRecord = records[safeIndex]
    val previousRecord = if (safeIndex > 0) records[safeIndex - 1] else null
    val momDeltaPct = remember(selectedRecord, previousRecord) {
        val prev = previousRecord?.collectedCents ?: 0L
        if (prev <= 0L) null
        else (((selectedRecord.collectedCents - prev) * 100.0) / prev)
    }

    val maxCents = remember(records) {
        val rawMax = records.maxOf {
            maxOf(it.collectedCents, it.outstandingCents, it.autoPayCents)
        }.coerceAtLeast(10000L)
        // Round up to clean $200 step
        val step = 20000L
        ((rawMax + step - 1) / step) * step
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_revenue_trend_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Mode Toggle + Sync Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONTHLY REVENUE TRENDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "6-Month Firestore History",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Segmented Area / Bar toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCanvas)
                            .padding(2.dp)
                    ) {
                        ChartModeButton(
                            selected = chartMode == RevenueChartMode.AREA,
                            icon = Icons.Rounded.ShowChart,
                            contentDescription = "Area trend chart",
                            onClick = { chartMode = RevenueChartMode.AREA },
                            tag = "chart_mode_area"
                        )
                        ChartModeButton(
                            selected = chartMode == RevenueChartMode.BARS,
                            icon = Icons.Rounded.BarChart,
                            contentDescription = "Grouped bar chart",
                            onClick = { chartMode = RevenueChartMode.BARS },
                            tag = "chart_mode_bars"
                        )
                    }

                    TextButton(
                        onClick = onSyncCurrentMonth,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("sync_month_snapshot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = EktaSkyBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sync",
                            style = MaterialTheme.typography.labelMedium,
                            color = EktaSkyBlue
                        )
                    }
                }
            }

            // Interactive Tooltip Callout Banner for Selected Month
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCanvas,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedRecord.monthLabel} (${selectedRecord.monthKey})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = TextPrimary
                        )
                        if (momDeltaPct != null) {
                            val isPositive = momDeltaPct >= 0.0
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isPositive) StatusActiveBg else StatusExpiredBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                val sign = if (isPositive) "+" else ""
                                Text(
                                    text = "$sign${String.format("%.1f", momDeltaPct)}% MoM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (isPositive) StatusActiveText else StatusExpiredText
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TooltipMetric(
                            label = "COLLECTED",
                            value = formatCurrencyCents(selectedRecord.collectedCents),
                            color = EktaSkyBlue
                        )
                        TooltipMetric(
                            label = "AUTO-PAY",
                            value = formatCurrencyCents(selectedRecord.autoPayCents),
                            color = EktaRoyalBlue
                        )
                        TooltipMetric(
                            label = "OUTSTANDING",
                            value = formatCurrencyCents(selectedRecord.outstandingCents),
                            color = StatusExpiredAccent
                        )
                    }
                }
            }

            // Native Jetpack Compose Canvas Chart
            var canvasWidthPx by remember { mutableIntStateOf(1) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .height(190.dp)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val steps = listOf(maxCents, (maxCents * 2) / 3, maxCents / 3, 0L)
                    steps.forEach { cents ->
                        Text(
                            text = formatCurrencyCents(cents),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(190.dp)
                        .onSizeChanged { canvasWidthPx = it.width.coerceAtLeast(1) }
                        .pointerInput(records.size, canvasWidthPx) {
                            detectTapGestures { offset ->
                                val idx = mapXToIndex(offset.x, canvasWidthPx.toFloat(), records.size)
                                selectedIndex = idx
                            }
                        }
                        .pointerInput(records.size, canvasWidthPx) {
                            detectHorizontalDragGestures { change, _ ->
                                val idx = mapXToIndex(change.position.x, canvasWidthPx.toFloat(), records.size)
                                selectedIndex = idx
                            }
                        }
                        .testTag("revenue_trend_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val topPad = 14.dp.toPx()
                        val bottomPad = 14.dp.toPx()
                        val horizPad = 14.dp.toPx()
                        val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                        val plotWidth = (size.width - horizPad * 2f).coerceAtLeast(1f)

                        // Horizontal dashed gridlines
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        for (i in 0..3) {
                            val y = topPad + (plotHeight * i / 3f)
                            drawLine(
                                color = BorderSubtle,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i < 3) dashEffect else null
                            )
                        }

                        val count = records.size
                        fun xFor(index: Int): Float {
                            return if (count <= 1) horizPad + plotWidth / 2f
                            else horizPad + (plotWidth * index / (count - 1).toFloat())
                        }

                        fun yFor(cents: Long): Float {
                            val ratio = (cents.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f)
                            return topPad + plotHeight * (1f - ratio)
                        }

                        // Vertical guide line at selected month
                        val selX = if (chartMode == RevenueChartMode.AREA) {
                            xFor(safeIndex)
                        } else {
                            val slotW = plotWidth / count.toFloat()
                            horizPad + slotW * safeIndex + slotW / 2f
                        }
                        drawLine(
                            color = EktaNavy.copy(alpha = 0.25f),
                            start = Offset(selX, topPad),
                            end = Offset(selX, topPad + plotHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        if (chartMode == RevenueChartMode.AREA) {
                            // Build Collected Revenue smooth curve & gradient fill
                            val collectedPoints = records.mapIndexed { idx, rec ->
                                Offset(xFor(idx), yFor(rec.collectedCents))
                            }
                            val autoPayPoints = records.mapIndexed { idx, rec ->
                                Offset(xFor(idx), yFor(rec.autoPayCents))
                            }
                            val outstandingPoints = records.mapIndexed { idx, rec ->
                                Offset(xFor(idx), yFor(rec.outstandingCents))
                            }

                            val areaPath = Path().apply {
                                moveTo(collectedPoints.first().x, topPad + plotHeight)
                                lineTo(collectedPoints.first().x, collectedPoints.first().y)
                                for (i in 0 until collectedPoints.lastIndex) {
                                    val p0 = collectedPoints[i]
                                    val p1 = collectedPoints[i + 1]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                                lineTo(collectedPoints.last().x, topPad + plotHeight)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        EktaSkyBlue.copy(alpha = 0.28f),
                                        EktaSkyBlue.copy(alpha = 0.02f)
                                    ),
                                    startY = topPad,
                                    endY = topPad + plotHeight
                                )
                            )

                            val collectedLine = buildSmoothLinePath(collectedPoints)
                            drawPath(
                                path = collectedLine,
                                color = EktaSkyBlue,
                                style = Stroke(
                                    width = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            val autoPayLine = buildSmoothLinePath(autoPayPoints)
                            drawPath(
                                path = autoPayLine,
                                color = EktaRoyalBlue,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            val outstandingLine = buildSmoothLinePath(outstandingPoints)
                            drawPath(
                                path = outstandingLine,
                                color = StatusExpiredAccent,
                                style = Stroke(
                                    width = 1.6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )
                            )

                            // Data point dots
                            collectedPoints.forEachIndexed { idx, pt ->
                                val isSel = idx == safeIndex
                                if (isSel) {
                                    drawCircle(
                                        color = EktaSkyBlue.copy(alpha = 0.22f),
                                        radius = 10.dp.toPx(),
                                        center = pt
                                    )
                                }
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSel) 5.dp.toPx() else 3.5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = if (isSel) EktaNavy else EktaSkyBlue,
                                    radius = if (isSel) 5.dp.toPx() else 3.5.dp.toPx(),
                                    center = pt,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        } else {
                            // Grouped Bar Mode (Collected vs Outstanding)
                            val slotW = plotWidth / count.toFloat()
                            val barWidth = (slotW * 0.28f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                            val gap = 3.dp.toPx()
                            records.forEachIndexed { idx, rec ->
                                val centerX = horizPad + slotW * idx + slotW / 2f
                                val collectedY = yFor(rec.collectedCents)
                                val outstandingY = yFor(rec.outstandingCents)
                                val baseY = topPad + plotHeight
                                val isSel = idx == safeIndex

                                drawRoundRect(
                                    color = if (isSel) EktaSkyBlue else EktaNavy,
                                    topLeft = Offset(centerX - barWidth - gap / 2f, collectedY),
                                    size = Size(barWidth, (baseY - collectedY).coerceAtLeast(2f)),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                                drawRoundRect(
                                    color = if (isSel) StatusExpiredAccent else StatusExpiredAccent.copy(alpha = 0.72f),
                                    topLeft = Offset(centerX + gap / 2f, outstandingY),
                                    size = Size(barWidth, (baseY - outstandingY).coerceAtLeast(2f)),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Clickable X-Axis Month Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                records.forEachIndexed { idx, rec ->
                    val selected = idx == safeIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) EktaNavy else Color.Transparent)
                            .clickable { selectedIndex = idx }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("chart_month_${rec.monthKey}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rec.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else TextMuted
                        )
                    }
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = EktaSkyBlue, label = "Collected MRR")
                ChartLegendItem(color = EktaRoyalBlue, label = "Auto-Pay")
                ChartLegendItem(color = StatusExpiredAccent, label = "Outstanding Dues")
            }
        }
    }
}

private fun buildSmoothLinePath(points: List<Offset>): Path {
    return Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.lastIndex) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        }
    }
}

private fun mapXToIndex(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val ratio = (x / width).coerceIn(0f, 1f)
    return (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

@Composable
private fun ChartModeButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) EktaNavy else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color.White else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TooltipMetric(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            style = TabularMetricStyle,
            color = color
        )
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
