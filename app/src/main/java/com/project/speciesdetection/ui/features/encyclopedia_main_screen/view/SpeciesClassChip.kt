package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.ui.composable.common.CustomChip

/*fun customCardShape() = GenericShape { size, _ ->
    val width = size.width
    val height = size.height

    val stemWidth = width * 0.1f  // Chiều rộng cuống lá
    val stemHeight = height * 0.15f // Chiều cao cuống lá

    // Bắt đầu từ góc trên trái, tạo cuống lá nhỏ ở đó
    moveTo(width * 0.05f, 0f)  // Cuống lá bắt đầu từ góc trái trên

    // Vẽ cuống lá nhỏ (hướng lên trên từ góc topStart)
    lineTo(width * 0.05f - stemWidth, -stemHeight) // Cuống lá hướng lên

    // Tiếp tục vẽ phần thân lá
    moveTo(width * 0.05f, 0f) // Trở lại góc trên trái
    quadraticBezierTo(
        width * 0.2f,
        height * 0.1f,
        width * 0.5f,
        height * 0.0f
    ) // Vẽ đường cong trên bên trái
    quadraticBezierTo(width * 0.8f, height * 0.1f, width, height * 0.5f) // Đường cong bên phải

    // Tạo phần đáy của chiếc lá
    quadraticBezierTo(width * 0.8f, height * 0.9f, width * 0.5f, height) // Đáy bên phải
    quadraticBezierTo(width * 0.2f, height * 0.9f, width * 0.05f, height) // Đáy bên trái

    // Đóng path lại
    close()
}

val leafGreenColor = Color(0xFF3A652A)

class LeafClippingShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height

        // Bắt đầu từ chóp trên cùng của cuống lá
        path.moveTo(width * 0.02f, height * 0.05f) // Điểm A

        // Vẽ phần bên trái/dưới của cuống
        path.lineTo(width * 0.05f, height * 0.12f) // Điểm B
        path.lineTo(width * 0.15f, height * 0.15f) // Điểm C
        path.lineTo(width * 0.12f, height * 0.25f) // Điểm D
        path.lineTo(width * 0.2f, height * 0.35f)  // Điểm E (nối với thân lá dưới)

        // Vẽ đường cong phía dưới của thân lá
        // Từ điểm E đến chóp phải của lá (điểm F)
        path.cubicTo(
            width * 0.35f, height * 0.9f,  // Điểm kiểm soát 1 cho đường cong dưới
            width * 0.8f, height * 0.85f, // Điểm kiểm soát 2 cho đường cong dưới
            width * 0.95f, height * 0.5f  // Điểm F (chóp phải của lá)
        )

        // Vẽ đường cong phía trên của thân lá
        // Từ điểm F về điểm nối với cuống phía trên (điểm G')
        path.cubicTo(
            width * 0.8f, height * 0.15f, // Điểm kiểm soát 1 cho đường cong trên
            width * 0.35f, height * 0.1f,  // Điểm kiểm soát 2 cho đường cong trên
            width * 0.28f, height * 0.22f // Điểm G' (nối với thân lá trên)
        )

        // Vẽ phần bên phải/trên của cuống, quay lại điểm bắt đầu
        path.lineTo(width * 0.22f, height * 0.1f) // Điểm H
        path.lineTo(width * 0.1f, height * 0.03f)  // Điểm I
        path.lineTo(width * 0.02f, height * 0.05f) // Quay lại điểm A

        path.close() // Đóng path để tạo thành hình kín

        return Outline.Generic(path)
    }
}*/

@Composable
fun SpeciesClassChip(
    transparentColor: androidx.compose.ui.graphics.Color? = null,
    speciesClass: DisplayableSpeciesClass,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    CustomChip(
        onClick = onClick,
        title = speciesClass.localizedName,
        isSelected = isSelected,
        painterIcon = speciesClass.getIcon(),
        unSelectedContainerColor = MaterialTheme.colorScheme.tertiary,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.tertiary,
        unSelectedContentColor = MaterialTheme.colorScheme.onTertiary
        //borderTopStart = 0,
        //borderTopEnd = 0,
        //contentColor = MaterialTheme.colorScheme.onTertiary
    )
}