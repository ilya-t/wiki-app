package com.tsourcecode.wiki.lib.domain.mocking

import com.tsourcecode.wiki.lib.domain.backend.REVISION_ZIP_REPOSITORY_DIR
import com.tsourcecode.wiki.lib.domain.documents.Document
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.util.Date
import java.util.UUID

const val NOTES_PROJECT_ROLLBACK_API = "/notes/api/1/rollback"
const val NOTES_PROJECT_STAGE_API = "/notes/api/1/stage"
const val NOTES_PROJECT_COMMIT_API = "/notes/api/1/commit"

class Backend {
    private val mapLocal = mutableMapOf<String, ResponseMaker>(
        "/api/1/projects" to ResponseMaker.JsonResponse(
            """
                    {
                        "configs": [
                            {
                                "name": "notes",
                                "repo_url": "git@github.com:username/notes.git"
                            }
                        ]
                    }
                """.trimIndent()
        ),
//        "/api/1/status" to ResponseMaker.JsonResponse(
//            """
//                    {"files":[]}
//                """.trimIndent()
//        ),
        NOTES_PROJECT_STAGE_API to ResponseMaker.ResponseOK,
        NOTES_PROJECT_ROLLBACK_API to ResponseMaker.ResponseOK,
        NOTES_PROJECT_COMMIT_API to ResponseMaker.ResponseOK,
    )

    val interceptor: Interceptor = ApiResponseInterceptor(mapLocal, this::record)
    private val interceptions = mutableListOf<Pair<Request,Response>>()

    fun captureInterceptions(): List<Pair<Request,Response>> = interceptions.toList()

    private fun record(request: Request, response: Response) {
        synchronized(this) {
            interceptions.add(request to response)
        }
    }

    fun updateRevision(r: ProjectRevision) {
        val zipFile = File("/tmp/${r.revision}.zip")
        val fileResponse = ResponseMaker.FileResponse(
            file = zipFile,
            setup = { file ->
                Archiver.zipFile(
                    src = r.rootFileProvider(),
                    dst = file,
                    rootDir = REVISION_ZIP_REPOSITORY_DIR,
                )
            })
        mapLocal["/api/1/revision/latest"] = fileResponse
        mapLocal["/api/1/revision/sync"] = fileResponse

        mapLocal["/notes/api/1/revision/show"] = ResponseMaker.JsonResponse(
            """
                    {
                        "revision": "${r.revision}",
                        "message": "${r.message}",
                        "date": "${Date()}"
                    }
                """.trimIndent()
        )

        mapLocal["/notes/api/1/show_not_staged"] = ResponseMaker.JsonResponse(
            """
                {
                    "files":[ ${r.notStagedFiles.joinToString(prefix = "\"", postfix = "\"")}]
                }
            """.trimIndent()
        )

        val files = r.statusApiFiles.joinToString {
            """
                {
                    "path": "$it",
                    "status": "modified",
                    "diff": "<some diff>"
                }
            """.trimIndent()
        }
        mapLocal["/notes/api/1/status"] = ResponseMaker.JsonResponse(
            """
                {
                    "files": [ $files ]
                }
            """.trimIndent()
        )
    }
}

data class ProjectRevision(
    val message: String,
    val rootFileProvider: () -> File,
    val revision: String = UUID.randomUUID().toString(),
    val statusApiFiles: List<String> = emptyList(),
    val notStagedFiles: List<String> = emptyList(),
) {
    fun markUnstaged(readmeDoc: Document): ProjectRevision {
        return this.copy(notStagedFiles = notStagedFiles + readmeDoc.relativePath)
    }

    fun includeToDiff(readmeDoc: Document): ProjectRevision {
        return this.copy(statusApiFiles = statusApiFiles + readmeDoc.relativePath)
    }
}

class DiffFromServer(
    val path: String,
//    val status: Status,
//    val diff: String,
)