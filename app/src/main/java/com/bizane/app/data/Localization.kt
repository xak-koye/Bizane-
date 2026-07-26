package com.bizane.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// MARK: - Language

enum class AppLanguage(val code: String, val title: String, val isRTL: Boolean) {
    CKB("ckb", "کوردی", true),
    EN("en", "English", false);

    companion object {
        fun fromCode(code: String?): AppLanguage = values().find { it.code == code } ?: CKB
    }
}

/**
 * زمانی ئێستای ئەپ. لە Compose ـدا وەک state ـێک ڕەفتار دەکات، بۆیە هەر Composable ـێک
 * کە AppLang.current بخوێنێتەوە خۆکارانە دووبارە دەکێشرێتەوە کاتێک زمان دەگۆڕدرێت
 * (هاوشێوەی Notification.Name.languageChanged لە iOS، بەڵام بە شێوازی Compose).
 */
object AppLang {
    private const val KEY = "app_language"

    private var _current by mutableStateOf(AppLanguage.CKB)
    var current: AppLanguage
        get() = _current
        set(value) {
            _current = value
            Prefs.sp.edit().putString(KEY, value.code).apply()
        }

    /** پێویستە لە BizaneApp.onCreate پاش Prefs.init بانگ بکرێت */
    fun load() {
        _current = AppLanguage.fromCode(Prefs.sp.getString(KEY, null))
    }
}

// MARK: - Translation table

