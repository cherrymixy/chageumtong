package com.pure.chageumtong.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.OnPrimary
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.PrimaryDark
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.fig

/** products.json / places.json 의 imageAsset 이름 → drawable id. 없으면 0 */
@SuppressLint("DiscouragedApi") // 이름이 JSON 데이터에서 오므로 R.drawable 상수로 쓸 수 없음
@Composable
fun drawableByName(name: String): Int {
    val ctx = LocalContext.current
    return ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
}

/** 사진이 있으면 Crop으로 채우고, 없으면 테마색 그라데이션 (승아 결정, STEP 8) */
@Composable
fun AssetImage(name: String, modifier: Modifier) {
    val id = drawableByName(name)
    if (id != 0) {
        Image(painterResource(id), null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Brush.linearGradient(listOf(Primary, PrimaryDark))))
    }
}

/** 사용가능 포인트 카드 (상점 95:2962, 470×103). 적립 내역 화면도 같이 쓴다 */
@Composable
fun PointsCard(total: Int, modifier: Modifier = Modifier, label: String = "사용가능\n포인트") {
    Row(
        modifier.size(fig(470), fig(103)).clip(RoundedCornerShape(D.RadiusCard)).background(Primary)
            .padding(start = fig(28)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.width(fig(104)), style = T.PointsLabel, color = OnPrimary)
        Box(Modifier.width(fig(1)).height(fig(53)).background(OnPrimary))
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(6.3))) {
                Text(fmtP(total), style = T.TotalNumber, color = OnPrimary)
                Text("P", style = T.TotalNumber, color = OnPrimary.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.width(fig(8)))
    }
}
