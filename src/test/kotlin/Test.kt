import com.waicool20.wai2k.android.AndroidDevice
import org.sikuli.script.ImagePath
import org.sikuli.script.Location
import java.util.*

fun main(args: Array<String>) {
    ImagePath.add(ClassLoader.getSystemClassLoader().getResource("images"))
    val device = AndroidDevice.listAll().first()
    device.displayPointerInfo(true)
    val random = Random()
    while (true) {
        device.screen.dragDrop(Location(random.nextInt(900) + 200, random.nextInt(900) + 200), Location(random.nextInt(900) + 200, random.nextInt(900) + 200))
    }
}
