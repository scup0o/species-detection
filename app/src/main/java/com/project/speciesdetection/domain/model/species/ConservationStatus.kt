package com.project.speciesdetection.domain.model.species

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.project.speciesdetection.R

// Data class để lưu thông tin mỗi trạng thái
data class ConservationStatusInfo(
    val code: String,          // Mã để so sánh (ví dụ: "ex", "vu")
    val abbreviation: String,  // Chữ viết tắt hiển thị (ví dụ: "EX", "VU")
    @StringRes val fullNameResId: Int,      // Tên đầy đủ (ví dụ: "Extinct", "Vulnerable")
    val group: String,         // Nhóm ("Extinct", "Threatened", "Least Concern")
    val colorCode: String      // Mã màu (ví dụ: "VU" có màu vàng đặc trưng)
)

// Danh sách các trạng thái bảo tồn theo thứ tự hiển thị
val iucnStatuses = listOf(
    ConservationStatusInfo("ex", "EX", R.string.iucn_status_ex, "Extinct", "EX"),
    ConservationStatusInfo("ew", "EW", R.string.iucn_status_ew, "Extinct", "EW"),
    ConservationStatusInfo("cr", "CR", R.string.iucn_status_cr, "Threatened", "CR"),
    ConservationStatusInfo("en", "EN", R.string.iucn_status_en, "Threatened", "EN"),
    ConservationStatusInfo("vu", "VU", R.string.iucn_status_vu, "Threatened", "VU"),
    ConservationStatusInfo("nt", "NT", R.string.iucn_status_nt, "Least Concern", "NT"),
    ConservationStatusInfo("lc", "LC", R.string.iucn_status_lc, "Least Concern", "LC")
    // Thêm DD, NE nếu cần
)

// Hàm để lấy màu dựa trên mã và trạng thái chọn
@Composable
fun getChipColors(statusInfo: ConservationStatusInfo, isSelected: Boolean): Pair<Color, Color> {
    val defaultSelectedBg = MaterialTheme.colorScheme.primary
    val defaultSelectedText = MaterialTheme.colorScheme.onPrimary
    val defaultUnselectedBg = MaterialTheme.colorScheme.surface
    val defaultUnselectedText = MaterialTheme.colorScheme.onSurfaceVariant
    val defaultBorder = Color.Gray

    if (isSelected) {
        return when (statusInfo.code.uppercase()) {
            // Nhóm "Extinct"
            "EX" -> Color(0xFF000000) to Color.White // Đen (Extinct)
            "EW" -> Color(0xFF555555) to Color.White // Xám đậm (Extinct in the Wild)

            // Nhóm "Threatened"
            "VU" -> Color(0xFFB8860B) to Color.White // DarkGoldenrod, bạn có thể chỉnh màu này
            "EN" -> Color(0xFFDC143C) to Color.White // Crimson
            "CR" -> Color(0xFF8B0000) to Color.White // DarkRed

            // Nhóm "Least Concern" (hoặc Lower Risk)
            "NT" -> Color(0xFF7CB342) to Color.White // Xanh lá cây nhạt (Near Threatened)
            "LC" -> Color(0xFF4CAF50) to Color.White // Xanh lá cây (Least Concern)

            // Các trạng thái khác (tùy chọn)
            "DD" -> Color(0xFF757575) to Color.White // Xám (Data Deficient)
            "NE" -> Color(0xFFBDBDBD) to Color.Black // Xám rất nhạt (Not Evaluated, chữ đen)

            else -> defaultSelectedBg to defaultSelectedText // Màu mặc định nếu mã không khớp
        }
    }
    return defaultUnselectedBg to defaultUnselectedText
}