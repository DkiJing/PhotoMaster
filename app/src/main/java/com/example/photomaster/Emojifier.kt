package com.example.photomaster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.widget.Toast
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector


class Emojifier {
    companion object {
        private const val SMILING_PROB_THRESHOLD = .1
        private const val EYE_OPEN_PROB_THRESHOLD = .5
        private const val EMOJI_SCALE_FACTOR = .9f

        fun detectFaces(c: Context, picture: Bitmap): Bitmap {
            val detector = FaceDetector.Builder(c)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()
            val frame = Frame.Builder().setBitmap(picture).build()
            val faces = detector.detect(frame)

            var resultBitmap = picture

            if(faces.size() == 0) {
                Toast.makeText(c, "No face detected", Toast.LENGTH_SHORT).show()
            } else {
                for (i: Int in 0 until faces.size()) {
                    val face = faces.valueAt(i)
                    val emojiBitmap: Bitmap
                    when (getClassifications(face)) {
                        Emoji.SMILE -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.smile)
                        Emoji.FROWN -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.frown)
                        Emoji.LEFT_WINK -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.leftwink)
                        Emoji.RIGHT_WINK -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.rightwink)
                        Emoji.LEFT_WINK_FROWN -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.leftwinkfrown)
                        Emoji.RIGHT_WINK_FROWN -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.rightwinkfrown)
                        Emoji.CLOSED_EYE_SMILE -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.closed_smile)
                        Emoji.CLOSED_EYE_FROWN -> emojiBitmap = BitmapFactory.decodeResource(c.resources,
                                R.drawable.closed_frown)
                    }
                    resultBitmap = addEmojiToFace(resultBitmap, emojiBitmap, face)
                }
            }
            detector.release()
            return resultBitmap
        }

        private fun getClassifications(face: Face): Emoji {
            val smiling = face.isSmilingProbability > SMILING_PROB_THRESHOLD
            val leftEyeClosed = face.isLeftEyeOpenProbability < EYE_OPEN_PROB_THRESHOLD
            val rightEyeClosed = face.isRightEyeOpenProbability < EYE_OPEN_PROB_THRESHOLD
            val emoji: Emoji
            if (smiling) {
                if (leftEyeClosed && !rightEyeClosed) {
                    emoji = Emoji.LEFT_WINK
                } else if (rightEyeClosed && !leftEyeClosed) {
                    emoji = Emoji.RIGHT_WINK
                } else if (leftEyeClosed) {
                    emoji = Emoji.CLOSED_EYE_SMILE
                } else {
                    emoji = Emoji.SMILE
                }
            } else {
                if (leftEyeClosed && !rightEyeClosed) {
                    emoji = Emoji.LEFT_WINK_FROWN
                } else if (rightEyeClosed && !leftEyeClosed) {
                    emoji = Emoji.RIGHT_WINK_FROWN
                } else if (leftEyeClosed) {
                    emoji = Emoji.CLOSED_EYE_FROWN
                } else {
                    emoji = Emoji.FROWN
                }
            }
            return emoji
        }

        private fun addEmojiToFace(backGroundBitmap: Bitmap, emojiBitmap: Bitmap, face: Face): Bitmap {
            var emoji = emojiBitmap
            val resultBitmap = Bitmap.createBitmap(backGroundBitmap.width, backGroundBitmap.height, backGroundBitmap.config)
            val scaleFactor = EMOJI_SCALE_FACTOR
            val newEmojiWidth = (face.width * scaleFactor).toInt()
            val newEmojiHeight = ((emoji.height * newEmojiWidth / emoji.width) * scaleFactor).toInt()

            emoji = Bitmap.createScaledBitmap(emoji, newEmojiWidth, newEmojiHeight, false)

            val emojiPositionX = face.position.x + face.width / 2 - emoji.width / 2
            val emojiPositionY = face.position.y + face.height / 4 - emoji.height / 3

            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(backGroundBitmap, 0f, 0f, null)
            canvas.drawBitmap(emoji, emojiPositionX, emojiPositionY, null)

            return resultBitmap
        }
    }

    private enum class Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }
}