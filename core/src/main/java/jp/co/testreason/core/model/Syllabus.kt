package jp.co.testreason.core.model

/**
 * Learning objective master for JSTQB Foundation Level syllabus 2023V4.0.J02.
 *
 * Summaries are written by us and intentionally do not reproduce the syllabus text (ADR-0006).
 */
@Suppress("LongMethod")
object Syllabus {
    const val VERSION = "2023V4.0.J02"

    val chapters: List<SyllabusChapter> = listOf(
        SyllabusChapter(1, "テストの基礎", 180),
        SyllabusChapter(2, "ソフトウェア開発ライフサイクル全体を通してのテスト", 130),
        SyllabusChapter(3, "静的テスト", 80),
        SyllabusChapter(4, "テスト分析と設計", 390),
        SyllabusChapter(5, "テスト活動のマネジメント", 335),
        SyllabusChapter(6, "テストツール", 20)
    )

    val learningObjectives: List<LearningObjective> = listOf(
        lo("FL-1.1.1", KLevel.K1, "テストの典型的な目的"),
        lo("FL-1.1.2", KLevel.K2, "テストとデバッグの違い"),
        lo("FL-1.2.1", KLevel.K2, "テストが必要とされる理由"),
        lo("FL-1.2.2", KLevel.K1, "テストと品質保証の関係"),
        lo("FL-1.2.3", KLevel.K2, "根本原因・エラー・欠陥・故障の区別"),
        lo("FL-1.3.1", KLevel.K2, "テストの7原則"),
        lo("FL-1.4.1", KLevel.K2, "テスト活動とタスクの全体像"),
        lo("FL-1.4.2", KLevel.K2, "状況や制約がテストプロセスに与える影響"),
        lo("FL-1.4.3", KLevel.K2, "テストウェアの種類"),
        lo("FL-1.4.4", KLevel.K2, "トレーサビリティがもたらす価値"),
        lo("FL-1.4.5", KLevel.K2, "テストに関わる役割の違い"),
        lo("FL-1.5.1", KLevel.K2, "テストに必要な汎用スキル"),
        lo("FL-1.5.2", KLevel.K1, "チーム全体アプローチの利点"),
        lo("FL-1.5.3", KLevel.K2, "テストの独立性の長所と短所"),

        lo("FL-2.1.1", KLevel.K2, "開発ライフサイクルの選択とテストへの影響"),
        lo("FL-2.1.2", KLevel.K1, "どの開発ライフサイクルにも通じるテストの良い実践"),
        lo("FL-2.1.3", KLevel.K1, "テストファーストの開発手法"),
        lo("FL-2.1.4", KLevel.K2, "DevOps とテスト"),
        lo("FL-2.1.5", KLevel.K2, "シフトレフト"),
        lo("FL-2.1.6", KLevel.K2, "ふりかえりを使ったプロセス改善"),
        lo("FL-2.2.1", KLevel.K2, "テストレベルの違い"),
        lo("FL-2.2.2", KLevel.K2, "テストタイプの違い"),
        lo("FL-2.2.3", KLevel.K2, "確認テストとリグレッションテストの違い"),
        lo("FL-2.3.1", KLevel.K2, "メンテナンステストとその契機"),

        lo("FL-3.1.1", KLevel.K1, "静的テストの対象になる作業成果物"),
        lo("FL-3.1.2", KLevel.K2, "静的テストがもたらす価値"),
        lo("FL-3.1.3", KLevel.K2, "静的テストと動的テストの比較"),
        lo("FL-3.2.1", KLevel.K1, "早期かつ頻繁なフィードバックの利点"),
        lo("FL-3.2.2", KLevel.K2, "レビュープロセスの活動"),
        lo("FL-3.2.3", KLevel.K1, "レビューの役割と責務"),
        lo("FL-3.2.4", KLevel.K2, "レビュー種別の比較"),
        lo("FL-3.2.5", KLevel.K1, "レビューを成功させる要因"),

        lo("FL-4.1.1", KLevel.K2, "テスト技法の3分類の違い"),
        lo("FL-4.2.1", KLevel.K3, "同値分割法の適用"),
        lo("FL-4.2.2", KLevel.K3, "境界値分析の適用"),
        lo("FL-4.2.3", KLevel.K3, "デシジョンテーブルテストの適用"),
        lo("FL-4.2.4", KLevel.K3, "状態遷移テストの適用"),
        lo("FL-4.3.1", KLevel.K2, "ステートメントテストとカバレッジ"),
        lo("FL-4.3.2", KLevel.K2, "ブランチテストとカバレッジ"),
        lo("FL-4.3.3", KLevel.K2, "ホワイトボックステストの価値"),
        lo("FL-4.4.1", KLevel.K2, "エラー推測"),
        lo("FL-4.4.2", KLevel.K2, "探索的テスト"),
        lo("FL-4.4.3", KLevel.K2, "チェックリストベースドテスト"),
        lo("FL-4.5.1", KLevel.K2, "ユーザーストーリーの共同作成"),
        lo("FL-4.5.2", KLevel.K2, "受け入れ基準の書き方"),
        lo("FL-4.5.3", KLevel.K3, "受け入れテスト駆動開発（ATDD）の適用"),

        lo("FL-5.1.1", KLevel.K2, "テスト計画書の目的と記載事項"),
        lo("FL-5.1.2", KLevel.K1, "イテレーション計画・リリース計画へのテスト担当者の貢献"),
        lo("FL-5.1.3", KLevel.K2, "開始基準と終了基準"),
        lo("FL-5.1.4", KLevel.K3, "見積り技法によるテスト工数の算出"),
        lo("FL-5.1.5", KLevel.K3, "テストケースの優先順位付け"),
        lo("FL-5.1.6", KLevel.K1, "テストピラミッド"),
        lo("FL-5.1.7", KLevel.K2, "テストの四象限"),
        lo("FL-5.2.1", KLevel.K1, "リスクの可能性と影響によるリスクレベル"),
        lo("FL-5.2.2", KLevel.K2, "プロジェクトリスクとプロダクトリスク"),
        lo("FL-5.2.3", KLevel.K2, "プロダクトリスク分析とテスト範囲"),
        lo("FL-5.2.4", KLevel.K2, "プロダクトリスクへの対応策"),
        lo("FL-5.3.1", KLevel.K1, "テストで使うメトリクス"),
        lo("FL-5.3.2", KLevel.K2, "テストレポートの目的・内容・読み手"),
        lo("FL-5.3.3", KLevel.K2, "テスト状況の伝え方"),
        lo("FL-5.4.1", KLevel.K2, "構成管理によるテストの支援"),
        lo("FL-5.5.1", KLevel.K3, "欠陥レポートの作成"),

        lo("FL-6.1.1", KLevel.K2, "テストツールの種類と支援内容"),
        lo("FL-6.2.1", KLevel.K1, "テスト自動化の利点とリスク")
    )

    private val byId: Map<String, LearningObjective> = learningObjectives.associateBy { it.id }

    fun findLearningObjective(id: String): LearningObjective? = byId[id]

    fun learningObjectivesOf(chapter: Int): List<LearningObjective> =
        learningObjectives.filter { it.chapter == chapter }

    private fun lo(id: String, kLevel: KLevel, summary: String) = LearningObjective(id, kLevel, summary)
}
