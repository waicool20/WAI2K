/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.wai2k.views

import ai.djl.metric.Metrics
import ai.djl.modality.cv.ImageFactory
import ai.djl.translate.TranslateException
import com.waicool20.cvauto.core.AnyDevice
import com.waicool20.cvauto.core.util.createCompatibleCopy
import com.waicool20.cvauto.core.util.removeChannels
import com.waicool20.cvauto.core.util.toBufferedImage
import com.waicool20.cvauto.core.util.toMat
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.util.ai.MatchingModel
import com.waicool20.wai2k.util.ai.MatchingTranslator
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

class HomographyViewer(
    private val device: AnyDevice,
    private val image: BufferedImage
) : CoroutineScopeView() {
    constructor(device: AnyDevice, imagePath: File) : this(device, ImageIO.read(imagePath))
    constructor(device: AnyDevice, imagePath: Path) : this(device, ImageIO.read(imagePath.toFile()))

    override val root: VBox by fxml("/views/homography-viewer.fxml")

    private val logger = loggerFor<HomographyViewer>()

    private val imageView: ImageView by fxid()
    private val region = device.displays.first().region
    private val window = region.subRegion(348, 151, 1281, 929)

    init {
        title = "Homography Viewer"
    }

    override fun onDock() {
        super.onDock()
        imageView.fitWidthProperty().bind(root.widthProperty())
        imageView.fitHeightProperty().bind(root.heightProperty())

        launch(Dispatchers.IO) {
            val model = MatchingModel(
                Wai2k.config.assetsDirectory.resolve("models/SuperPoint.pt"),
                Wai2k.config.assetsDirectory.resolve("models/SuperGlue.pt")
            )
            val translator = MatchingTranslator(480, 360)
            val metrics = Metrics()
            val predictor = model.newPredictor(translator).apply {
                setMetrics(metrics)
                try {
                    batchPredict(emptyList())
                } catch (e: TranslateException) {
                    // Expected, this preloads the model to device memory
                }
            }
            val imf = ImageFactory.getInstance()

            val baseImage = imf.fromImage(image)

            while (coroutineContext.isActive) {
                val screenshot = window.capture().img.let {
                    val copy = image.createCompatibleCopy(it.width, it.height)
                    copy.createGraphics().apply {
                        color = Color.BLACK
                        fillRect(0, 0, it.width, it.height)
                        drawImage(it, 0, 0, null)
                        dispose()
                    }
                    copy
                }
                try {
                    logger.debug("HomographyViewer predict begin -----------")
                    val h = predictor.predict(baseImage to imf.fromImage(screenshot))
                    logger.debug("Homography prediction metrics:")
                    logger.debug("Preprocess: ${metrics.latestMetric("Preprocess").value.toLong() / 1000} ms")
                    logger.debug("Inference: ${metrics.latestMetric("Inference").value.toLong() / 1000} ms")
                    logger.debug("Postprocess: ${metrics.latestMetric("Postprocess").value.toLong() / 1000} ms")
                    logger.debug("Prediction: ${metrics.latestMetric("Prediction").value.toLong() / 1000} ms")
                    imageView.image =
                        SwingFXUtils.toFXImage(renderStitching(image, screenshot, h), null)
                } catch (e: Exception) {
                    delay(1000)
                    logger.warn("Homography not found: ${e.message}")
                    continue
                }
            }
        }
    }

    private fun renderStitching(
        imageA: BufferedImage,
        imageB: BufferedImage,
        fromAtoB: Mat
    ): BufferedImage {
        val matA = imageA.toMat()
        val matB = imageB.toMat()

        val bg = Mat(matA.size(), matA.type())
        val overlay = Mat(matA.size(), matA.type())

        val fromAtoWork = Mat(3, 3, CvType.CV_64FC1).apply {
            put(
                0, 0,
                0.5, 0.0, matA.width() / 4.0,
                0.0, 0.5, matA.height() / 4.0,
                0.0, 0.0, 1.0
            )
        }

        Imgproc.warpPerspective(matA, bg, fromAtoWork, bg.size())

        val fromBtoWork = fromAtoWork.matMul(fromAtoB.inv())

        Imgproc.warpPerspective(
            matB,
            overlay,
            fromBtoWork,
            overlay.size(),
            Imgproc.INTER_LINEAR,
            Core.BORDER_CONSTANT,
            Scalar(255.0,  255.0, 255.0, 255.0)
        )

        val corners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(matB.width().toDouble(), 0.0),
            Point(0.0, matB.height().toDouble()),
            Point(matB.width().toDouble(), matB.height().toDouble())
        )

        Core.perspectiveTransform(corners, corners, fromBtoWork)
        corners.removeChannels(2)

        Core.bitwise_not(overlay, overlay)
        Core.addWeighted(bg, 0.5, overlay, 0.5, 0.0, bg)

        val cornersArr = corners.toArray()
        val redScalar = Scalar(0.0, 0.0, 255.0)
        Imgproc.line(bg, cornersArr[0], cornersArr[1], redScalar, 2)
        Imgproc.line(bg, cornersArr[1], cornersArr[3], redScalar, 2)
        Imgproc.line(bg, cornersArr[2], cornersArr[3], redScalar, 2)
        Imgproc.line(bg, cornersArr[0], cornersArr[2], redScalar, 2)

        return bg.toBufferedImage()
    }
}
