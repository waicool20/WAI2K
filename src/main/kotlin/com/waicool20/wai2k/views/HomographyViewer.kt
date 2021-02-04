/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
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

import ai.djl.modality.cv.ImageFactory
import boofcv.alg.distort.PixelTransformHomography_F32
import boofcv.alg.distort.impl.DistortSupport
import boofcv.factory.interpolate.FactoryInterpolation
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.border.BorderType
import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.core.IDevice
import com.waicool20.cvauto.util.transformPoint
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.util.ai.MatchingModel
import com.waicool20.wai2k.util.ai.MatchingTranslator
import com.waicool20.waicoolutils.createCompatibleCopy
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.logging.loggerFor
import georegression.struct.homography.Homography2D_F64
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO


class HomographyViewer(
    private val device: IDevice,
    private val image: BufferedImage
) : CoroutineScopeView() {
    constructor(device: IDevice, imagePath: File) : this(device, ImageIO.read(imagePath))
    constructor(device: IDevice, imagePath: Path) : this(device, ImageIO.read(imagePath.toFile()))

    override val root: VBox by fxml("/views/homography-viewer.fxml")

    private val logger = loggerFor<HomographyViewer>()

    private val wai2KContext: Wai2KContext by inject()

    private val imageView: ImageView by fxid()
    private val region = device.screens[0]
    private val window = region.subRegion(455, 151, 1281, 929)

    init {
        title = "Homography Viewer"
    }

    override fun onDock() {
        super.onDock()
        imageView.fitWidthProperty().bind(root.widthProperty());
        imageView.fitHeightProperty().bind(root.heightProperty());

        launch(Dispatchers.IO) {
            val model = MatchingModel(
                wai2KContext.wai2KConfig.assetsDirectory.resolve("models/SuperPoint.pt"),
                wai2KContext.wai2KConfig.assetsDirectory.resolve("models/SuperGlue.pt")
            )
            val translator = MatchingTranslator(480, 360)
            translator.prepare(model.ndManager, model)
            val predictor = model.newPredictor(translator)
            val imf = ImageFactory.getInstance()

            val baseImage = imf.fromImage(image)

            while (isActive) {
                val screenshot = window.capture().let {
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
                    val h = predictor.predict(baseImage to imf.fromImage(screenshot))
                    imageView.image =
                        SwingFXUtils.toFXImage(renderStitching(image, screenshot, h), null)
                } catch (e: Exception) {
                    delay(1000)
                    logger.warn("Homography not found")
                    continue
                }

            }
        }
    }

    private fun renderStitching(
        imageA: BufferedImage,
        imageB: BufferedImage,
        fromAtoB: Homography2D_F64
    ): BufferedImage {
        val scale = 0.5
        val colorA = ConvertBufferedImage.convertFromPlanar(imageA, null, true, GrayF32::class.java)
        val colorB = ConvertBufferedImage.convertFromPlanar(imageB, null, true, GrayF32::class.java)

        val work = colorA.createSameShape()

        val fromAToWork = Homography2D_F64(
            scale, 0.0, colorA.width / 4.0,
            0.0, scale, colorA.height / 4.0,
            0.0, 0.0, 1.0
        )
        val fromWorkToA = fromAToWork.invert(null)

        val model = PixelTransformHomography_F32()
        val interp = FactoryInterpolation.bilinearPixelS(GrayF32::class.java, BorderType.ZERO)
        val distort =
            DistortSupport.createDistortPL(GrayF32::class.java, model, interp, false).apply {
                renderAll = false
            }

        model.set(fromWorkToA)
        distort.apply(colorA, work)
        val fromWorkToB = fromWorkToA.concat(fromAtoB, null)
        model.set(fromWorkToB)
        distort.apply(colorB, work)

        val output = BufferedImage(work.width, work.height, imageA.type)
        ConvertBufferedImage.convertTo(work, output, true)

        val fromBtoWork = fromWorkToB.invert(null)
        val corners = arrayOf(
            fromBtoWork.transformPoint(0, 0),
            fromBtoWork.transformPoint(colorB.width, 0),
            fromBtoWork.transformPoint(colorB.width, colorB.height),
            fromBtoWork.transformPoint(0, colorB.height)
        )

        output.createGraphics().apply {
            color = Color.RED
            stroke = BasicStroke(4f)
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            drawLine(corners[0].x, corners[0].y, corners[1].x, corners[1].y)
            drawLine(corners[1].x, corners[1].y, corners[2].x, corners[2].y)
            drawLine(corners[2].x, corners[2].y, corners[3].x, corners[3].y)
            drawLine(corners[3].x, corners[3].y, corners[0].x, corners[0].y)
        }
        return output
    }
}