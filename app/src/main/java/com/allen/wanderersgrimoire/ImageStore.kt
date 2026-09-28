package com.allen.wanderersgrimoire

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Images are copied into filesDir/images/ the moment they're shared in,
 * since a content:// Uri from another app is only readable for the life
 * of that share intent, not permanently. Item.imagePath stores the path
 * relative to filesDir, e.g. "images/<uuid>.jpg".
 */
object ImageStore {

    private fun imagesDir(context: Context): File {
        val dir = File(context.filesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Copies the content behind [uri] into internal storage. Returns the
     *  relative path to store on the Item, or null if the copy failed. */
    fun saveFromUri(context: Context, uri: Uri): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir(context), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            "images/$fileName"
        } catch (_: Exception) {
            null
        }
    }

    fun absoluteFile(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    fun delete(context: Context, relativePath: String) {
        try {
            absoluteFile(context, relativePath).delete()
        } catch (_: Exception) {
        }
    }

    /** Decodes a bitmap downsampled to roughly [reqWidth]x[reqHeight], so a
     *  12MP screenshot doesn't get loaded at full resolution just to show
     *  a small thumbnail. */
    fun loadSampled(context: Context, relativePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val file = absoluteFile(context, relativePath)
        if (!file.exists()) return null

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        var sample = 1
        var halfWidth = boundsOptions.outWidth / 2
        var halfHeight = boundsOptions.outHeight / 2
        while (halfWidth / sample >= reqWidth && halfHeight / sample >= reqHeight) {
            sample *= 2
        }

        val loadOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, loadOptions)
    }
}
