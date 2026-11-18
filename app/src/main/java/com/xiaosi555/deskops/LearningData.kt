package com.xiaosi555.deskops

import org.json.JSONArray
import org.json.JSONObject

internal data class Chapter(
    val id: Long,
    val title: String,
    val content: String = "",
    val mediaUri: String = "",
    val completed: Boolean = false,
    val lastPositionMs: Long = 0L
)

internal data class Highlight(
    val id: Long,
    val courseId: Long,
    val chapterId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

internal data class StudySession(
    val id: Long,
    val courseId: Long,
    val chapterId: Long,
    val durationMs: Long,
    val day: String,
    val createdAt: Long = System.currentTimeMillis()
)

internal object LearningDataCodec {
    fun encodeHighlights(items: List<Highlight>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject()
                .put("id", item.id)
                .put("courseId", item.courseId)
                .put("chapterId", item.chapterId)
                .put("text", item.text)
                .put("createdAt", item.createdAt))
        }
    }.toString()

    fun decodeHighlights(raw: String): List<Highlight> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Highlight(
                item.optLong("id"),
                item.optLong("courseId"),
                item.optLong("chapterId"),
                item.optString("text"),
                item.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }.getOrDefault(emptyList())

    fun encodeSessions(items: List<StudySession>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject()
                .put("id", item.id)
                .put("courseId", item.courseId)
                .put("chapterId", item.chapterId)
                .put("durationMs", item.durationMs)
                .put("day", item.day)
                .put("createdAt", item.createdAt))
        }
    }.toString()

    fun decodeSessions(raw: String): List<StudySession> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            StudySession(
                item.optLong("id"),
                item.optLong("courseId"),
                item.optLong("chapterId"),
                item.optLong("durationMs"),
                item.optString("day"),
                item.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }.getOrDefault(emptyList())

    fun encodeBackup(coursesJson: String, mediaJson: String, highlightsJson: String, sessionsJson: String): String {
        return JSONObject()
            .put("format", "deskops-learning-backup")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("courses", JSONArray(coursesJson))
            .put("media", JSONArray(mediaJson))
            .put("highlights", JSONArray(highlightsJson))
            .put("sessions", JSONArray(sessionsJson))
            .toString(2)
    }

    fun validateBackup(raw: String): JSONObject {
        val root = JSONObject(raw)
        require(root.optString("format") == "deskops-learning-backup") { "文件格式不正确" }
        require(root.optInt("version") in 1..1) { "暂不支持此备份版本" }
        require(root.has("courses") && root.get("courses") is JSONArray) { "备份中缺少课程数据" }
        return root
    }
}
