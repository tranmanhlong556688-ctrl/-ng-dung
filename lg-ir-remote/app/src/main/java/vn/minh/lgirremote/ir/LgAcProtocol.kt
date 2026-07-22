package vn.minh.lgirremote.ir

import kotlin.math.max
import kotlin.math.min

object LgAcProtocol {
    const val CARRIER_HZ = 38_000
    const val OFF_COMMAND = 0x88C0051
    const val SWING_TOGGLE_COMMAND = 0x8810001
    const val SWING_AUTO_COMMAND = 0x8813149
    const val SWING_OFF_COMMAND = 0x881315A

    enum class Mode(val code: Int, val label: String) { COOL(0,"Làm lạnh"), DRY(1,"Hút ẩm"), FAN(2,"Quạt"), AUTO(3,"Tự động"), HEAT(4,"Sưởi") }
    enum class Fan(val code: Int, val label: String) { LOWEST(0,"Rất thấp"), LOW(1,"Thấp"), MEDIUM(2,"Trung bình"), MAX(4,"Cao"), AUTO(5,"Tự động"), LOW_ALT(9,"Thấp (LG2)"), HIGH_ALT(10,"Cao (LG2)") }
    enum class SwingStyle { TOGGLE, SET_AUTO_OFF }
    data class Timings(val headerMark:Int,val headerSpace:Int,val bitMark:Int,val oneSpace:Int,val zeroSpace:Int,val gap:Int=39_750,val repeatSpace:Int=2_250)
    data class Profile(val id:String,val displayName:String,val timings:Timings,val preferredFan:Fan,val swingStyle:SwingStyle)
    data class State(val power:Boolean=false,val temperature:Int=24,val mode:Mode=Mode.COOL,val fan:Fan=Fan.AUTO,val swing:Boolean=false)
    data class ScanCandidate(val profile:Profile,val state:State,val description:String)

    val profiles = listOf(
        Profile("lg2_standard","LG2 chuẩn – dòng AKB mới",Timings(3200,9900,480,1600,550),Fan.AUTO,SwingStyle.SET_AUTO_OFF),
        Profile("lg2_akb749","LG2 – AKB749/AKB733",Timings(3200,9900,480,1600,550),Fan.LOW_ALT,SwingStyle.SET_AUTO_OFF),
        Profile("lg_classic","LG cổ điển – 6711A/GE",Timings(8500,4250,550,1600,550),Fan.AUTO,SwingStyle.TOGGLE),
        Profile("lg_classic_9ms","LG cổ điển – timing 9 ms",Timings(9000,4500,560,1690,560),Fan.AUTO,SwingStyle.TOGGLE),
        Profile("lg2_tolerant","LG2 – timing mở rộng",Timings(3200,9850,500,1600,550),Fan.AUTO,SwingStyle.SET_AUTO_OFF)
    )
    val scanCandidates = buildList {
        for (profile in profiles) for (temperature in listOf(24,22,26)) add(ScanCandidate(profile,State(true,temperature,Mode.COOL,profile.preferredFan),"${profile.displayName}, Làm lạnh ${temperature}°C, quạt ${profile.preferredFan.label}"))
    }
    fun profileById(id:String?) = profiles.firstOrNull { it.id==id }
    fun encodeState(state:State):Int {
        if (!state.power) return OFF_COMMAND
        val t=min(30,max(16,state.temperature))
        var raw=0x8800000 or (state.mode.code shl 12) or ((t-15) shl 8) or (state.fan.code shl 4)
        return raw or checksum(raw)
    }
    fun checksum(rawWithoutChecksum:Int):Int { var value=rawWithoutChecksum ushr 4; var sum=0; repeat(4){sum+=value and 0xF; value=value ushr 4}; return sum and 0xF }
    fun isValidChecksum(raw:Int)=(raw and 0xF)==checksum(raw and 0xFFFFFF0)
    fun swingCommand(profile:Profile,enabled:Boolean)=when(profile.swingStyle){SwingStyle.TOGGLE->SWING_TOGGLE_COMMAND; SwingStyle.SET_AUTO_OFF->if(enabled) SWING_AUTO_COMMAND else SWING_OFF_COMMAND}
    fun buildPattern(raw:Int,t:Timings,includeRepeat:Boolean=true):IntArray {
        val values=ArrayList<Int>(64); values+=t.headerMark; values+=t.headerSpace
        for(bit in 27 downTo 0){values+=t.bitMark; values+=if(((raw ushr bit) and 1)==1)t.oneSpace else t.zeroSpace}
        values+=t.bitMark
        if(includeRepeat){values+=t.gap; values+=t.headerMark; values+=t.repeatSpace; values+=t.bitMark}
        return values.toIntArray()
    }
}
