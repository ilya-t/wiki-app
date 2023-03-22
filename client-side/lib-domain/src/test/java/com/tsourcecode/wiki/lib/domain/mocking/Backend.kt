package com.tsourcecode.wiki.lib.domain.mocking

import okhttp3.Interceptor
import java.io.File
import java.util.*

private const val REVISION_ZIP_REPO_DIR = "repo"

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
    )

    val interceptor: Interceptor = ApiResponseInterceptor(mapLocal)

    fun updateRevision(r: ProjectRevision) {
        val zipFile = File("/tmp/${r.revision}.zip")
        val fileResponse = ResponseMaker.FileResponse(
            file = zipFile,
            setup = { file ->
                Archiver.zipFile(
                    src = r.rootFileProvider(),
                    dst = file,
                    rootDir = REVISION_ZIP_REPO_DIR,
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