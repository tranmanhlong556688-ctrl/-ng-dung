package vn.minh.lgirremote.ir

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager

class IrTransmitter(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    val isAvailable: Boolean get() = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR) && manager?.hasIrEmitter() == true

    fun send(profile: LgAcProtocol.Profile, raw: Int): Result<Int> = runCatching {
        check(isAvailable) { "Điện thoại không báo có bộ phát hồng ngoại." }
        val pattern = LgAcProtocol.buildPattern(raw, profile.timings)
        require(pattern.sum() < 2_000_000) { "Tín hiệu IR dài quá giới hạn Android." }
        manager!!.transmit(LgAcProtocol.CARRIER_HZ, pattern)
        raw
    }
}
