package com.tsourcecode.wiki.lib.domain.commitment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
        @SerialName("files")
        val files: List<FileStatus>,
)

@Serializable
data class UnstagedResponse(
        @SerialName("files")
        val files: List<String>,
)

@Serializable
data class FileStatus(
        @SerialName("path")
        val path: String,
        @SerialName("status")
        val status: Status,
        @SerialName("diff")
        val diff: String,
) {
    override fun toString(): String {
        return "FileStatus(path='$path', status=$status) diff:\n```diff\n$diff\n```\n"
    }
}

@Serializable
enum class Status(private val stringValue: String) {
    @SerialName("new")
    NEW("new"),
    @SerialName("modified")
    MODIFIED("modified"),
    @SerialName("untracked")
    UNTRACKED("untracked"),
}