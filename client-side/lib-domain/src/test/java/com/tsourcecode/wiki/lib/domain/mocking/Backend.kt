package com.tsourcecode.wiki.lib.domain.mocking

import com.tsourcecode.wiki.lib.domain.backend.REVISION_ZIP_REPOSITORY_DIR
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.util.Date
import java.util.UUID

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
        "/api/1/status" to ResponseMaker.JsonResponse(
            """
                    {"files":[]}
                """.trimIndent()
        ),
        NOTES_PROJECT_STAGE_API to ResponseMaker.ResponseOK,
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
    }
}

class ProjectRevision(
    val message: String,
    val rootFileProvider: () -> File,
    val revision: String = UUID.randomUUID().toString(),
)