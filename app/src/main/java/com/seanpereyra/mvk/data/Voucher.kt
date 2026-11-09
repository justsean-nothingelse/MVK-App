package com.seanpereyra.mvk.data

import org.json.JSONObject
import java.util.UUID

/**
 * One Wi-Fi voucher slip, e.g.
 *
 *   CICTLAB          [153]
 *        Voucher Code
 *   [      FWxWU       ]
 *   [        6h        ]
 */
data class Voucher(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val network: String = "CICTLAB",
    val slot: String = "",
    val duration: String = "",
    val used: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("code", code)
        put("network", network)
        put("slot", slot)
        put("duration", duration)
        put("used", used)
        put("addedAt", addedAt)
    }

    companion object {
        fun fromJson(o: JSONObject) = Voucher(
            id = o.optString("id", UUID.randomUUID().toString()),
            code = o.optString("code"),
            network = o.optString("network", "CICTLAB"),
            slot = o.optString("slot"),
            duration = o.optString("duration"),
            used = o.optBoolean("used", false),
            addedAt = o.optLong("addedAt", System.currentTimeMillis()),
        )
    }
}
