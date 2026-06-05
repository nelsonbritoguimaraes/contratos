package com.contractops.api.time.bridge.adapters

import com.contractops.api.time.bridge.ClockBridgeAdapter
import com.contractops.api.time.bridge.ClockVendor
import com.contractops.api.time.bridge.GenericAfdAdapter
import org.springframework.stereotype.Component
import java.util.*

@Component
class ZKTecoAdapter(private val genericAfdAdapter: GenericAfdAdapter) : ClockBridgeAdapter {
    override fun getVendor() = ClockVendor.ZKTECO
    override fun importPunches(content: String, deviceId: UUID?, tenantId: UUID) =
        genericAfdAdapter.importPunches(content, deviceId, tenantId).copy(vendor = getVendor())
}
