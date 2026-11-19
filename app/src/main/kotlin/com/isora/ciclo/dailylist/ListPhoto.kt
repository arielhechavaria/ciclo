package com.isora.ciclo.dailylist

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The single daily list photo. It lives in the app's private cache, is shared only with the camera app
 * through the FileProvider, and is deleted as soon as it has been read (or the reading failed).
 */
class ListPhoto(private val context: Context) {
    private val folder get() = File(context.cacheDir, "list_photo")
    private val file get() = File(folder, "list.jpg")

    /** A fresh, empty target for the camera. Removes any photo a previous run left behind. */
    fun target(): Uri {
        deleteAll()
        folder.mkdirs()
        return FileProvider.getUriForFile(context, "${context.packageName}.listphoto", file)
    }

    /** Reads the text lines of the photo on the phone with the bundled ML Kit model, then deletes it. */
    suspend fun readAndDelete(): List<TextLine> = try {
        recognize(InputImage.fromFilePath(context, Uri.fromFile(file)))
    } finally {
        deleteAll()
    }

    fun deleteAll() {
        folder.listFiles()?.forEach { it.delete() }
    }

    private suspend fun recognize(image: InputImage): List<TextLine> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            return text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                line.boundingBox?.let { box -> TextLine(line.text, box.left, box.top, box.right, box.bottom) }
            }
        } finally {
            recognizer.close()
        }
    }
}
