// Trong file com/project/speciesdetection/ui/composable/common/species/ConservationStatusViews.kt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.ui.composable.common.species.IUCNConservationStatusView
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

// Composable IUCNConservationStatusView và ConservationStatusChip giữ nguyên, không cần thay đổi.

/**
 * Composable hiển thị tình trạng bảo tồn theo nhiều hệ thống.
 * PHIÊN BẢN CẬP NHẬT: Luôn sử dụng giao diện chi tiết của IUCN cho tất cả các tab.
 *
 * @param iucnStatusCode Mã trạng thái của hệ thống IUCN.
 * @param otherSystems Map chứa các hệ thống đánh giá khác.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiSystemConservationStatusView(
    iucnStatusCode: String?,
    otherSystems: Map<String, String>
) {
    val tabs = remember(otherSystems) {
        listOf("IUCN") + otherSystems.keys.toList()
    }

    // Các state cần thiết cho việc đồng bộ LazyRow và HorizontalPager
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val lazyListState = rememberLazyListState() // State cho LazyRow
    val scope = rememberCoroutineScope()

    // Tự động cuộn LazyRow đến tab được chọn khi Pager thay đổi (do người dùng vuốt)
    /*LaunchedEffect(pagerState.currentPage) {
        scope.launch {
            lazyListState.animateScrollToItem(index = pagerState.currentPage)
        }
    }*/

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(15.dp)
            )
    ) {
        // =======================================================
        // START: THANH TAB TÙY CHỈNH BẰNG LAZYROW
        // =======================================================
        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            itemsIndexed(tabs) { index, title ->
                val isSelected = pagerState.currentPage == index

                // Mỗi tab là một cột chứa Text và thanh chỉ báo (indicator)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)) // Bo góc cho vùng click
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // Tắt hiệu ứng gợn sóng mặc định
                            onClick = {
                                // Khi click, cuộn Pager đến trang tương ứng
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tiêu đề của tab
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Thanh chỉ báo (indicator)
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(24.dp) // Chiều rộng của thanh chỉ báo
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp) // Bo tròn thanh chỉ báo
                            )
                    )
                }
            }
        }
        // =======================================================
        // END: THANH TAB TÙY CHỈNH BẰNG LAZYROW
        // =======================================================

            //HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

        // HorizontalPager giữ nguyên logic như trước
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentCodeToShow = if (page == 0) {
                iucnStatusCode
            } else {
                val systemName = tabs[page]
                otherSystems[systemName]
            }

            IUCNConservationStatusView(currentStatusCode = currentCodeToShow)
        }
    }
}