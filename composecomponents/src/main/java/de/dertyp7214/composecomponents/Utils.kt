package de.dertyp7214.composecomponents

import android.util.AttributeSet

object Utils {
    var cleanAttributeSet: AttributeSet = object : AttributeSet {
        override fun getAttributeCount() = 0
        override fun getAttributeName(index: Int) = null
        override fun getAttributeValue(index: Int) = null
        override fun getAttributeValue(namespace: String?, name: String?) = null
        override fun getPositionDescription(): String? = null
        override fun getAttributeNameResource(index: Int) = 0
        override fun getAttributeListValue(namespace: String?, attribute: String?, options: Array<String?>?, defaultValue: Int) = 0
        override fun getAttributeBooleanValue(namespace: String?, attribute: String?, defaultValue: Boolean) = false
        override fun getAttributeResourceValue(namespace: String?, attribute: String?, defaultValue: Int) = 0
        override fun getAttributeIntValue(namespace: String?, attribute: String?, defaultValue: Int) = 0
        override fun getAttributeUnsignedIntValue(namespace: String?, attribute: String?, defaultValue: Int) = 0
        override fun getAttributeFloatValue(namespace: String?, attribute: String?, defaultValue: Float) = 0f
        override fun getAttributeListValue(index: Int, options: Array<String?>?, defaultValue: Int) = 0
        override fun getAttributeBooleanValue(index: Int, defaultValue: Boolean) = false
        override fun getAttributeResourceValue(index: Int, defaultValue: Int) = 0
        override fun getAttributeIntValue(index: Int, defaultValue: Int) = 0
        override fun getAttributeUnsignedIntValue(index: Int, defaultValue: Int) = 0
        override fun getAttributeFloatValue(index: Int, defaultValue: Float) = 0f
        override fun getIdAttribute() = null
        override fun getClassAttribute() = null
        override fun getIdAttributeResourceValue(defaultValue: Int) = 0
        override fun getStyleAttribute() = 0
    }
}