package jp.co.testreason.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.ui.components.ResponsiveScreenColumn

/** Disclaimer, attribution and licences that ADR-0006 requires the app to show. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    AboutContent(versionName = versionName, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContent(versionName: String, modifier: Modifier = Modifier) {
    Scaffold(topBar = { TopAppBar(title = { Text("このアプリについて") }) }, modifier = modifier) { padding ->
        ResponsiveScreenColumn(contentPadding = padding) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TestReason", style = MaterialTheme.typography.titleLarge)
                if (versionName.isNotBlank()) {
                    Text("バージョン $versionName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AboutSection(
                title = "非公式の学習アプリです",
                paragraphs = listOf(
                    "このアプリは非公式の学習アプリです。ISTQB® および JSTQB® とは提携しておらず、両団体による承認・推奨・認定を受けたものではありません。",
                    "ISTQB® と JSTQB® は、それぞれの権利者の登録商標です。"
                )
            )
            AboutSection(
                title = "対応するシラバスと内容",
                paragraphs = listOf(
                    "学習目的（LO）の番号と知識レベル（K1〜K3）は、JSTQB® Foundation Level シラバス Version ${Syllabus.VERSION}" +
                        "（© ISTQB®、日本語翻訳 © JSTQB®）に対応しています。",
                    "問題・解説・用語の要約は、このアプリが独自に作成したものです。シラバスや公式の用語集の文章は掲載していません。" +
                        "正確な定義と最新の内容は、公式のシラバスで確認してください。",
                    "模擬試験の問題数・時間・合格ラインは、公開されている試験の構成にならっています。実際の試験の合否を保証するものではありません。"
                )
            )
            AboutSection(
                title = "データの扱い",
                paragraphs = listOf(
                    "学習の記録と設定は、この端末の中に保存されます。このアプリはインターネットに接続する権限を持たず、データを外部に送信しません。",
                    "端末のバックアップ機能を有効にしている場合は、その設定に従って、端末のバックアップに含まれることがあります。"
                )
            )
            AboutSection(
                title = "オープンソースライセンス",
                paragraphs = listOf(
                    "このアプリは次のオープンソースソフトウェアを使用しています。いずれも Apache License, Version 2.0 で提供されています。",
                    OPEN_SOURCE_COMPONENTS.joinToString("\n") { "・$it" },
                    "ライセンスの全文: https://www.apache.org/licenses/LICENSE-2.0"
                )
            )
        }
    }
}

@Composable
private fun AboutSection(title: String, paragraphs: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/** Libraries shipped in the app (runtime dependencies in gradle/libs.versions.toml). Update when one is added. */
private val OPEN_SOURCE_COMPONENTS = listOf(
    "AndroidX・Jetpack Compose・Room・DataStore・Navigation（The Android Open Source Project）",
    "Kotlin・kotlinx.coroutines・kotlinx.serialization（JetBrains）",
    "Dagger・Hilt（The Dagger Authors）"
)
