package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.R
import com.example.model.GymMember
import com.example.model.GymSettings
import com.example.model.MembershipStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

private const val MAX_RAW_IMAGE_BYTES = 5 * 1024 * 1024 // 5 MB
private const val MAX_PHOTO_DIMENSION_PX = 1024
private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

data class PreparedMemberPhoto(
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int,
    val height: Int
)

fun createTempCameraImageUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val photoFile = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

suspend fun validateAndPrepareMemberPhoto(
    context: Context,
    uri: Uri
): Result<PreparedMemberPhoto> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val declaredMime = resolver.getType(uri)?.lowercase()
        if (declaredMime != null && declaredMime !in ALLOWED_MIME_TYPES) {
            throw IllegalArgumentException("Unsupported image format ($declaredMime). Use JPG, PNG, or WEBP.")
        }

        // Check reported size first if available
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    val reportedSize = cursor.getLong(sizeIndex)
                    if (reportedSize > MAX_RAW_IMAGE_BYTES) {
                        throw IllegalArgumentException("Photo exceeds 5 MB limit. Please select a smaller image.")
                    }
                }
            }
        }

        val rawBytes = resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > MAX_RAW_IMAGE_BYTES) {
                    throw IllegalArgumentException("Photo exceeds 5 MB limit. Please select a smaller image.")
                }
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: throw IllegalArgumentException("Unable to read selected image.")

        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("Selected image file is empty.")
        }

        // Decode image bounds and validate actual container format
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
        val decodedMime = boundsOptions.outMimeType?.lowercase()
        if (decodedMime == null || decodedMime !in ALLOWED_MIME_TYPES) {
            throw IllegalArgumentException("Unsupported image file. Please select a JPG, PNG, or WEBP photo.")
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            throw IllegalArgumentException("Invalid or corrupted image file.")
        }

        val sampleSize = calculateInSampleSize(
            boundsOptions.outWidth,
            boundsOptions.outHeight,
            MAX_PHOTO_DIMENSION_PX,
            MAX_PHOTO_DIMENSION_PX
        )
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: throw IllegalArgumentException("Unable to decode selected image.")

        val exifRotation = readExifTransform(rawBytes)
        val orientedBitmap = if (!exifRotation.isIdentity) {
            Bitmap.createBitmap(
                sampledBitmap,
                0,
                0,
                sampledBitmap.width,
                sampledBitmap.height,
                exifRotation,
                true
            ).also {
                if (it !== sampledBitmap) sampledBitmap.recycle()
            }
        } else {
            sampledBitmap
        }

        val finalBitmap = scaleDownIfNeeded(orientedBitmap, MAX_PHOTO_DIMENSION_PX)
        if (finalBitmap !== orientedBitmap) {
            orientedBitmap.recycle()
        }

        val out = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 86, out)
        val jpegBytes = out.toByteArray()
        val width = finalBitmap.width
        val height = finalBitmap.height
        finalBitmap.recycle()

        PreparedMemberPhoto(
            bytes = jpegBytes,
            mimeType = "image/jpeg",
            width = width,
            height = height
        )
    }
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun readExifTransform(bytes: ByteArray): Matrix {
    val matrix = Matrix()
    try {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
    } catch (_: Exception) {
        // Ignore EXIF read failures on PNG/WEBP streams
    }
    return matrix
}

private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val maxSide = max(bitmap.width, bitmap.height)
    if (maxSide <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / maxSide.toFloat()
    val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
}

