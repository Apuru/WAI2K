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

package com.waicool20.wai2k.script

import com.waicool20.wai2k.android.AndroidRegion

open class Asset(
        val imageName: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
) {
    var prefix = ""
    val imagePath get() = "$prefix$imageName.png"

    fun getSubRegionFor(region: AndroidRegion): AndroidRegion {
        return region.subRegion(x, y, width, height)
    }
}