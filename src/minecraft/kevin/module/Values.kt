package kevin.module

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kevin.file.FileManager
import java.util.*

abstract class Value<T>(val name: String, var value: T) {

    fun set(newValue: T) {
        if (newValue == value)
            return

        val oldValue = get()

        try {
            onChange(oldValue, newValue)
            changeValue(newValue)
            onChanged(oldValue, newValue)
            FileManager.saveConfig(FileManager.modulesConfig)
        } catch (e: Exception) {
            println("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
        }
    }

    fun get() = value

    open fun changeValue(value: T) {
        this.value = value
    }

    abstract fun toJson(): JsonElement?
    abstract fun fromJson(element: JsonElement)

    protected open fun onChange(oldValue: T, newValue: T) {}
    protected open fun onChanged(oldValue: T, newValue: T) {}

}

open class BooleanValue(name: String, value: Boolean) : Value<Boolean>(name, value) {

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive)
            value = element.asBoolean || element.asString.equals("true", ignoreCase = true)
    }

}

open class IntegerValue(name: String, value: Int, val minimum: Int = 0, val maximum: Int = Integer.MAX_VALUE)
    : Value<Int>(name, value) {

    fun set(newValue: Number) {
        set(newValue.toInt())
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive)
            value = element.asInt
    }

}

open class FloatValue(name: String, value: Float, val minimum: Float = 0F, val maximum: Float = Float.MAX_VALUE)
    : Value<Float>(name, value) {

    fun set(newValue: Number) {
        set(newValue.toFloat())
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive)
            value = element.asFloat
    }
}

open class DoubleValue(name: String, value: Double, val minimum: Double = .0, val maximum: Double = Double.MAX_VALUE)
    : Value<Double>(name, value) {

    fun set(newValue: Number) {
        set(newValue.toDouble())
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive)
            value = element.asDouble
    }
}

open class TextValue(name: String, value: String) : Value<String>(name, value) {

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive)
            value = element.asString
    }
}

class BlockValue(name: String, value: Int) : IntegerValue(name, value, 1, 197)

open class ListValue(name: String, val values: Array<String>, value: String) : Value<String>(name, value) {

    @JvmField
    var openList = false

    init {
        this.value = value
    }

    infix fun equal(other: String):Boolean{
        return this.get().equals(other,true)
    }

    operator fun contains(string: String?): Boolean {
        return Arrays.stream(values).anyMatch { s: String -> s.equals(string, ignoreCase = true) }
    }

    override fun changeValue(value: String) {
        for (element in values) {
            if (element.equals(value, ignoreCase = true)) {
                this.value = element
                break
            }
        }
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) changeValue(element.asString)
    }
}