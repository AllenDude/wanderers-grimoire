package com.allen.wanderersgrimoire

data class Folder(
    val id: String,
    var name: String
)

data class Item(
    val id: String,
    val type: String, // "note", "pin", or "task"
    var title: String,
    var body: String = "",
    var url: String = "",
    var folderId: String? = null,
    var done: Boolean = false,
    var imagePath: String? = null, // path inside filesDir/images/, null if no attachment
    val createdAt: Long = System.currentTimeMillis()
)
