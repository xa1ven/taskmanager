package com.example.todolist.data.local

import androidx.room.TypeConverter
import com.example.todolist.data.model.RelatedTaskResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromRelatedTaskList(value: List<RelatedTaskResponse>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toRelatedTaskList(value: String?): List<RelatedTaskResponse>? =
        value?.let {
            val type = object : TypeToken<List<RelatedTaskResponse>>() {}.type
            gson.fromJson(it, type)
        }
}
