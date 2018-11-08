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

package com.waicool20.wai2k.script.modules

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.DollType
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.*
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.Color
import java.awt.image.BufferedImage

private const val OCR_THRESHOLD = 2

class CombatModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<CombatModule>()

    override suspend fun execute() {
        if (!profile.combat.enabled) return
        switchDolls()
    }

    private suspend fun switchDolls() {
        navigator.navigateTo(LocationId.FORMATION)
        logger.info("Switching doll 2 of echelon 1")
        // Doll 2 region ( excludes stuff below name/type )
        region.subRegion(612, 167, 263, 667).clickRandomly()
        delay(100)
        applyFilters(1)
        scanValidDolls(1).shuffled().first().clickRandomly()
    }

    /**
     * Applies the filters ( stars and types ) in formation doll list
     */
    private suspend fun applyFilters(doll: Int) {
        logger.info("Applying doll filters for dragging doll $doll")
        val stars: Int
        val type: DollType
        with(profile.combat) {
            when (doll) {
                1 -> {
                    stars = doll1Stars
                    type = doll1Type
                }
                2 -> {
                    stars = doll2Stars
                    type = doll2Type
                }
                else -> error("Invalid doll: $doll")
            }
        }

        // Filter By button
        val filterButtonRegion = region.subRegion(1765, 348, 257, 161)
        filterButtonRegion.clickRandomly(); yield()
        // Filter popup region
        val prefix = "combat/formation/filters"
        region.subRegion(900, 159, 834, 910).run {
            logger.info("Resetting filters")
            find("$prefix/reset.png").clickRandomly(); delay(300)
            filterButtonRegion.clickRandomly(); yield()
            logger.info("Applying filter $stars star")
            find("$prefix/${stars}star.png").clickRandomly(); delay(100)
            logger.info("Applying filter $type")
            find("$prefix/$type.png").clickRandomly(); delay(100)
            logger.info("Confirming filters")
            find("$prefix/confirm.png").clickRandomly(); delay(100)
        }
    }

    /**
     * Scans for valid dolls in the formation doll list
     *
     * @return List of regions that can be clicked to select the valid doll
     */
    private suspend fun scanValidDolls(doll: Int): List<AndroidRegion> {
        logger.info("Scanning for valid dolls in filtered list for dragging doll $doll")
        val name: String
        val level: Int
        with(profile.combat) {
            when (doll) {
                1 -> {
                    name = doll1Name
                    level = doll1Level
                }
                2 -> {
                    name = doll2Name
                    level = doll2Level
                }
                else -> error("Invalid doll: $doll")
            }
        }
        // Temporary convenience class for storing doll regions
        class DollRegions(val nameRegion: BufferedImage, val levelRegion: BufferedImage, val clickRegion: AndroidRegion)
        // Optimize by taking a single screenshot and working on that
        val image = region.takeScreenshot()
        return region.findAllOrEmpty("combat/formation/lock.png")
                // Transform the lock region into 3 distinct regions used for ocr and clicking by offset
                .map {
                    DollRegions(
                            image.getSubimage(it.x + 67, it.y + 72, 161, 52),
                            image.getSubimage(it.x + 183, it.y + 124, 45, 32),
                            region.subRegion(it.x - 7, it.y, 244, 164)
                    )
                }
                // Filter by name
                .filterAsync(this) { Ocr.forConfig(config).doOCRAndTrim(it.nameRegion).distanceTo(name, Ocr.OCR_DISTANCE_MAP) < OCR_THRESHOLD }
                // Filter by level
                .filterAsync(this) {
                    it.levelRegion.binarizeImage().scale(2.0).pad(20, 10, Color.WHITE).let { bi ->
                        Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(bi).toIntOrNull() ?: 0 > level
                    }
                }
                // Return click regions
                .map { it.clickRegion }
                .also {
                    if (it.isEmpty()) {
                        error("Failed to find dragging doll $doll that matches criteria")
                    } else {
                        logger.info("Found ${it.size} dolls that match the criteria for doll $doll")
                    }
                }
    }
}