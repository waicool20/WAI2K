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

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.util.asGrayF32
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.ai.GFLObject
import com.waicool20.wai2k.util.ai.ModelLoader
import com.waicool20.wai2k.util.ai.YoloTranslator
import com.waicool20.wai2k.util.ai.toDetectedObjects
import com.waicool20.wai2k.util.useCharFilter
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.invert
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.tooltips.TooltipSide
import com.waicool20.waicoolutils.javafx.tooltips.fadeAfter
import com.waicool20.waicoolutils.javafx.tooltips.showAt
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import net.sourceforge.tess4j.ITesseract
import tornadofx.*
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.math.roundToInt
import kotlin.streams.asSequence
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


class DebugView : CoroutineScopeView() {
    override val root: VBox by fxml("/views/debug.fxml")
    private val openButton: Button by fxid()
    private val testButton: Button by fxid()
    private val pathField: TextField by fxid()
    private val assetOCRButton: Button by fxid()

    private val xSpinner: Spinner<Int> by fxid()
    private val ySpinner: Spinner<Int> by fxid()
    private val wSpinner: Spinner<Int> by fxid()
    private val hSpinner: Spinner<Int> by fxid()
    private val copyBoundsButton: Button by fxid()
    private val pasteBoundsButton: Button by fxid()
    private val windowBoundsButton: Button by fxid()
    private val ocrImageView: ImageView by fxid()
    private val OCRButton: Button by fxid()
    private val saveButton: Button by fxid()
    private val resetOCRButton: Button by fxid()
    private val annotatePreviewCheckBox: CheckBox by fxid()
    private val annotateSetButton: Button by fxid()
    private val saveAnnotationsCheckBox: CheckBox by fxid()

    private val filterCheckBox: CheckBox by fxid()
    private val filterOptions: ToggleGroup by fxid()
    private val filterOptionsVBox: VBox by fxid()
    private val digitsOnlyRadioButton: RadioButton by fxid()
    private val customRadioButton: RadioButton by fxid()
    private val allowedCharsTextField: TextField by fxid()

    private val invertCheckBox: CheckBox by fxid()
    private val thresholdSpinner: Spinner<Double> by fxid()

    private var lastAndroidDevice: AndroidDevice? = null
    private var lastJob: Job? = null

    private val wai2KContext: Wai2KContext by inject()

    private val logger = loggerFor<DebugView>()

    private var model: Model? = null
    private var predictor: Predictor<Image, List<GFLObject>>? = null

    private var mouseEvent: MouseEvent? = null

    init {
        title = "WAI2K - Debugging tools"
    }