suspend fun loadImageBitmap(context: Context, imageUrl: String?): Bitmap? {
    if (imageUrl.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                result.drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

suspend fun loadMemberPhotoBitmap(context: Context, photoUrl: String?): Bitmap? =
    loadImageBitmap(context, photoUrl)

fun renderMemberIdCardBitmap(
    context: Context,
    member: GymMember,
    photoBitmap: Bitmap?,
    gymLogoBitmap: Bitmap? = null,
    gymSettings: GymSettings? = null
): Bitmap {
    val width = 1080
    val height = 680
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background card canvas
    canvas.drawColor(Color.parseColor("#FFFFFF"))

    // Top Turquoise/Navy header band
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), 196f,
            Color.parseColor("#0F2942"),
            Color.parseColor("#0089A7"),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), 196f, headerPaint)

    val showLogo = gymSettings?.idCardShowLogo ?: true
    val gymDisplayName = gymSettings?.gymName?.takeIf { it.isNotBlank() } ?: "EKTA 47 GYM & FITNESS CENTRE"
    val tagline = gymSettings?.idCardTagline?.takeIf { it.isNotBlank() } ?: "OFFICIAL MEMBER ID CARD  •  SINCE 1986"

    var textStartX = 44f
    if (showLogo) {
        // Official / custom EKTA 47 GYM logo badge (preserving exact aspect ratio)
        val logoBox = RectF(44f, 30f, 180f, 166f)
        val whiteBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(logoBox, 20f, 20f, whiteBadgePaint)

        val defaultLogoBitmap = if (gymLogoBitmap == null) {
            BitmapFactory.decodeResource(context.resources, R.drawable.ekta_47_logo)
        } else null
        val logoToDraw = gymLogoBitmap ?: defaultLogoBitmap
        if (logoToDraw != null) {
            val innerPad = 10f
            val availW = logoBox.width() - innerPad * 2
            val availH = logoBox.height() - innerPad * 2
            val scale = min(availW / logoToDraw.width.toFloat(), availH / logoToDraw.height.toFloat())
            val drawW = logoToDraw.width * scale
            val drawH = logoToDraw.height * scale
            val left = logoBox.left + (logoBox.width() - drawW) / 2f
            val top = logoBox.top + (logoBox.height() - drawH) / 2f
            canvas.drawBitmap(
                logoToDraw,
                null,
                RectF(left, top, left + drawW, top + drawH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            defaultLogoBitmap?.recycle()
        }
        textStartX = 206f
    }

    // Header text
    val gymTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D6F4FA")
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(gymDisplayName.take(32), textStartX, 84f, gymTitlePaint)
    canvas.drawText(tagline.take(44), textStartX, 124f, subTitlePaint)

    // Status pill on top right
    val (pillBg, pillFg) = when {
        !member.accessEnabled || member.statusEnum == MembershipStatus.EXPIRED ->
            Color.parseColor("#FEE2E2") to Color.parseColor("#B91C1C")
        member.statusEnum == MembershipStatus.EXPIRING ->
            Color.parseColor("#FEF3C7") to Color.parseColor("#B45309")
        else -> Color.parseColor("#DCFCE7") to Color.parseColor("#15803D")
    }
    val statusText = if (!member.accessEnabled) "BLOCKED" else member.statusEnum.label
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBg }
    val pillRect = RectF(850f, 134f, 1036f, 178f)
    canvas.drawRoundRect(pillRect, 22f, 22f, pillPaint)
    val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillFg
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(statusText, pillRect.centerX(), pillRect.centerY() + 8f, pillTextPaint)

    // Circular Member Profile Photo on left
    val avatarCx = 166f
    val avatarCy = 392f
    val avatarRadius = 112f

    val avatarRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0094B6")
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    val avatarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEF2F6")
    }
    canvas.drawCircle(avatarCx, avatarCy, avatarRadius, avatarBgPaint)

    if (photoBitmap != null) {
        canvas.save()
        val clipPath = Path().apply {
            addCircle(avatarCx, avatarCy, avatarRadius - 4f, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        // Center-crop square from bitmap
        val size = min(photoBitmap.width, photoBitmap.height)
        val srcX = (photoBitmap.width - size) / 2
        val srcY = (photoBitmap.height - size) / 2
        val srcRect = Rect(srcX, srcY, srcX + size, srcY + size)
        val dstRect = RectF(
            avatarCx - avatarRadius,
            avatarCy - avatarRadius,
            avatarCx + avatarRadius,
            avatarCy + avatarRadius
        )
        canvas.drawBitmap(
            photoBitmap,
            srcRect,
            dstRect,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        canvas.restore()
    } else {
        val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F2942")
            textSize = 68f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(member.initials, avatarCx, avatarCy + 24f, initialsPaint)
    }
    canvas.drawCircle(avatarCx, avatarCy, avatarRadius, avatarRingPaint)

    // Member Details on right
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#007D99")
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 23f
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val rightX = 324f
    canvas.drawText(member.fullName.take(26), rightX, 286f, namePaint)
    canvas.drawText("Member ID: ${member.displayMemberId}", rightX, 330f, idPaint)

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 2f
    }
    canvas.drawLine(rightX, 354f, width - 44f, 354f, dividerPaint)

    canvas.drawText("MEMBERSHIP PLAN", rightX, 392f, labelPaint)
    canvas.drawText(member.displayPlanName.take(36), rightX, 426f, valuePaint)

    canvas.drawText("VALIDITY", rightX, 472f, labelPaint)
    val validityText = if (member.daysRemaining < 0) {
        "${member.startDateFormatted} → Expired (${member.expiryDateFormatted})"
    } else {
        "${member.startDateFormatted} → ${member.expiryDateFormatted} (${member.daysRemaining}d left)"
    }
    canvas.drawText(validityText, rightX, 506f, valuePaint)

    canvas.drawText("CONTACT & FACILITY", rightX, 552f, labelPaint)
    val showLocker = gymSettings?.idCardShowLocker ?: true
    val showCoach = gymSettings?.idCardShowCoach ?: true
    val contactParts = buildList {
        add(member.phone)
        if (showLocker) add("Locker: ${member.lockerNumber?.takeIf { it.isNotBlank() } ?: "General"}")
        if (showCoach) add(member.trainerName?.takeIf { it.isNotBlank() } ?: "Floor Coach")
    }
    canvas.drawText(contactParts.joinToString("  •  ").take(46), rightX, 586f, valuePaint)

    // Bottom footer strip
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F1F5F9") }
    canvas.drawRect(0f, 622f, width.toFloat(), height.toFloat(), footerPaint)
    val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 21f
    }
    val phoneFooter = gymSettings?.contactPhone?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
    canvas.drawText("$gymDisplayName$phoneFooter • Verified Pass".take(64), 44f, 658f, footerTextPaint)

    // Outer subtle border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(2f, 2f, width - 2f, height - 2f, borderPaint)

    return bitmap
}

