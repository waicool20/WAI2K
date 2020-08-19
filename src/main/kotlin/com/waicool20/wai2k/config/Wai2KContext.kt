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

package com.waicool20.wai2k.config

import javafx.beans.property.ObjectProperty
import tornadofx.*

data class Wai2KContext(
    val wai2KConfigProperty: ObjectProperty<Wai2KConfig> = Wai2KConfig().toProperty(),
    val versionInfoProperty: ObjectProperty<VersionInfo> = VersionInfo().toProperty(),
    val currentProfileProperty: ObjectProperty<Wai2KProfile> = Wai2KProfile().toProperty()
) : Component(), ScopedInstance {
    var wai2KConfig by wai2KConfigProperty
    var versionInfo by versionInfoProperty
    var currentProfile by currentProfileProperty
}
