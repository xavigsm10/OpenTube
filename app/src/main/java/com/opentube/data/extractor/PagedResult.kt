package com.opentube.data.extractor

import org.schabi.newpipe.extractor.Page
import com.google.gson.Gson

/**
 * Resultado paginado - Almacena el objeto Page serializado para paginación correcta
 */
data class PagedResult<T>(
    val items: List<T>,
    val nextPageUrl: String?, // Serialized Page object as JSON
    val hasMore: Boolean = nextPageUrl != null
) {
    companion object {
        private val gson = Gson()
        
        /**
         * Serializa un objeto Page a JSON string
         */
        fun serializePage(page: Page?): String? {
            if (page == null) return null
            return try {
                // Guardar URL, ID y body si existen
                gson.toJson(PageData(
                    url = page.url ?: "",
                    id = page.id,
                    body = page.body
                ))
            } catch (e: Exception) {
                page.url // Fallback to just URL
            }
        }
        
        /**
         * Deserializa un JSON string a objeto Page
         */
        fun deserializePage(serialized: String?): Page? {
            if (serialized == null) return null
            return try {
                val data = gson.fromJson(serialized, PageData::class.java)
                // Usar constructor apropiado según los datos disponibles
                when {
                    data.id != null && data.body != null -> Page(data.url, data.id, data.body)
                    data.id != null -> Page(data.url, data.id)
                    data.body != null -> Page(data.url, data.body)
                    else -> Page(data.url)
                }
            } catch (e: Exception) {
                // Fallback: try as plain URL
                try {
                    Page(serialized)
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Datos internos para serialización de Page
 */
private data class PageData(
    val url: String,
    val id: String? = null,
    val body: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PageData
        return url == other.url && id == other.id && body.contentEquals(other.body)
    }
    override fun hashCode(): Int = arrayOf(url, id, body).contentHashCode()
}
