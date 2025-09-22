package com.tsourcecode.wiki.lib.domain.documents.staging

import kotlinx.serialization.Serializable

@Serializable
class PersistentFileHash(
    val path: String,
    val hash: String,
)
