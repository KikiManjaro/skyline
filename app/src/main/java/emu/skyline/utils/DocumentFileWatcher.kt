/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2023 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */

package emu.skyline.utils

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlin.reflect.KFunction

object DocumentFileWatcher {
    private var currentFileIndirectory = 0
    private const val FILE_CREATED = 0
    private const val FILE_DELETED = 1
    private const val FILE_CHANGED = 2
    private var callback : KFunction<Unit>? = null
    private var filesToWatch = mutableMapOf<String, Long>()
    private var changingFiles = mutableSetOf<String>()
    private var watcherThread : Thread? = null

    private fun onDirectoryChanged(documentFiles : Array<DocumentFile?>?, event : Int) : Array<DocumentFile?>? {
        Log.d("FileUtil", "onDirectoryChanged: $event")
        callback!!.call(false)
        return documentFiles
    }

    fun startWatchingDirectory(directory : DocumentFile?) {
            if (directory != null) {
                if (watcherThread != null) {
                    watcherThread?.interrupt()
                    watcherThread = null
                    filesToWatch.clear()
                    changingFiles.clear()
                    currentFileIndirectory = 0
                }
                val documentFiles = directory.listFiles()
                documentFiles.forEach {
                    if (it.name != null) {
                        filesToWatch[it.name!!] = it.lastModified()
                    }
                }
                currentFileIndirectory = documentFiles.size
                watcherThread = Thread {
                    var running = true
                    loop@ while (running) {
                        runCatching {
                            val currentDocFiles = directory.listFiles()
                            filesToWatch.forEach { (key, value) ->
                                val file = currentDocFiles.find { it.name == key }
                                if (changingFiles.contains(key)) {
                                    if (file != null && file.lastModified() == value) {
                                        changingFiles.remove(key)
                                        onDirectoryChanged(arrayOf(file), FILE_CHANGED)
                                    }
                                } else if (file != null && file.lastModified() != value) {
                                    changingFiles.add(file.name!!)
                                    onDirectoryChanged(arrayOf(file), FILE_CHANGED)
                                }
                            }
                            if (currentDocFiles.size != currentFileIndirectory) {
                                if (currentDocFiles.size > currentFileIndirectory) {
                                    val newFiles = arrayOfNulls<DocumentFile>(currentDocFiles.size - currentFileIndirectory)
                                    onDirectoryChanged(newFiles, FILE_CREATED)
                                } else {
                                    onDirectoryChanged(null, FILE_DELETED)
                                }
                                currentFileIndirectory = currentDocFiles.size
                                currentDocFiles.forEach {
                                    if (it.name != null) {
                                        if (filesToWatch[it.name!!] == null) {
                                            changingFiles.add(it.name!!)
                                        }
                                        filesToWatch[it.name!!] = it.lastModified()
                                    }
                                }
                            }
                            Thread.sleep(1000)
                        }.onFailure {
                            if (it is InterruptedException) {
                                running = false
                            } else {
                                Log.e("FileUtil", "Error while watching directory: ${it.message}")
                            }
                        }
                    }
                }
                watcherThread!!.start()
            }
    }

    fun registerCallbackOnFileChanged(callbackFunction : KFunction<Unit>) {
        callback = callbackFunction
    }
}