suspend fun saveMemberIdCardToGallery(
    context: Context,
    member: GymMember,
    photoBitmap: Bitmap?,
    gymLogoBitmap: Bitmap? = null,
    gymSettings: GymSettings? = null
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val cardBitmap = renderMemberIdCardBitmap(context, member, photoBitmap, gymLogoBitmap, gymSettings)
        val safeCode = member.memberCode.replace(Regex("[^A-Za-z0-9_-]"), "")
        val fileName = "EKTA47_ID_${safeCode}_${System.currentTimeMillis()}.png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/EKTA 47 GYM"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Unable to create image entry in Pictures.")

                resolver.openOutputStream(itemUri)?.use { out ->
                    cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: throw IllegalStateException("Unable to write ID card image.")

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                "Saved ID card to Pictures/EKTA 47 GYM/$fileName"
            } else {
                val targetDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "EKTA 47 GYM"
                ).apply { mkdirs() }
                val outFile = File(targetDir, fileName)
                FileOutputStream(outFile).use { out ->
                    cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                "Saved ID card: ${outFile.name}"
            }
        } finally {
            cardBitmap.recycle()
        }
    }
}

suspend fun shareMemberIdCard(
    context: Context,
    member: GymMember,
    photoBitmap: Bitmap?,
    gymLogoBitmap: Bitmap? = null,
    gymSettings: GymSettings? = null
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val cardBitmap = renderMemberIdCardBitmap(context, member, photoBitmap, gymLogoBitmap, gymSettings)
        val safeCode = member.memberCode.replace(Regex("[^A-Za-z0-9_-]"), "")
        val dir = File(context.cacheDir, "id_cards").apply { mkdirs() }
        val file = File(dir, "EKTA47_ID_${safeCode}.png")
        try {
            FileOutputStream(file).use { out ->
                cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } finally {
            cardBitmap.recycle()
        }

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "EKTA 47 GYM Member ID Card - ${member.fullName} (${member.displayMemberId})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "EKTA 47 GYM & FITNESS CENTRE Digital ID Card for ${member.fullName} (${member.displayMemberId})."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Member ID Card"))
        }
    }
}