private val translations: Map<String, Map<AppLanguage, String>> = mapOf(
    // Common
    "common.ok" to mapOf(AppLanguage.CKB to "باشە", AppLanguage.EN to "OK"),
    "common.cancel" to mapOf(AppLanguage.CKB to "پاشگەزبوونەوە", AppLanguage.EN to "Cancel"),
    "common.close" to mapOf(AppLanguage.CKB to "داخستن", AppLanguage.EN to "Close"),
    "common.save" to mapOf(AppLanguage.CKB to "پاشەکەوت", AppLanguage.EN to "Save"),
    "common.done" to mapOf(AppLanguage.CKB to "تەواو", AppLanguage.EN to "Done"),
    "common.delete" to mapOf(AppLanguage.CKB to "سڕینەوە", AppLanguage.EN to "Delete"),
    "common.no" to mapOf(AppLanguage.CKB to "نەخێر", AppLanguage.EN to "No"),
    "common.undo" to mapOf(AppLanguage.CKB to "گەڕاندنەوە", AppLanguage.EN to "Undo"),
    "common.areYouSure" to mapOf(AppLanguage.CKB to "دڵنیایت؟", AppLanguage.EN to "Are you sure?"),
    "common.yesDelete" to mapOf(AppLanguage.CKB to "بەڵێ، بسڕەوە", AppLanguage.EN to "Yes, delete"),
    "common.success" to mapOf(AppLanguage.CKB to "سەرکەوتوو بوو ✅", AppLanguage.EN to "Success ✅"),
    "common.failed" to mapOf(AppLanguage.CKB to "سەرکەوتوو نەبوو", AppLanguage.EN to "Failed"),
    "common.tryAgain" to mapOf(AppLanguage.CKB to "هەوڵدانەوە بکە.", AppLanguage.EN to "Please try again."),
    "common.error" to mapOf(AppLanguage.CKB to "هەڵە", AppLanguage.EN to "Error"),

    // Tabs
    "tab.main" to mapOf(AppLanguage.CKB to "سەرەکی", AppLanguage.EN to "Foods"),
    "tab.settings" to mapOf(AppLanguage.CKB to "رێکخستنەکان", AppLanguage.EN to "Settings"),

    // Main list
    "main.title" to mapOf(AppLanguage.CKB to "خواردنەکان", AppLanguage.EN to "Foods"),
    "search.placeholder" to mapOf(AppLanguage.CKB to "🔍  گەڕان بەدوای خواردندا...", AppLanguage.EN to "🔍  Search foods..."),
    "list.empty" to mapOf(AppLanguage.CKB to "🛒\n\nهیچ خواردنێک نییە\nبستێنە + بۆ زیادکردن", AppLanguage.EN to "🛒\n\nNo foods yet\nTap + to add one"),
    "list.emptySearch" to mapOf(AppLanguage.CKB to "🔍\n\nهیچ ئەنجامێک نەدۆزرایەوە", AppLanguage.EN to "🔍\n\nNo results found"),
    "undo.deleted" to mapOf(AppLanguage.CKB to "\"%s\" سڕایەوە", AppLanguage.EN to "\"%s\" deleted"),
    "item.deleteConfirmMsg" to mapOf(AppLanguage.CKB to "ئایا دەتەوێت \"%s\" بسڕیتەوە؟ ئەم کارە ناگەڕێتەوە.", AppLanguage.EN to "Delete \"%s\"? This can't be undone."),

    // Sort
    "sort.title" to mapOf(AppLanguage.CKB to "ڕیزکردنی لیست", AppLanguage.EN to "Sort list"),
    "sort.soonest" to mapOf(AppLanguage.CKB to "نزیکترین بەسەرچوون", AppLanguage.EN to "Expiring soonest"),
    "sort.latest" to mapOf(AppLanguage.CKB to "دوورترین بەسەرچوون", AppLanguage.EN to "Expiring latest"),
    "sort.nameAZ" to mapOf(AppLanguage.CKB to "ناو (ئا-یی)", AppLanguage.EN to "Name (A-Z)"),

    // Categories
    "cat.all" to mapOf(AppLanguage.CKB to "هەموو", AppLanguage.EN to "All"),
    "cat.food" to mapOf(AppLanguage.CKB to "خواردن", AppLanguage.EN to "Food"),
    "cat.drink" to mapOf(AppLanguage.CKB to "نەساتل", AppLanguage.EN to "Drink"),
    "cat.fridge" to mapOf(AppLanguage.CKB to "بەراد", AppLanguage.EN to "Fridge"),

    // Item status
    "status.expired" to mapOf(AppLanguage.CKB to "بەسەرچووە!", AppLanguage.EN to "Expired!"),
    "status.today" to mapOf(AppLanguage.CKB to "ئەمڕۆ بەسەردەچێت", AppLanguage.EN to "Expires today"),
    "status.oneDayLeft" to mapOf(AppLanguage.CKB to "١ رۆژ ماوە", AppLanguage.EN to "1 day left"),
    "status.daysLeft" to mapOf(AppLanguage.CKB to "%d رۆژ ماوە", AppLanguage.EN to "%d days left"),
    "status.weeksLeft" to mapOf(AppLanguage.CKB to "%d هەفتە ماوە", AppLanguage.EN to "%d weeks left"),
    "status.weeksDaysLeft" to mapOf(AppLanguage.CKB to "%d هەفتە، %d رۆژ ماوە", AppLanguage.EN to "%d weeks, %d days left"),
    "badge.daysLeft" to mapOf(AppLanguage.CKB to " %dڕۆژ ", AppLanguage.EN to " %dd "),

    // Add / Edit item
    "add.addTitle" to mapOf(AppLanguage.CKB to "زیاد بکە", AppLanguage.EN to "Add Item"),
    "add.editTitle" to mapOf(AppLanguage.CKB to "دەستکاری بکە", AppLanguage.EN to "Edit Item"),
    "add.photoLabel" to mapOf(AppLanguage.CKB to "وێنەی خواردنەکە", AppLanguage.EN to "Photo of item"),
    "add.namePlaceholder" to mapOf(AppLanguage.CKB to "ناوی خواردن", AppLanguage.EN to "Item name"),
    "add.barcodeLabel" to mapOf(AppLanguage.CKB to "بارکۆد", AppLanguage.EN to "Barcode"),
    "add.barcodePlaceholder" to mapOf(AppLanguage.CKB to "بارکۆد (ئارەزوومەندانە)", AppLanguage.EN to "Barcode (optional)"),
    "add.scanHint" to mapOf(AppLanguage.CKB to "بارکۆدەکە بخەرە ناو چوارچێوەکە", AppLanguage.EN to "Point the camera at the barcode"),
    "add.cameraPermissionMsg" to mapOf(AppLanguage.CKB to "بۆ سکانکردنی بارکۆد پێویستە ڕێگە بە کامێرا بدرێت لە ڕێکخستنەکانی مۆبایل", AppLanguage.EN to "Please allow camera access in Settings to scan barcodes"),
    "add.barcodeNotFound" to mapOf(AppLanguage.CKB to "بەرهەمەکە نەدۆزرایەوە، بەدەستی ناوی خواردنەکە بنووسە", AppLanguage.EN to "Product not found — please enter the name manually"),
    "add.categoryLabel" to mapOf(AppLanguage.CKB to "جۆری خواردن", AppLanguage.EN to "Category"),
    "add.purchaseDateLabel" to mapOf(AppLanguage.CKB to "بەرواری کڕین", AppLanguage.EN to "Purchase date"),
    "add.expiryDateLabel" to mapOf(AppLanguage.CKB to "بەرواری بەسەرچون", AppLanguage.EN to "Expiry date"),
    "add.notesLabel" to mapOf(AppLanguage.CKB to "تێبینی", AppLanguage.EN to "Notes"),
    "add.notifyLabel" to mapOf(AppLanguage.CKB to "ئاگادارکردنەوەی بەسەرچوون", AppLanguage.EN to "Expiry notification"),
    "add.daysToday" to mapOf(AppLanguage.CKB to "لە ڕۆژی بەسەرچوون خۆیدا", AppLanguage.EN to "On the expiry day itself"),
    "add.daysBefore" to mapOf(AppLanguage.CKB to "%d ڕۆژ پێش بەسەرچوون", AppLanguage.EN to "%d day(s) before expiry"),
    "add.deleteBtn" to mapOf(AppLanguage.CKB to "🗑  سڕینەوە", AppLanguage.EN to "🗑  Delete"),
    "add.deleteConfirmMsg" to mapOf(AppLanguage.CKB to "ئایا دەتەوێت بیسڕیتەوە؟", AppLanguage.EN to "Are you sure you want to delete this?"),
    "add.camera" to mapOf(AppLanguage.CKB to "📷 کامێرا", AppLanguage.EN to "📷 Camera"),
    "add.gallery" to mapOf(AppLanguage.CKB to "🖼 گەلەری وێنەکان", AppLanguage.EN to "🖼 Photo Library"),
    "add.errNoName" to mapOf(AppLanguage.CKB to "تکایە ناوی خواردنەکە بنووسە", AppLanguage.EN to "Please enter the item's name"),
    "add.errDateOrder" to mapOf(AppLanguage.CKB to "بەرواری بەسەرچون دەبێت لەدوای بەرواری کڕین بێت", AppLanguage.EN to "Expiry date must be after purchase date"),

    // Settings
    "settings.notifSection" to mapOf(AppLanguage.CKB to "🔔  ئاگادارکردنەوە", AppLanguage.EN to "🔔  Notifications"),
    "settings.notifQuestion" to mapOf(AppLanguage.CKB to "ئاگادارکردنەوە پێش چەند رۆژ؟", AppLanguage.EN to "Notify how many days before?"),
    "settings.notifDaysOpt" to mapOf(AppLanguage.CKB to "%d رۆژ", AppLanguage.EN to "%d day"),
    "settings.accountSection" to mapOf(AppLanguage.CKB to "🔐  پاراستنی هەژمار / باکئەپ", AppLanguage.EN to "🔐  Account & Backup"),
    "settings.unlinkedWarning" to mapOf(AppLanguage.CKB to "هەژمارت پارێزراو نیە. ئەگەر مۆبایل بگۆڕیت، مەوادەکانت لەدەست دەچن.", AppLanguage.EN to "Your account isn't backed up. If you change phones, your data will be lost."),
    "settings.linkedStatus" to mapOf(AppLanguage.CKB to "پارێزراوە بە Google ✅  (%s)", AppLanguage.EN to "Backed up with Google ✅  (%s)"),
    "settings.linkGoogle" to mapOf(AppLanguage.CKB to "پارێزگاری بکە بە  Google", AppLanguage.EN to "Link with Google"),
    "settings.linkSuccessMsg" to mapOf(AppLanguage.CKB to "ئێستا دەتوانیت باکئەپ بگریت.", AppLanguage.EN to "You can now create backups."),
    "settings.unlinkGoogle" to mapOf(AppLanguage.CKB to "چوونە دەرەوە لە Google", AppLanguage.EN to "Sign out of Google"),
    "settings.lastBackup" to mapOf(AppLanguage.CKB to "دوایین باکئەپ: %s", AppLanguage.EN to "Last backup: %s"),
    "settings.noBackupYet" to mapOf(AppLanguage.CKB to "هێشتا هیچ باکئەپێک نەگیراوە", AppLanguage.EN to "No backup yet"),
    "settings.autoBackupBtn" to mapOf(AppLanguage.CKB to "🔄  باکئەپی خۆکارانە: %s", AppLanguage.EN to "🔄  Auto backup: %s"),
    "settings.autoBackupQuestion" to mapOf(AppLanguage.CKB to "باکئەپی خۆکارانە کەی؟", AppLanguage.EN to "When should auto backup run?"),
    "settings.backupBtn" to mapOf(AppLanguage.CKB to "☁️  دروستکردنی باکئەپ", AppLanguage.EN to "☁️  Create Backup"),
    "settings.restoreBtn" to mapOf(AppLanguage.CKB to "⬇️  گەڕاندنەوەی باکئەپ", AppLanguage.EN to "⬇️  Restore Backup"),
    "settings.backingUp" to mapOf(AppLanguage.CKB to "باکئەپ دەکرێت...", AppLanguage.EN to "Backing up..."),
    "settings.restoring" to mapOf(AppLanguage.CKB to "دەگەڕێندرێتەوە...", AppLanguage.EN to "Restoring..."),
    "settings.restoreConfirmTitle" to mapOf(AppLanguage.CKB to "گەڕاندنەوەی باکئەپ؟", AppLanguage.EN to "Restore backup?"),
    "settings.restoreConfirmMsg" to mapOf(AppLanguage.CKB to "هەموو خواردنەکانی ئێستای ئەم مۆبایلە دەگۆڕدرێن بە خواردنەکانی باکئەپەکە. ئەم کارە ناگەڕێتەوە.", AppLanguage.EN to "All current foods on this phone will be replaced with the backup. This can't be undone."),
    "settings.restoreConfirmYes" to mapOf(AppLanguage.CKB to "بەڵێ، بگەڕێنەوە", AppLanguage.EN to "Yes, restore"),
    "settings.backupSuccessMsg" to mapOf(AppLanguage.CKB to "خواردنەکانت باکئەپ کران بۆ Google Drive.", AppLanguage.EN to "Your foods were backed up to Google Drive."),
    "settings.restoreSuccessMsg" to mapOf(AppLanguage.CKB to "خواردنەکانت گەڕێندرانەوە.", AppLanguage.EN to "Your foods were restored."),
    "settings.signOutTitle" to mapOf(AppLanguage.CKB to "چوونە دەرەوە لە Google؟", AppLanguage.EN to "Sign out of Google?"),
    "settings.signOutMsg" to mapOf(AppLanguage.CKB to "ئەم مۆبایلە دەبێتەوە بێ باکئەپ (ئەگەر مۆبایل بگۆڕیت یان ئەپ بسڕیتەوە، داتاکانت پارێزراو نامێننەوە)، بەڵام خواردنەکانت لەم مۆبایلە دەمێننەوە وەک خۆیان.", AppLanguage.EN to "This phone will no longer be backed up (if you change phones or delete the app, your data won't be preserved), but your foods will stay as they are on this phone."),
    "settings.signOutConfirm" to mapOf(AppLanguage.CKB to "چوونە دەرەوە", AppLanguage.EN to "Sign out"),
    "settings.statsSection" to mapOf(AppLanguage.CKB to "📊  ئامار", AppLanguage.EN to "📊  Stats"),
    "stats.total" to mapOf(AppLanguage.CKB to "کۆی گشتی", AppLanguage.EN to "Total"),
    "stats.ok" to mapOf(AppLanguage.CKB to "باش", AppLanguage.EN to "OK"),
    "stats.soon" to mapOf(AppLanguage.CKB to "نزیک", AppLanguage.EN to "Soon"),
    "stats.expired" to mapOf(AppLanguage.CKB to "بەسەرچوو", AppLanguage.EN to "Expired"),
    "settings.trashSection" to mapOf(AppLanguage.CKB to "🧹  سڕاوەکان", AppLanguage.EN to "🧹  Trash"),
    "settings.aboutSection" to mapOf(AppLanguage.CKB to "ℹ️  دەربارە", AppLanguage.EN to "ℹ️  About"),
    "settings.appName" to mapOf(AppLanguage.CKB to "bizane", AppLanguage.EN to "bizane"),
    "settings.version" to mapOf(AppLanguage.CKB to "وەشان ٢.٠", AppLanguage.EN to "Version 2.0"),
    "settings.madeByTeam" to mapOf(AppLanguage.CKB to "دروستکراوە لەلایەن ستافی ئارا تیمەوە", AppLanguage.EN to "Made by the Ara team"),
    "settings.purpose" to mapOf(AppLanguage.CKB to "ئامانجی ئەپەکە ئاگاداربوونتە لە بەرواری بەسەرچوونی خواردنەکانت،\nتاوەکو هیچ خواردنێکت بەفیڕۆ نەچێت.", AppLanguage.EN to "This app's purpose is to keep you aware of your food's expiry dates,\nso nothing goes to waste."),
    "settings.developer" to mapOf(AppLanguage.CKB to "گەشەپێدەر: xak koye", AppLanguage.EN to "Developer: xak koye"),
    "settings.contactSection" to mapOf(AppLanguage.CKB to "📞  پەیوەندی", AppLanguage.EN to "📞  Contact"),
    "settings.facebook" to mapOf(AppLanguage.CKB to "فەیسبووک", AppLanguage.EN to "Facebook"),
    "settings.facebookSub" to mapOf(AppLanguage.CKB to "پەیوەندیمان پێوە بکە", AppLanguage.EN to "Get in touch with us"),
    "settings.github" to mapOf(AppLanguage.CKB to "گیتهەب", AppLanguage.EN to "GitHub"),
    "settings.githubSub" to mapOf(AppLanguage.CKB to "کۆدی سەرچاوە", AppLanguage.EN to "View source code"),
    "settings.aboutContactSection" to mapOf(AppLanguage.CKB to "ℹ️  دەربارە و پەیوەندی", AppLanguage.EN to "ℹ️  About & Contact"),
    "settings.aboutContactBtn" to mapOf(AppLanguage.CKB to "ℹ️  دەربارەی ئەپ و پەیوەندی", AppLanguage.EN to "ℹ️  About app & Contact"),
    "settings.accountBackupBtn" to mapOf(AppLanguage.CKB to "🔐  هەژمار و باکئەپ", AppLanguage.EN to "🔐  Account & Backup"),
    "settings.accountRowSubtitleLinked" to mapOf(AppLanguage.CKB to "پارێزراوە بە Google  ·  %s", AppLanguage.EN to "Backed up with Google  ·  %s"),
    "settings.accountRowSubtitleUnlinked" to mapOf(AppLanguage.CKB to "پارێزراو نیە — کلیک بکە بۆ پاراستن", AppLanguage.EN to "Not backed up — tap to secure"),
    "settings.notifRowSubtitle" to mapOf(AppLanguage.CKB to "ئاگادارکردنەوە %d رۆژ پێش بەسەرچوون", AppLanguage.EN to "Notify %d day(s) before expiry"),
    "settings.languageRowSubtitle" to mapOf(AppLanguage.CKB to "%s", AppLanguage.EN to "%s"),
    "settings.trashRowSubtitle" to mapOf(AppLanguage.CKB to "%d ئایتم سڕاوە", AppLanguage.EN to "%d deleted item(s)"),
    "settings.aboutRowSubtitle" to mapOf(AppLanguage.CKB to "زانیاری ئەپ و لینکی پەیوەندی", AppLanguage.EN to "App info & contact links"),
    "settings.generalSection" to mapOf(AppLanguage.CKB to "گشتی", AppLanguage.EN to "GENERAL"),
    "settings.dataSection" to mapOf(AppLanguage.CKB to "🗑  داتا", AppLanguage.EN to "🗑  Data"),
    "settings.clearAll" to mapOf(AppLanguage.CKB to "سڕینەوەی هەموو خواردنەکان", AppLanguage.EN to "Delete all foods"),
    "settings.clearAllMsg" to mapOf(AppLanguage.CKB to "هەموو خواردنەکان دەسڕێتەوە. ئەم کارە ناگەڕێتەوە!", AppLanguage.EN to "All foods will be deleted. This can't be undone!"),
    "settings.yesDeleteAll" to mapOf(AppLanguage.CKB to "بەڵێ، هەموویان بسڕەوە", AppLanguage.EN to "Yes, delete all"),
    "settings.languageSection" to mapOf(AppLanguage.CKB to "🌐  زمان", AppLanguage.EN to "🌐  Language"),
    "settings.languageQuestion" to mapOf(AppLanguage.CKB to "زمانی ئەپ هەڵبژێرە", AppLanguage.EN to "Choose app language"),

    // Auto backup modes
    "autobackup.onOpen" to mapOf(AppLanguage.CKB to "هەر کاتێک ئەپ دەکرێتەوە", AppLanguage.EN to "Every time app opens"),
    "autobackup.daily" to mapOf(AppLanguage.CKB to "ڕۆژانە", AppLanguage.EN to "Daily"),
    "autobackup.weekly" to mapOf(AppLanguage.CKB to "هەفتانە", AppLanguage.EN to "Weekly"),
    "autobackup.manual" to mapOf(AppLanguage.CKB to "بە دەست (تەنیا کاتێک کلیک دەکەم)", AppLanguage.EN to "Manual (only when I tap)"),

    // Trash screen
    "trash.title" to mapOf(AppLanguage.CKB to "سڕاوەکان", AppLanguage.EN to "Trash"),
    "trash.empty" to mapOf(AppLanguage.CKB to "🧹\n\nهیچ ئایتمێکی سڕاوە نییە", AppLanguage.EN to "🧹\n\nNo deleted items"),
    "trash.clearAllMsg" to mapOf(AppLanguage.CKB to "هەموو تۆمارەکانی سڕاوەکان بەتاڵ دەکرێتەوە. ئەم کارە ناگەڕێتەوە.", AppLanguage.EN to "All deleted item records will be cleared. This can't be undone."),
    "trash.yesEmpty" to mapOf(AppLanguage.CKB to "بەڵێ، بەتاڵی بکەوە", AppLanguage.EN to "Yes, clear"),
    "trash.deleteForever" to mapOf(AppLanguage.CKB to "سڕینەوەی هەتاهەتایی", AppLanguage.EN to "Delete Forever"),
    "trash.restore" to mapOf(AppLanguage.CKB to "↩︎ گەڕاندنەوە", AppLanguage.EN to "↩︎ Restore"),
    "trash.deletedAt" to mapOf(AppLanguage.CKB to "سڕایەوە لە %s", AppLanguage.EN to "Deleted on %s"),

    // Notifications
    "notif.title" to mapOf(AppLanguage.CKB to "⏰ بەسەرچوونی خواردن نزیکە", AppLanguage.EN to "⏰ Food expiry approaching"),
    "notif.bodyToday" to mapOf(AppLanguage.CKB to "%s ئەمڕۆ بەسەردەچێت!", AppLanguage.EN to "%s expires today!"),
    "notif.bodyDays" to mapOf(AppLanguage.CKB to "%s لە %d ڕۆژی تردا بەسەردەچێت.", AppLanguage.EN to "%s expires in %d day(s)."),
)

/**
 * وەرگێڕانی وشە/ڕستەیەک بۆ زمانی ئێستای هەڵبژێردراو. لە Composable ـدا بانگی بکە
 * (AppLang.current دەخوێنێتەوە، بۆیە خۆکارانە نوێ دەبێتەوە کاتێک زمان دەگۆڕدرێت).
 * بۆ format string: L("status.daysLeft", 3) دەبێتە "3 رۆژ ماوە".
 */
fun L(key: String, vararg args: Any): String {
    val lang = AppLang.current
    val format = translations[key]?.get(lang) ?: translations[key]?.get(AppLanguage.CKB) ?: key
    return if (args.isEmpty()) format else String.format(format, *args)
}

/** ناوی مانگەکان بەپێی زمانی هەڵبژێردراو */
object LocalizedMonths {
    private val ckb = listOf(
        "کانونی دووەم", "شوبات", "ئازار", "نیسان", "ئایار", "حوزەیران",
        "تەمووز", "ئاب", "ئەیلوول", "تشرینی یەکەم", "تشرینی دووەم", "کانونی یەکەم"
    )
    private val en = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val current: List<String> get() = if (AppLang.current == AppLanguage.EN) en else ckb
}
