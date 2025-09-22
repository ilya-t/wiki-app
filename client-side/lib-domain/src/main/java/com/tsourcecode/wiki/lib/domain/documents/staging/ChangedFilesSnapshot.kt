package com.tsourcecode.wiki.lib.domain.documents.staging

import kotlinx.serialization.Serializable

@Serializable
class ChangedFilesSnapshot(
    val files: List<PersistentFileHash>
)