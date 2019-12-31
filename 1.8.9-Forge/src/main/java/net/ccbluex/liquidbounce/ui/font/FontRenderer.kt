package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Generate new bitmap based font renderer
 */
@SideOnly(Side.CLIENT)
class FontRenderer(val font: Font, startChar: Int = 0, stopChar: Int = 255) {

    private var fontHeight = -1
    private val charLocations = arrayOfNulls<CharLocation>(stopChar)

    private var textureID = 0
    private var textureWidth = 0
    private var textureHeight = 0

    val height: Int
        get() = (fontHeight - 8) / 2

    init {
        renderBitmap(startChar, stopChar)
    }

    /**
     * Allows you to draw a string with the target font
     *
     * @param text  to render
     * @param x     location for target position
     * @param y     location for target position
     * @param color of the text
     */
    fun drawString(text: String, x: Double, y: Double, color: Color) {
        GL11.glPushMatrix()
        GL11.glScaled(0.25, 0.25, 0.25)
        GlStateManager.bindTexture(textureID)
        RenderUtils.glColor(color)

        var currX = x * 2F
        for (char in text.toCharArray()) {
            val fontChar = charLocations[
                    if (char.toInt() < charLocations.size)
                        char.toInt()
                    else
                        '\u0003'.toInt()
            ] ?: continue

            drawChar(fontChar, currX.toFloat(), (y * 2F - 2F).toFloat())
            currX += fontChar.width - 8.0
        }

        GL11.glPopMatrix()
    }

    /**
     * Draw char from texture to display
     *
     * @param char target font char to render
     * @param x        target positon x to render
     * @param y        target potion y to render
     */
    private fun drawChar(char: CharLocation, x: Float, y: Float) {
        val width = char.width.toFloat()
        val height = char.height.toFloat()
        val srcX = char.x.toFloat()
        val srcY = char.y.toFloat()
        val renderX = srcX / textureWidth
        val renderY = srcY / textureHeight
        val renderWidth = width / textureWidth
        val renderHeight = height / textureHeight

        GL11.glBegin(GL11.GL_TRIANGLES)
        GL11.glTexCoord2f(renderX + renderWidth, renderY)
        GL11.glVertex2f(x + width, y)
        GL11.glTexCoord2f(renderX, renderY)
        GL11.glVertex2f(x, y)
        GL11.glTexCoord2f(renderX, renderY + renderHeight)
        GL11.glVertex2f(x, y + height)
        GL11.glTexCoord2f(renderX, renderY + renderHeight)
        GL11.glVertex2f(x, y + height)
        GL11.glTexCoord2f(renderX + renderWidth, renderY + renderHeight)
        GL11.glVertex2f(x + width, y + height)
        GL11.glTexCoord2f(renderX + renderWidth, renderY)
        GL11.glVertex2f(x + width, y)
        GL11.glEnd()
    }

    /**
     * Render font chars to a bitmap
     */
    private fun renderBitmap(startChar: Int, stopChar: Int) {
        val fontImages = arrayOfNulls<BufferedImage>(stopChar)
        var rowHeight = 0
        var charX = 0
        var charY = 0

        for (targetChar in startChar until stopChar) {
            val fontImage = drawCharToImage(targetChar.toChar())
            val fontChar = CharLocation(charX, charY, fontImage.width, fontImage.height)

            if (fontChar.height > fontHeight)
                fontHeight = fontChar.height
            if (fontChar.height > rowHeight)
                rowHeight = fontChar.height

            charLocations[targetChar] = fontChar
            fontImages[targetChar] = fontImage

            charX += fontChar.width

            if (charX > 2048) {
                if (charX > textureWidth)
                    textureWidth = charX

                charX = 0
                charY += rowHeight
                rowHeight = 0
            }
        }
        textureHeight = charY + rowHeight

        val bufferedImage = BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = bufferedImage.graphics as Graphics2D
        graphics2D.font = font
        graphics2D.color = Color(255, 255, 255, 0)
        graphics2D.fillRect(0, 0, textureWidth, textureHeight)
        graphics2D.color = Color.white

        for (targetChar in startChar until stopChar)
            if (fontImages[targetChar] != null && charLocations[targetChar] != null)
                graphics2D.drawImage(fontImages[targetChar], charLocations[targetChar]!!.x, charLocations[targetChar]!!.y,
                        null)

        textureID = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), bufferedImage, true,
                true)
    }

    /**
     * Draw a char to a buffered image
     *
     * @param ch char to render
     * @return image of the char
     */
    private fun drawCharToImage(ch: Char): BufferedImage {
        val graphics2D = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D

        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics2D.font = font

        val fontMetrics = graphics2D.fontMetrics

        var charWidth = fontMetrics.charWidth(ch) + 8
        if (charWidth <= 0)
            charWidth = 7

        var charHeight = fontMetrics.height + 3
        if (charHeight <= 0)
            charHeight = font.size

        val fontImage = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = fontImage.graphics as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.font = font
        graphics.color = Color.WHITE
        graphics.drawString(ch.toString(), 3, 1 + fontMetrics.ascent)

        return fontImage
    }

    /**
     * Calculate the string width of a text
     *
     * @param text for width calculation
     * @return the width of the text
     */
    fun getStringWidth(text: String): Int {
        var width = 0

        for (c in text.toCharArray()) {
            val fontChar = charLocations[
                    if (c.toInt() < charLocations.size)
                        c.toInt()
                    else
                        '\u0003'.toInt()
            ] ?: continue

            width += fontChar.width - 8
        }

        return width / 2
    }

    /**
     * Data class for saving char location of the font image
     */
    private data class CharLocation(var x: Int, var y: Int, var width: Int, var height: Int)
}