    override fun onDock() {
        super.onDock()
        uiSetup()
        openButton.setOnAction { openPath() }
        testButton.setOnAction { testPath() }
        assetOCRButton.setOnAction { doAssetOCR() }
        OCRButton.setOnAction { doOCR() }
        resetOCRButton.setOnAction { createNewRenderJob() }
        annotatePreviewCheckBox.selectedProperty().addListener("DebugAPCB") { newVal ->
            onAnnotatePreviewCheckBox(newVal)
        }
        annotateSetButton.setOnAction { annotateSet() }
        root.children.filterIsInstance(TitledPane::class.java).forEach { tp ->
            tp.expandedProperty().addListener("Debug${tp.id}") { _ ->
                tp.scene.window.sizeToScene()
                tp.requestLayout()
            }
        }
        ocrImageView.setOnMousePressed {
            mouseEvent = it
            Tooltip("Start selection ${it.x} ${it.y}").apply {
                fadeAfter(500)
                showAt(fxmlLoader.namespace["previewLabel"] as Node, TooltipSide.TOP_LEFT)
            }
        }
        val handler = EventHandler<MouseEvent> { e ->
            val se = mouseEvent ?: return@EventHandler
            Tooltip("Stop selection ${e.x} ${e.y}").apply {
                fadeAfter(500)
                showAt(fxmlLoader.namespace["previewLabel"] as Node, TooltipSide.TOP_LEFT)
            }
            val newX = xSpinner.value + (se.x / ocrImageView.boundsInParent.width * wSpinner.value)
                .roundToInt()
            val newY = ySpinner.value + (se.y / ocrImageView.boundsInParent.height * hSpinner.value)
                .roundToInt()
            val newW = ((e.x - se.x) / ocrImageView.boundsInParent.width * wSpinner.value)
                .roundToInt()
            val newH = ((e.y - se.y) / ocrImageView.boundsInParent.height * hSpinner.value)
                .roundToInt()
            if (newW > 0 && newH > 0) {
                xSpinner.valueFactory.value = newX
                ySpinner.valueFactory.value = newY
                wSpinner.valueFactory.value = newW
                hSpinner.valueFactory.value = newH
            }
            mouseEvent = null
        }
        ocrImageView.onMouseReleased = handler
        ocrImageView.onMouseExited = handler

        thresholdSpinner.valueFactory =
            SpinnerValueFactory.DoubleSpinnerValueFactory(-0.1, 1.0, -1.0, 0.1)

        copyBoundsButton.setOnAction {
            Clipboard.getSystemClipboard()
                .putString("${xSpinner.value}, ${ySpinner.value}, ${wSpinner.value}, ${hSpinner.value}")
            Tooltip("Copied coordinates").apply {
                fadeAfter(500)
                showAt(copyBoundsButton)
            }
        }
        pasteBoundsButton.setOnAction {
            Regex("(\\d+),?\\s*?(\\d+),?\\s*?(\\d+),?\\s*?(\\d+)")
                .find(Clipboard.getSystemClipboard().string)
                ?.destructured?.let { (x, y, w, h) ->
                    xSpinner.valueFactory.value = x.toInt()
                    ySpinner.valueFactory.value = y.toInt()
                    wSpinner.valueFactory.value = w.toInt()
                    hSpinner.valueFactory.value = h.toInt()
                }
        }
        windowBoundsButton.setOnAction {
            xSpinner.valueFactory.value = 455
            ySpinner.valueFactory.value = 151
            wSpinner.valueFactory.value = 1281
            hSpinner.valueFactory.value = 929
        }
        saveButton.setOnAction {
            FileChooser().apply {
                title = "Save screenshot to?"
                extensionFilters.add(FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"))
                showSaveDialog(null)?.let { file ->
                    launch(Dispatchers.IO) {
                        ImageIO.write(grabScreenshot(), "PNG", file)
                    }
                }
            }
        }
    }

    private fun onAnnotatePreviewCheckBox(newVal: Boolean) {
        launch(Dispatchers.IO) {
            if (newVal) {
                if (model == null) {
                    model = try {
                        ModelLoader.loadModel(
                            wai2KContext.wai2KConfig.assetsDirectory.resolve("models/gfl.pt")
                        ).apply { setProperty("InputSize", "640") }
                    } catch (e: Exception) {
                        logger.error("Error loading annotation model", e)
                        null
                    }
                }
                model?.let { predictor = it.newPredictor(YoloTranslator(it, 0.6)) }
            } else {
                predictor?.close()
                predictor = null
            }
        }
    }

    override fun onUndock() {
        predictor?.close()
        predictor = null
        model?.close()
        model = null
        super.onUndock()
    }

    private fun uiSetup() {
        createNewRenderJob()
        wai2KContext.wai2KConfig.lastDeviceSerialProperty
            .addListener("DebugViewDeviceListener") { _ -> createNewRenderJob() }
    }

    private fun grabScreenshot(): BufferedImage? {
        return try {
            val img = lastAndroidDevice?.screens?.firstOrNull()
                ?.capture()
                ?.getSubimage(xSpinner.value, ySpinner.value, wSpinner.value, hSpinner.value)
            if (thresholdSpinner.value >= 0.0) img?.binarizeImage(thresholdSpinner.value)
            if (invertCheckBox.isSelected) img?.invert()
            img
        } catch (e: Region.CaptureIOException) {
            logger.warn("Capture error!")
            null
        }
    }

    private fun createNewRenderJob(serial: String = wai2KContext.wai2KConfig.lastDeviceSerial) {
        val device = ADB.getDevices().find { it.serial == serial }
            .also { lastAndroidDevice = it } ?: return
        lastJob?.cancel()
        lastJob = launch(Dispatchers.IO) {
            withContext(Dispatchers.JavaFx) {
                val maxWidth = device.properties.displayWidth
                val maxHeight = device.properties.displayHeight
                xSpinner.valueFactory = IntegerSpinnerValueFactory(0, maxWidth, 0)
                ySpinner.valueFactory = IntegerSpinnerValueFactory(0, maxHeight, 0)
                wSpinner.valueFactory = IntegerSpinnerValueFactory(0, maxWidth, maxWidth)
                hSpinner.valueFactory = IntegerSpinnerValueFactory(0, maxHeight, maxHeight)

                xSpinner.valueProperty().addListener("DebugViewXSpinner") { newVal ->
                    if (newVal + wSpinner.value > maxWidth) {
                        wSpinner.valueFactory.value = maxWidth - newVal
                    }
                }
                ySpinner.valueProperty().addListener("DebugViewYSpinner") { newVal ->
                    if (newVal + hSpinner.value > maxHeight) {
                        hSpinner.valueFactory.value = maxHeight - newVal
                    }
                }
                wSpinner.valueProperty().addListener("DebugViewWSpinner") { newVal ->
                    if (newVal + xSpinner.value > maxWidth) {
                        wSpinner.valueFactory.value = maxWidth - xSpinner.value
                    }
                }
                hSpinner.valueProperty().addListener("DebugViewHSpinner") { newVal ->
                    if (newVal + ySpinner.value > maxHeight) {
                        hSpinner.valueFactory.value = maxHeight - ySpinner.value
                    }
                }
            }
            while (coroutineContext.isActive) {
                val predictor = this@DebugView.predictor
                var bImg = grabScreenshot() ?: continue
                if (predictor != null) {
                    try {
                        val image = ImageFactory.getInstance().fromImage(bImg)
                        val objects = predictor.predict(image)
                        image.drawBoundingBoxes(objects.toDetectedObjects())
                        bImg = image.wrappedImage as BufferedImage
                    } catch (e: IllegalStateException) {
                        // Do nothing, predictor was prob closed
                    }
                } else {
                    delay(100) // Add some delay, otherwise it might glitch out
                }
                withContext(Dispatchers.JavaFx) {
                    ocrImageView.image = SwingFXUtils.toFXImage(bImg, null)
                }
            }
        }
    }

    private fun openPath() {
        FileChooser().apply {
            title = "Open path to an asset..."
            initialDirectory = if (pathField.text.isNotBlank()) {
                Path(pathField.text).parent.toFile()
            } else {
                wai2KContext.wai2KConfig.assetsDirectory.toFile()
            }
            extensionFilters.add(FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"))
            showOpenDialog(null)?.let {
                pathField.text = it.path
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun testPath() {
        launch(Dispatchers.IO) {
            wai2KContext.apply {
                val path = Path(pathField.text)
                if (path.exists()) {
                    logger.info("Finding $path")
                    val device = ADB.getDevices().find { it.serial == wai2KConfig.lastDeviceSerial }
                    if (device == null) {
                        logger.warn("Could not find device!")
                        return@launch
                    }
                    val image = grabScreenshot()?.asGrayF32() ?: return@launch
                    val matcher = device.screens[0].matcher
                    matcher.settings.matchDimension = ScriptRunner.HIGH_RES
                    // Set similarity to 0.6f to make cvauto report the similarity value down to 0.6
                    val (results, duration) = measureTimedValue {
                        try {
                            matcher.findBest(FileTemplate(path, 0.6), image, 20)
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            val max = e.localizedMessage.takeLastWhile { it.isDigit() }.toInt() - 1
                            matcher.findBest(FileTemplate(path, 0.6), image, max)
                        }
                    }
                    matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
                    results.takeIf { it.isNotEmpty() }
                        ?.sortedBy { it.score }
                        ?.forEach {
                            logger.info("Found ${path.fileName}: $it")
                        } ?: run { logger.warn("Could not find the asset anywhere") }
                    logger.info("Took ${duration.inWholeMilliseconds} ms")
                } else {
                    logger.warn("That asset doesn't exist!")
                }
            }
        }
    }

    private fun doAssetOCR() {
        launch(Dispatchers.IO) {
            val path = Path(pathField.text)
            if (path.exists()) {
                logger.info("Result: \n${getOCR().doOCR(path.toFile())}\n----------")
            } else {
                logger.warn("That asset doesn't exist!")
            }
        }
    }

    private fun doOCR() {
        launch(Dispatchers.IO) {
            val img = grabScreenshot() ?: return@launch
            logger.info("Result: \n${getOCR().doOCR(img)}\n----------")
        }
    }

    private fun getOCR(): ITesseract {
        val ocr = Ocr.forConfig(wai2KContext.wai2KConfig)
        if (filterCheckBox.isSelected) {
            when (filterOptions.selectedToggle) {
                digitsOnlyRadioButton -> {
                    ocr.useCharFilter(Ocr.DIGITS)
                }
                customRadioButton -> {
                    ocr.useCharFilter(allowedCharsTextField.text)
                }
            }

        }
        return ocr
    }

    private fun annotateSet() {
        launch(Dispatchers.IO) {
            val predictor = predictor ?: return@launch

            val dir = withContext(Dispatchers.JavaFx) {
                DirectoryChooser().apply {
                    title = "Annotate which directory?"
                }.showDialog(null)?.toPath()
            } ?: return@launch

            val output = dir.resolve("out")
            logger.info("Annotating images in $dir")
            output.createDirectories()

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
            val root = doc.createElement("annotations").also { doc.appendChild(it) }
            val version = doc.createElement("version").apply {
                appendChild(doc.createTextNode("1.1"))
            }
            root.appendChild(version)

            Files.walk(dir).asSequence()
                .filterNot { it.parent.endsWith("out") }
                .filter { it.extension == "png" || it.extension == "jpg" }
                .sorted()
                .forEachIndexed { i, path ->
                    val image = ImageFactory.getInstance().fromFile(path)
                    val objects = predictor.predict(image)
                    val imageNode = doc.createElement("image").apply {
                        setAttribute("id", "$i")
                        setAttribute("name", "${dir.parent.relativize(path)}")
                        setAttribute("width", "${image.width}")
                        setAttribute("height", "${image.height}")
                    }
                    objects.forEach { obj ->
                        val bbox = obj.bbox
                        doc.createElement("box").apply {
                            setAttribute("label", "$obj")
                            setAttribute("occluded", "0")
                            setAttribute("xtl", "${bbox.x * image.width}")
                            setAttribute("ytl", "${bbox.y * image.height}")
                            setAttribute("xbr", "${(bbox.x + bbox.width) * image.width}")
                            setAttribute("ybr", "${(bbox.y + bbox.height) * image.height}")
                        }.also { imageNode.appendChild(it) }
                    }
                    root.appendChild(imageNode)
                    if (saveAnnotationsCheckBox.isSelected) {
                        image.drawBoundingBoxes(objects.toDetectedObjects())
                        image.save(Files.newOutputStream(output.resolve(path.fileName)), "png")
                    }
                    logger.info("Image: $path\n$objects")
                }

            TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            }.transform(
                DOMSource(doc),
                StreamResult(Files.newOutputStream(output.resolve("annotations.xml")))
            )

            logger.info("All annotations done")
        }
    }
}