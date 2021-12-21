package kevin.font

import kevin.utils.Mc
import net.minecraft.client.gui.FontRenderer
import java.awt.Font

object FontManager : Mc() {

    val minecraftFont: FontRenderer = mc.fontRenderer

    val font35 = GameFontRenderer(getFont("JetBrainsMono-Medium.ttf",35))

    val font40 = GameFontRenderer(getFont("JetBrainsMono-Medium.ttf",40))

    val font180 = GameFontRenderer(getFont("JetBrainsMono-Bold.ttf",180))

    private fun getFont(fontName: String, size: Int): Font {
        return try {
            val inputStream = FontManager::class.java.getResourceAsStream("fonts/$fontName")
            var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size.toFloat())
            inputStream.close()
            awtClientFont
        } catch (e: Exception) {
            e.printStackTrace()
            Font("default", Font.PLAIN, size)
        }
    }
}