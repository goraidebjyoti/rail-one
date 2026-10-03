package com.example.railone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID

internal fun profilePhotoFile(context: Context, name: String): File? =
    name.takeIf { Regex("profile-[a-f0-9-]+\\.jpg").matches(it) }?.let { File(context.filesDir, it) }

// Copy a bounded image to private storage; it survives picker permission expiry.
internal fun importProfilePhoto(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Choose a readable image." }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
    val bitmap = resolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("Could not read the image.")
    val orientation = runCatching { resolver.openInputStream(uri).use { stream ->
        requireNotNull(stream)
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            2 -> setScale(-1f, 1f)
            3 -> setRotate(180f)
            4 -> setScale(1f, -1f)
            5 -> { setRotate(90f); postScale(-1f, 1f) }
            6 -> setRotate(90f)
            7 -> { setRotate(270f); postScale(-1f, 1f) }
            8 -> setRotate(270f)
        }
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val name = "profile-${UUID.randomUUID()}.jpg"
    val file = requireNotNull(profilePhotoFile(context, name))
    try {
        file.outputStream().use { check(rotated.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
    } catch (e: Exception) {
        file.delete()
        throw e
    } finally {
        if (rotated !== bitmap) rotated.recycle()
        bitmap.recycle()
    }
    return name
}
