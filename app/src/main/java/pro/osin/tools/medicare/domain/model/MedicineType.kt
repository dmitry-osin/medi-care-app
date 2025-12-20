package pro.osin.tools.medicare.domain.model

import java.util.Locale

enum class MedicineType(val displayNameRu: String, val displayNameEn: String) {
    TABLET("Таблетка", "Tablet"),
    DROPS("Капли", "Drops"),
    CAPSULE("Капсула", "Capsule"),
    SUPPOSITORY("Свечка", "Suppository"),
    SYRUP("Сироп", "Syrup"),
    OINTMENT("Мазь", "Ointment"),
    INJECTION("Инъекция", "Injection");

    fun getDisplayName(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "en") displayNameEn else displayNameRu
    }
}

