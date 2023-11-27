
package com.demo

import android.graphics.Bitmap
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Image Analyser for performing depth estimation on camera frames.
class FrameAnalyser(
    private var depthEstimationModel : MiDASModel,
    private var drawingOverlay: DrawingOverlay
) : ImageAnalysis.Analyzer {

    private var frameBitmap : Bitmap? = null
    private var isFrameProcessing = false
    var isComputingDepthMap = false



    override fun analyze(image: ImageProxy) {
        // Return if depth map computation is turned off. See MainActivity.kt
        if ( !isComputingDepthMap ) {
            image.close()
            return
        }
        // If a frame is being processed, drop the current frame.
        if ( isFrameProcessing ) {
            image.close()
            return
        }
        isFrameProcessing = true
        if ( image.image != null ) {
            // Get the `Bitmap` of the current frame ( with corrected rotation ).
            frameBitmap = BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)
            image.close()
            CoroutineScope( Dispatchers.Main ).launch {
                runModel( frameBitmap!! )
            }
        }
    }


    private suspend fun runModel( inputImage : Bitmap ) = withContext( Dispatchers.Default ) {
        // Compute the depth given the frame Bitmap.
        val output = depthEstimationModel.getDepthMap( inputImage )
        withContext( Dispatchers.Main ) {
            // Notify that the current frame is processed and the pipeline is
            // ready for the next frame.
            isFrameProcessing = false
            if ( drawingOverlay.visibility == View.GONE ) {
                drawingOverlay.visibility = View.VISIBLE
            }
            // Submit the depth Bitmap to the DrawingOverlay and update it.
            // Note, calling `drawingOverlay.invalidate()` here will call `onDraw()` in DrawingOverlay.kt.
            drawingOverlay.depthMaskBitmap = BitmapUtils.resizeBitmap(
                output,
                frameBitmap!!.width, frameBitmap!!.height
            )
            drawingOverlay.invalidate()
        }
    }

}