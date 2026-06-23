package com.example.data.database

import androidx.room.TypeConverter
import com.example.model.DesignLayer
import com.example.model.FilterProperties
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class DatabaseConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromDesignLayerList(layers: List<DesignLayer>?): String {
        if (layers == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, DesignLayer::class.java)
        val adapter = moshi.adapter<List<DesignLayer>>(type)
        return adapter.toJson(layers)
    }

    @TypeConverter
    fun toDesignLayerList(json: String?): List<DesignLayer> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, DesignLayer::class.java)
        val adapter = moshi.adapter<List<DesignLayer>>(type)
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFilterProperties(filter: FilterProperties?): String {
        if (filter == null) return "{}"
        val adapter = moshi.adapter(FilterProperties::class.java)
        return adapter.toJson(filter)
    }

    @TypeConverter
    fun toFilterProperties(json: String?): FilterProperties {
        if (json.isNullOrEmpty()) return FilterProperties()
        val adapter = moshi.adapter(FilterProperties::class.java)
        return try {
            adapter.fromJson(json) ?: FilterProperties()
        } catch (e: Exception) {
            FilterProperties()
        }
    }
}
