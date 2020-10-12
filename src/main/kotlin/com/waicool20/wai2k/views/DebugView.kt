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
import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.util.asGrayF32
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.ai.ModelLoader
import com.waicool20.wai2k.util.ai.YoloTranslator
import com.waicool20.wai2k.util.ai.toDetectedObjects
import com.waicool20.wai2k.util.useCharFilter
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.*
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import net.sourceforge.tess4j.ITesseract
import tornadofx.*
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
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
    private val ocrImageView: ImageView by fxid()
    private val OCRButton: Button by fxid()
    private val resetOCRButton: Button by fxid()
    private val annotateSetButton: Button by fxid()
    private val saveAnnotationsCheckBox: CheckBox by fxid()

    private val useLSTMCheckBox: CheckBox by fxid()
    private val filterCheckBox: CheckBox by fxid()
    private val filterOptions: ToggleGroup by fxid()
    private val filterOptionsVBox: VBox by fxid()
    private val digitsOnlyRadioButton: RadioButton by fxid()
    private val customRadioButton: RadioButton by fxid()
    private val allowedCharsTextField: TextField by fxid()

    private var lastAndroidDevice: AndroidDevice? = null
    private var lastJob: Job? = null

    private val wai2KContext: Wai2KContext by inject()

    private val logger = loggerFor<DebugView>()

    private val predictor by lazy {
        try {
            val model = ModelLoader.loadModel(wai2KContext.wai2KConfig.assetsDirectory.resolve("models/gfl.pt"))
            model.setProperty("InputSize", "640")
            model.newPredictor(YoloTranslator(model, 0.6))
        } catch (e: Exception) {
            null
        }
    }

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
        annotateSetButton.setOnAction { annotateSet() }
    }

    private fun uiSetup() {
        filterOptionsVBox.disableWhen { filterCheckBox.selectedProperty().not() }
        createNewRenderJob()
        wai2KContext.wai2KConfig.lastDeviceSerialProperty
            .addListener("DebugViewDeviceListener") { _ -> createNewRenderJob() }
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
            while (isActive) {
                val predictor = this@DebugView.predictor
                val bImg = device.screens[0].capture()
                    .getSubimage(xSpinner.value, ySpinner.value, wSpinner.value, hSpinner.value)
                if (predictor == null) {
                    withContext(Dispatchers.JavaFx) {
                        ocrImageView.image = SwingFXUtils.toFXImage(bImg, null)
                    }
                } else {
                    val image = ImageFactory.getInstance().fromImage(bImg)
                    val objects = predictor.predict(image)
                    image.drawBoundingBoxes(objects.toDetectedObjects())
                    withContext(Dispatchers.JavaFx) {
                        ocrImageView.image = SwingFXUtils.toFXImage(image.wrappedImage as BufferedImage, null)
                    }
                }
            }
        }
    }

    private fun openPath() {
        FileChooser().apply {
            title = "Open path to an asset..."
            initialDirectory = if (pathField.text.isNotBlank()) {
                Paths.get(pathField.text).parent.toFile()
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
                val path = Paths.get(pathField.text)
                if (Files.exists(path)) {
                    logger.info("Finding $path")
                    val device = ADB.getDevices().find { it.serial == wai2KConfig.lastDeviceSerial }
                    if (device == null) {
                        logger.warn("Could not find device!")
                        return@launch
                    }
                    val image = device.screens[0].capture()
                        .getSubimage(xSpinner.value, ySpinner.value, wSpinner.value, hSpinner.value)
                        .asGrayF32()
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
                    logger.info("Took ${duration.inMilliseconds} ms")
                } else {
                    logger.warn("That asset doesn't exist!")
                }
            }
        }
    }

    private fun doAssetOCR() {
        launch(Dispatchers.IO) {
            val path = Paths.get(pathField.text)
            if (Files.exists(path)) {
                logger.info("Result: \n${getOCR().doOCR(path.toFile())}\n----------")
            } else {
                logger.warn("That asset doesn't exist!")
            }
        }
    }

    private fun doOCR() {
        launch(Dispatchers.IO) {
            lastAndroidDevice?.let {
                val image = it.screens[0].capture().let { bi ->
                    if (wSpinner.value > 0 && hSpinner.value > 0) {
                        bi.getSubimage(xSpinner.value, ySpinner.value, wSpinner.value, hSpinner.value)
                    } else bi
                }
                logger.info("Result: \n${getOCR().doOCR(image)}\n----------")
            }
        }
    }

    private fun getOCR(): ITesseract {
        val ocr = Ocr.forConfig(
            config = wai2KContext.wai2KConfig,
            digitsOnly = filterCheckBox.isSelected && filterOptions.selectedToggle == digitsOnlyRadioButton,
            useLSTM = useLSTMCheckBox.isSelected
        )
        if (filterCheckBox.isSelected && filterOptions.selectedToggle == customRadioButton) {
            ocr.useCharFilter(allowedCharsTextField.text)
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
            Files.createDirectories(output)

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
            val root = doc.createElement("annotations").also { doc.appendChild(it) }
            val version = doc.createElement("version").apply {
                appendChild(doc.createTextNode("1.1"))
            }
            root.appendChild(version)

            Files.walk(dir).asSequence()
                .filterNot { it.parent.endsWith("out") }
                .filter { "$it".endsWith(".png", true) || "$it".endsWith(".jpg", true) }
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
            }.transform(DOMSource(doc), StreamResult(Files.newOutputStream(output.resolve("annotations.xml"))))

            logger.info("All annotations done")
        }
    }
}