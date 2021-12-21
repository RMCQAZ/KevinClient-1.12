package kevin.skin

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kevin.file.FileManager
import kevin.utils.ChatUtils
import net.minecraft.client.Minecraft
import org.apache.commons.io.IOUtils
import java.io.*
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

object SkinManager {
    var nowSkin:Skin? = null
    var skinMode:SkinMode = SkinMode.Default
    private val jsonFile = File(FileManager.skinsDir,"skin.json")
    private val skinList = mutableListOf<Skin>()
    fun load(){
        skinList.clear()
        FileManager.skinsDir.listFiles().forEach { file ->
            if (file.isFile&&!file.name.equals(jsonFile.name)){
                try {
                    val args = file.name.split(".").toTypedArray()
                    skinList.add(loadSkinFromFile(java.lang.String.join(".", *args.copyOfRange(0, args.size - 1)),file))
                }catch (e: Exception){
                    Minecraft.LOGGER.error("Occurred an error while loading skin from file: ${file.name}")
                    e.printStackTrace()
                }
            }
        }
        if (!jsonFile.exists()) return
        val json = JsonParser().parse(IOUtils.toString(FileInputStream(jsonFile),"utf-8")).asJsonObject
        if(json.has("name")){
            val name=json.get("name").asString
            if(!name.equals("NONE")){
                val result = skinList.filter { it.name == name }
                if(result.isNotEmpty())
                    nowSkin=result[0]
            }
        }
        if (json.has("mode")){
            val mode=json.get("mode").asString
            skinMode = SkinMode.valueOf(mode)
        }
    }
    fun save(){
        val json = JsonObject()

        json.addProperty("name", if(nowSkin!=null){ nowSkin!!.name }else{ "NONE" })
        json.addProperty("mode", skinMode.name)

        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(jsonFile), StandardCharsets.UTF_8))
        writer.write(FileManager.PRETTY_GSON.toJson(json))
        writer.close()
    }
    fun set(name: String):Int{
        val list = skinList.filter { it.name == name }
        return if (list.isNotEmpty()){
            nowSkin = list[0]
            save()
            0
        } else {
            1
        }
    }
    fun setMode(mode: SkinMode){
        skinMode = mode
        save()
    }
    fun list(){
        if (skinList.isNotEmpty()){
            skinList.forEach {
                ChatUtils.messageWithPrefix("§b${it.name}")
            }
        } else {
            ChatUtils.messageWithPrefix("§bSkin List is Empty.")
        }
    }
    private fun loadSkinFromFile(name: String, file: File) = Skin(name,ImageIO.read(file))
    enum class SkinMode{
        Default,Slim
    }
}