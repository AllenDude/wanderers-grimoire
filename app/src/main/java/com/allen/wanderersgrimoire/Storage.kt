package com.allen.wanderersgrimoire

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Everything lives in SharedPreferences as two JSON blobs, folders and items.
 * No backend, no database file, matches how the web version does it in
 * localStorage.
 */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("wanderers_grimoire", Context.MODE_PRIVATE)

    val folders = mutableListOf<Folder>()
    val items = mutableListOf<Item>()

    fun load() {
        folders.clear()
        items.clear()

        val foldersJson = prefs.getString("folders", null)
        if (foldersJson != null) {
            val arr = JSONArray(foldersJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                folders.add(Folder(o.getString("id"), o.getString("name")))
            }
        } else {
            folders.add(Folder(UUID.randomUUID().toString(), "Recipes"))
            folders.add(Folder(UUID.randomUUID().toString(), "Useful things"))
        }

        val itemsJson = prefs.getString("items", null)
        if (itemsJson != null) {
            val arr = JSONArray(itemsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(
                    Item(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        title = o.getString("title"),
                        body = o.optString("body", ""),
                        url = o.optString("url", ""),
                        folderId = if (o.isNull("folderId")) null else o.optString("folderId", null),
                        done = o.optBoolean("done", false),
                        imagePath = if (o.isNull("imagePath")) null else o.optString("imagePath", null),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        save() // persists the default folders on first run
    }

    fun save() {
        val foldersArr = JSONArray()
        folders.forEach { f ->
            foldersArr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
            })
        }

        val itemsArr = JSONArray()
        items.forEach { i ->
            itemsArr.put(JSONObject().apply {
                put("id", i.id)
                put("type", i.type)
                put("title", i.title)
                put("body", i.body)
                put("url", i.url)
                put("folderId", i.folderId)
                put("done", i.done)
                put("imagePath", i.imagePath)
                put("createdAt", i.createdAt)
            })
        }

        prefs.edit()
            .putString("folders", foldersArr.toString())
            .putString("items", itemsArr.toString())
            .apply()
    }
}
