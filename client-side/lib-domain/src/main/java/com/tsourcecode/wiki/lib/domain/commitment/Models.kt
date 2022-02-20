package com.tsourcecode.wiki.lib.domain.commitment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StatusResponse(
        @SerialName("files")
        val files: List<FileStatus>,
)

@Serializable
class FileStatus(
        @SerialName("path")
        val path: String,
        @SerialName("status")
        val status: Status,
        @SerialName("diff")
        val diff: String,
)

@Serializable
enum class Status(private val stringValue: String) {
    @SerialName("new")
    NEW("new"),
    @SerialName("modified")
    MODIFIED("modified"),
    @SerialName("untracked")
    UNTRACKED("untracked"),
}