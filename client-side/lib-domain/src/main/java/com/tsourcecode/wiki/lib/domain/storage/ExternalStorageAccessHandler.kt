package com.tsourcecode.wiki.lib.domain.storage

interface ExternalStorageAccessHandler {
    val accessGranted: Boolean
    fun requestAccess()
}