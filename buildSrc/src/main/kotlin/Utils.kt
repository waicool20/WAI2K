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

import org.gradle.api.file.RegularFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

object Utils {
    fun md5sum(file: RegularFile) {
        md5sum(file.asFile)
    }

    fun md5sum(file: File) {
        val path = file.toPath()
        val md5File = Paths.get("$path.md5")
        val md5sum = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(path))
            .joinToString("") { String.format("%02x", it) }
        Files.write(md5File, md5sum.toByteArray())
    }
}
