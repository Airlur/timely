package com.airlur.timely.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 日期页三种排序 */
enum class SortMode { CREATED, DATE, MANUAL }
/** 列表 / 网格 */
enum class ViewMode { LIST, GRID }
/** 置顶区单个 / 轮播 */
enum class PinMode { SINGLE, CAROUSEL }
/** 滑动操作两种模式 */
enum class SwipeMode { TRIGGER, REVEAL }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class LangMode { SYSTEM, ZH, EN }
enum class WmPos { BR, BL, TR, TL }
enum class WmOpacity { LOW, MID, HIGH }

/** 滑动卡片动作槽位（六个位置各绑一个动作） */
enum class SwipeAction { PIN, DETAIL, TIMELINE, EDIT, DELETE, SHARE }

data class UiSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val lang: LangMode = LangMode.SYSTEM,
    val sort: SortMode = SortMode.CREATED,
    val view: ViewMode = ViewMode.LIST,
    val pinMode: PinMode = PinMode.CAROUSEL,
    val swipeMode: SwipeMode = SwipeMode.TRIGGER,
    val slotsLeft: List<SwipeAction> = listOf(SwipeAction.PIN, SwipeAction.DETAIL, SwipeAction.TIMELINE),
    val slotsRight: List<SwipeAction> = listOf(SwipeAction.EDIT, SwipeAction.DELETE, SwipeAction.SHARE),
    val wmOn: Boolean = false,
    val wmPos: WmPos = WmPos.BR,
    val wmOp: WmOpacity = WmOpacity.LOW,
    val customSolids: List<Int> = emptyList(),
    val customGradients: List<Pair<Int, Int>> = emptyList()
)

class PrefsRepository(private val context: Context) {

    private object K {
        val theme = stringPreferencesKey("theme")
        val lang = stringPreferencesKey("lang")
        val sort = stringPreferencesKey("sort")
        val view = stringPreferencesKey("view")
        val pinMode = stringPreferencesKey("pin_mode")
        val swipeMode = stringPreferencesKey("swipe_mode")
        val slotsL = stringPreferencesKey("slots_left")
        val slotsR = stringPreferencesKey("slots_right")
        val wmOn = booleanPreferencesKey("wm_on")
        val wmPos = stringPreferencesKey("wm_pos")
        val wmOp = stringPreferencesKey("wm_op")
        val customSolids = stringPreferencesKey("custom_solids")
        val customGradients = stringPreferencesKey("custom_gradients")
    }

    val settings: Flow<UiSettings> = context.dataStore.data.map { p ->
        UiSettings(
            theme = p[K.theme]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            lang = p[K.lang]?.let { LangMode.valueOf(it) } ?: LangMode.SYSTEM,
            sort = p[K.sort]?.let { SortMode.valueOf(it) } ?: SortMode.CREATED,
            view = p[K.view]?.let { ViewMode.valueOf(it) } ?: ViewMode.LIST,
            pinMode = p[K.pinMode]?.let { PinMode.valueOf(it) } ?: PinMode.CAROUSEL,
            swipeMode = p[K.swipeMode]?.let { SwipeMode.valueOf(it) } ?: SwipeMode.TRIGGER,
            slotsLeft = p[K.slotsL]?.split(",")?.map { SwipeAction.valueOf(it) }
                ?: listOf(SwipeAction.PIN, SwipeAction.DETAIL, SwipeAction.TIMELINE),
            slotsRight = p[K.slotsR]?.split(",")?.map { SwipeAction.valueOf(it) }
                ?: listOf(SwipeAction.EDIT, SwipeAction.DELETE, SwipeAction.SHARE),
            wmOn = p[K.wmOn] ?: false,
            wmPos = p[K.wmPos]?.let { WmPos.valueOf(it) } ?: WmPos.BR,
            wmOp = p[K.wmOp]?.let { WmOpacity.valueOf(it) } ?: WmOpacity.LOW,
            customSolids = p[K.customSolids]?.split(",")?.filter { it.isNotBlank() }?.map { it.toInt() } ?: emptyList(),
            customGradients = p[K.customGradients]?.split(",")?.filter { it.isNotBlank() }
                ?.map { val t = it.split("|"); t[0].toInt() to t[1].toInt() } ?: emptyList()
        )
    }

    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[K.theme] = v.name }
    suspend fun setLang(v: LangMode) = context.dataStore.edit { it[K.lang] = v.name }
    suspend fun setSort(v: SortMode) = context.dataStore.edit { it[K.sort] = v.name }
    suspend fun setView(v: ViewMode) = context.dataStore.edit { it[K.view] = v.name }
    suspend fun setPinMode(v: PinMode) = context.dataStore.edit { it[K.pinMode] = v.name }
    suspend fun setSwipeMode(v: SwipeMode) = context.dataStore.edit { it[K.swipeMode] = v.name }
    suspend fun setSlots(left: List<SwipeAction>, right: List<SwipeAction>) = context.dataStore.edit {
        it[K.slotsL] = left.joinToString(",") { a -> a.name }
        it[K.slotsR] = right.joinToString(",") { a -> a.name }
    }
    suspend fun setWatermark(on: Boolean? = null, pos: WmPos? = null, op: WmOpacity? = null) =
        context.dataStore.edit {
            if (on != null) it[K.wmOn] = on
            pos?.let { p -> it[K.wmPos] = p.name }
            op?.let { o -> it[K.wmOp] = o.name }
        }
    suspend fun addCustomSolid(color: Int) = context.dataStore.edit {
        val cur = it[K.customSolids]?.split(",")?.filter { c -> c.isNotBlank() }.orEmpty()
        it[K.customSolids] = (cur + color.toString()).joinToString(",")
    }
    suspend fun addCustomGradient(start: Int, end: Int) = context.dataStore.edit {
        val cur = it[K.customGradients]?.split(",")?.filter { c -> c.isNotBlank() }.orEmpty()
        it[K.customGradients] = (cur + "$start|$end").joinToString(",")
    }

    suspend fun snapshot(): UiSettings = settings.first()
}
