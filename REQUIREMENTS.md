# Требования к разработке Android приложения "Medicare"

## Краткое резюме
Приложение для управления лекарствами и напоминаниями о их приеме. Позволяет пользователям добавлять лекарства, настраивать напоминания на определенное время и дни недели, получать уведомления в фоновом режиме. Поддерживает светлую/темную тему и локализацию (русский/английский).

**Ключевые технологии:** Jetpack Compose, Room, Navigation Compose, WorkManager, Material Design 3

## Общее описание
Приложение для уведомления о необходимости выпить лекарство в определенное время.

## Технологический стек
- **Jetpack Compose** - для UI
- **Room** - для локального хранения данных
- **Navigation Compose** - для навигации между экранами
- **Material Design 3** - для дизайна
- **WorkManager** - для фоновых задач и уведомлений
- **Kotlin Coroutines & Flow** - для асинхронных операций
- **Hilt/Dagger** (опционально) - для dependency injection

### Необходимые зависимости
Добавить в `app/build.gradle.kts`:
- Room: `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler` (kapt/ksp)
- Navigation Compose: `androidx.navigation:navigation-compose`
- WorkManager: `androidx.work:work-runtime-ktx`
- DataStore (для настроек): `androidx.datastore:datastore-preferences` (опционально, можно использовать SharedPreferences)
- ViewModel Compose: `androidx.lifecycle:lifecycle-viewmodel-compose`

## Основные возможности приложения

### Управление лекарствами
- Добавление лекарств с полями:
  - Название (обязательное)
  - Описание (опциональное)
  - Тип препарата: таблетка, капли, капсула, свечка, сироп, мазь, инъекция (выбор из списка)
  - Дозировка (например: "10 мг", "5 мл")
  - Количество единиц для приема (например: "1 таблетка", "2 капсулы")
  - Длительность приема (дата начала и окончания)
  - Цвет для визуального отличия (выбор из палитры)
  - Статус активности (включено/выключено)

- Редактирование существующих лекарств
- Удаление лекарств (с подтверждением)
- Включение/выключение лекарств без удаления

### Управление напоминаниями
- Создание напоминаний для каждого лекарства
- Максимум 5 напоминаний в день для одного лекарства
- Поля напоминания:
  - Связь с лекарством (выбор из списка)
  - Время напоминания (TimePicker)
  - Дни недели (чекбоксы для каждого дня: Пн, Вт, Ср, Чт, Пт, Сб, Вс)
  - Статус активности (включено/выключено)

- Редактирование существующих напоминаний
- Удаление напоминаний

### Уведомления
- Отправка push-уведомлений в установленное время
- Работа в фоновом режиме (даже когда приложение закрыто)
- Уведомления должны содержать:
  - Название лекарства
  - Дозировку и количество
  - Время приема

### Темы и локализация
- Поддержка светлой и темной темы (Material Design 3)
- Переключение темы в настройках
- Поддержка двух языков:
  - Русский (по умолчанию)
  - Английский
- Полная локализация всех строк интерфейса

### Технические требования
- Сохранение данных в локальную базу данных (Room)
- Обработка поворота экрана (сохранение состояния)
- Работа в фоновом режиме
- Корректная обработка перезапуска устройства (восстановление уведомлений)

## Экраны приложения

### 1. Главный экран (Home Screen)
**Путь навигации:** `/home`

**Описание:**
Отображает список будущих напоминаний с разделителями по дням (сегодня, завтра).

**Элементы:**
- Список карточек напоминаний для сегодня и завтра
- Разделители:
  - "Сегодня" - для напоминаний на текущий день
  - "Завтра" - для напоминаний на следующий день
- Каждая карточка содержит:
  - Цветовой индикатор лекарства (круглый)
  - Название лекарства
  - Дозировка и количество (например: "10 мг - 1")
  - Время напоминания (отображается справа)
  - Прошедшие напоминания сегодня отображаются красным цветом
- Карточки некликабельны (только для просмотра)

- FloatingActionButton (FAB):
  - Если лекарств нет: простая кнопка "+" → переход на экран добавления лекарства
  - Если есть хотя бы одно лекарство: расширяемая FAB с двумя опциями:
    - "Добавить напоминание" (иконка Notifications) → переход на экран создания напоминания
    - "Добавить лекарство" (иконка List) → переход на экран создания лекарства
  - При клике на фон раскрывающееся меню закрывается

- TopAppBar с кнопкой настроек (шестеренка) в правом верхнем углу

**Поведение:**
- Отображение напоминаний для сегодня и завтра
- Сортировка:
  - Прошедшие сегодня (по убыванию времени)
  - Будущие сегодня (по возрастанию времени)
  - Завтра (по возрастанию времени)
- Прошедшие напоминания сегодня выделяются красным цветом
- Отображение только активных лекарств и напоминаний
- Если напоминаний нет, отображается сообщение "Нет напоминаний" с подсказкой

### 2. Экран списка лекарств (Medicines List Screen)
**Путь навигации:** `/medicines`

**Описание:**
Полный список всех лекарств с возможностью управления.

**Элементы:**
- TopAppBar с кнопкой настроек (шестеренка)
- Список всех лекарств (карточки)
- Каждая карточка содержит:
  - Цветовой индикатор (круглый)
  - Название
  - Тип препарата (локализованный)
  - Дозировка и количество
  - Статус активности (переключатель)
  - Кнопка редактирования
  - Кнопка удаления

- FloatingActionButton "+" → переход на экран создания лекарства
- Если лекарств нет, отображается сообщение "Нет лекарств" с подсказкой нажать кнопку "+"

**Действия:**
- Нажатие на карточку → переход на экран редактирования
- Переключатель активности → включение/выключение лекарства
- Кнопка удаления → подтверждение и удаление

### 3. Экран списка напоминаний (Reminders List Screen) - Менеджер уведомлений
**Путь навигации:** `/reminders`

**Описание:**
Список всех напоминаний с возможностью управления. Доступен из нижней навигации как "Менеджер уведомлений".

**Элементы:**
- TopAppBar с кнопкой настроек (шестеренка)
- BottomNavigationBar
- Список всех активных напоминаний
- Каждая карточка напоминания содержит:
  - Название лекарства
  - Время напоминания (выделено цветом primary)
  - Дни недели (краткие названия, выходные дни красным цветом)
  - Статус активности (переключатель)
  - Кнопка редактирования
  - Кнопка удаления

- FloatingActionButton "+" → переход на экран создания напоминания

**Действия:**
- Нажатие на кнопку редактирования → переход на экран редактирования напоминания
- Переключатель активности → включение/выключение напоминания
- Кнопка удаления → подтверждение и удаление

### 4. Экран создания/редактирования лекарства (Medicine Form Screen)
**Путь навигации:** `/medicine/new` или `/medicine/{id}`

**Описание:**
Форма для создания или редактирования лекарства.

**Поля формы:**
- Название (TextField, обязательное)
- Описание (TextField, многострочное, опциональное)
- Тип препарата (DropdownMenu/ExposedDropdownMenu):
  - Таблетка
  - Капли
  - Капсула
  - Свечка
  - Сироп
  - Мазь
  - Инъекция
- Дозировка (TextField, например: "10 мг", "5 мл")
- Количество единиц (TextField, например: "1", "2")
- Длительность приема:
  - Дата начала (DatePicker, обязательное, по умолчанию текущая дата)
  - Дата окончания (DatePicker, опциональное, можно очистить)
- Цвет (выбор из предустановленной палитры с горизонтальной прокруткой):
  - 12 уникальных цветов
  - Горизонтальная прокрутка (LazyRow)
  - Визуальная индикация выбранного цвета (галочка и рамка)

**Кнопки:**
- "Отмена" (Cancel) → возврат на предыдущий экран без сохранения
- "Применить" (Apply/Save) → сохранение и возврат

**Валидация:**
- Название обязательно для заполнения
- Тип препарата обязателен для выбора
- Дата начала устанавливается автоматически в текущую дату при создании нового лекарства (если не указана)
- Дата окончания должна быть после даты начала (если указана)

### 5. Экран создания/редактирования напоминания (Reminder Form Screen)
**Путь навигации:** `/reminder/new` или `/reminder/{id}`

**Описание:**
Форма для создания или редактирования напоминания.

**Поля формы:**
- Выбор лекарства (DropdownMenu/ExposedDropdownMenu, обязательное)
- Время (TimePicker, обязательное)
- Дни недели (Checkboxes, минимум один день должен быть выбран):
  - Понедельник
  - Вторник
  - Среда
  - Четверг
  - Пятница
  - Суббота (красным цветом так как выходной)
  - Воскресенье (красным цветом так как выходной)

**Кнопки:**
- "Отмена" (Cancel) → возврат на предыдущий экран без сохранения
- "Применить" (Apply/Save) → сохранение и возврат

**Валидация:**
- Лекарство обязательно для выбора
- Время обязательно
- Минимум один день недели должен быть выбран
- Максимум 5 напоминаний в день для одного лекарства (проверка при сохранении нового напоминания, при редактировании существующего проверка не выполняется)

### 6. Экран настроек (Settings Screen)
**Путь навигации:** `/settings`

**Описание:**
Настройки приложения. Доступен через кнопку шестеренки в TopAppBar всех основных экранов.

**Элементы:**
- TopAppBar с кнопкой "Назад" (стрелка влево)
- Язык (Language):
  - Русский
  - English
  - FilterChip для выбора
- Тема (Theme):
  - Светлая (Light)
  - Темная (Dark)
  - Системная (System)
  - FilterChip для выбора

**Поведение:**
- Изменения темы применяются немедленно
- Изменение языка требует перезапуска Activity для применения локализации
- После перезапуска автоматически возвращается на экран настроек
- Сохранение настроек в DataStore

### 7. Экран "О нас" / "About" (About Screen)
**Путь навигации:** `/about`

**Описание:**
Информация о приложении и разработчике.

**Элементы:**
- TopAppBar с кнопкой настроек (шестеренка)
- BottomNavigationBar
- Иконка приложения (большая карточка с "MC")
- Название приложения: "MediCare"
- Версия приложения: "Версия 1.0" / "Version 1.0"
- Карточка с информацией о разработчике:
  - Автор: Dmitry Osin
  - Email: d@osin.pro (кликабельная ссылка, открывает почтовое приложение)
  - Сайт: osin.pro/medicare (кликабельная ссылка, открывает браузер)
- Копирайт с текущим годом внизу экрана

## Дизайн

### Material Design 3
- Использование Material Design 3 компонентов
- Поддержка динамических цветов (Material You) - опционально
- Адаптивный дизайн для разных размеров экранов

### Цветовая схема
- Светлая тема: стандартная Material Design 3 светлая палитра
- Темная тема: стандартная Material Design 3 темная палитра
- Поддержка пользовательских цветов для лекарств

### Типографика
- Material Design 3 типографика
- Поддержка локализации шрифтов

### Компоненты
- Использование Material 3 компонентов:
  - Cards для карточек лекарств и напоминаний
  - FloatingActionButton для основных действий (с расширяемым меню)
  - TextField для ввода текста
  - TimePicker для выбора времени
  - DatePicker для выбора даты
  - Checkbox для дней недели
  - Switch для переключателей
  - DropdownMenu (ExposedDropdownMenu) для выбора из списка
  - LazyRow для горизонтальной прокрутки (палитра цветов)
  - LazyColumn для списков
  - FilterChip для выбора языка и темы
  - NavigationBar для нижней навигации
  - TopAppBar с кнопками действий

## Архитектура данных

### Enum типов

#### MedicineType
```kotlin
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
```

#### DayOfWeek (для удобства работы с днями)
```kotlin
enum class DayOfWeek(val index: Int, val calendarDay: Int, val isWeekend: Boolean = false) {
    MONDAY(1, Calendar.MONDAY),
    TUESDAY(2, Calendar.TUESDAY),
    WEDNESDAY(3, Calendar.WEDNESDAY),
    THURSDAY(4, Calendar.THURSDAY),
    FRIDAY(5, Calendar.FRIDAY),
    SATURDAY(6, Calendar.SATURDAY, isWeekend = true),
    SUNDAY(7, Calendar.SUNDAY, isWeekend = true);

    fun getShortName(locale: Locale = Locale.getDefault()): String {
        return Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, calendarDay) }
            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale) ?: ""
    }

    fun getFullName(locale: Locale = Locale.getDefault()): String {
        return Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, calendarDay) }
            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale) ?: ""
    }
}
```

**Примечание:** Используется Calendar API для автоматической локализации названий дней недели.

### Room Database

**Примечание:** В текущей реализации используется `fallbackToDestructiveMigration()` для разработки, что означает пересоздание базы данных при изменении схемы. Для production версии необходимо добавить миграции.

#### Entity: Medicine
```kotlin
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val type: String, // MedicineType.name
    val dosage: String,
    val quantity: String,
    val startDate: Long, // timestamp
    val endDate: Long?, // timestamp, nullable
    val color: Int, // color value (ARGB)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### Entity: Reminder
```kotlin
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicineId"])]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val time: String, // HH:mm format (например: "09:00", "14:30")
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

**Примечание:** Используется `@ForeignKey` с `CASCADE` для автоматического удаления напоминаний при удалении лекарства. Также добавлен индекс на `medicineId` для оптимизации запросов.

#### DAO интерфейсы

**MedicineDao:**
```kotlin
@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY name")
    fun getAllActiveMedicines(): Flow<List<Medicine>>
    
    @Query("SELECT * FROM medicines ORDER BY name")
    fun getAllMedicines(): Flow<List<Medicine>>
    
    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long
    
    @Update
    suspend fun updateMedicine(medicine: Medicine)
    
    @Delete
    suspend fun deleteMedicine(medicine: Medicine)
    
    // Get medicines for current day (considering start/end dates)
    @Query("SELECT * FROM medicines WHERE isActive = 1 AND startDate <= :currentDate AND (endDate IS NULL OR endDate >= :currentDate) ORDER BY name")
    fun getMedicinesForDate(currentDate: Long): Flow<List<Medicine>>
}
```

**ReminderDao:**
```kotlin
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY time")
    fun getAllActiveReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE medicineId = :medicineId")
    fun getRemindersForMedicine(medicineId: Long): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?
    
    @Query("SELECT COUNT(*) FROM reminders WHERE medicineId = :medicineId AND isActive = 1")
    suspend fun getActiveReminderCountForMedicine(medicineId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
    
    // Получить активные напоминания для определенного дня недели и времени
    @Query("SELECT * FROM reminders WHERE isActive = 1 AND time = :time AND (CASE :dayOfWeek WHEN 1 THEN monday WHEN 2 THEN tuesday WHEN 3 THEN wednesday WHEN 4 THEN thursday WHEN 5 THEN friday WHEN 6 THEN saturday WHEN 7 THEN sunday END) = 1")
    suspend fun getRemindersForDayAndTime(dayOfWeek: Int, time: String): List<Reminder>
}
```

### Repository паттерн
Создать Repository классы для абстракции доступа к данным:
- `MedicineRepository` - работа с лекарствами
- `ReminderRepository` - работа с напоминаниями
- Использовать Flow для реактивных обновлений UI

## Навигация

### Navigation Graph
```
/home (HomeScreen)
  ├─> /medicine/new (MedicineFormScreen)
  ├─> /medicine/{id} (MedicineFormScreen)
  ├─> /reminder/new (ReminderFormScreen)
  ├─> /reminder/{id} (ReminderFormScreen)
  ├─> /medicines (MedicinesListScreen)
  ├─> /reminders (RemindersListScreen)
  ├─> /settings (SettingsScreen)
  └─> /about (AboutScreen)
```

### Bottom Navigation Bar
Нижняя навигация содержит 4 пункта:
- **Главная** (Home) - иконка Home
- **Лекарства** (Medicines) - иконка List
- **Напоминания** (Reminders) - иконка Notifications
- **О нас** (About) - иконка Info

**Примечание:** Настройки (Settings) доступны через кнопку шестеренки в TopAppBar всех основных экранов.

### TopAppBar
Все основные экраны используют компонент `AppTopBar` с:
- Заголовком экрана
- Кнопкой настроек (шестеренка) в правом верхнем углу (опционально)

## Фоновые задачи и уведомления

### WorkManager
- Использование WorkManager для планирования уведомлений
- Периодическая работа для проверки активных напоминаний
- Восстановление уведомлений после перезапуска устройства

**Рекомендуемый подход:**
1. При создании/обновлении напоминания планировать OneTimeWorkRequest для каждого дня недели
2. Использовать PeriodicWorkRequest для ежедневной проверки активных напоминаний (каждые 15 минут или при загрузке устройства)
3. При удалении/деактивации напоминания отменять соответствующие WorkRequest

### Notification Channels
- Создание канала уведомлений для напоминаний о лекарствах
- Настройка важности уведомлений (IMPORTANCE_HIGH для критических напоминаний)
- Канал должен быть создан при первом запуске приложения

**Пример создания канала:**
```kotlin
val channel = NotificationChannel(
    CHANNEL_ID,
    "Напоминания о лекарствах",
    NotificationManager.IMPORTANCE_HIGH
).apply {
    description = "Уведомления о необходимости принять лекарство"
    enableVibration(true)
    enableLights(true)
}
```

### Разрешения
- Для Android 13+ (API 33+) необходимо запрашивать разрешение `POST_NOTIFICATIONS`
- Проверять и запрашивать разрешение при первом запуске или при создании первого напоминания

## Локализация

### Поддерживаемые языки
- Русский (ru) - по умолчанию
- Английский (en)

### Файлы локализации
- `values/strings.xml` (русский)
- `values-en/strings.xml` (английский)

### Локализуемые элементы
- Все текстовые строки интерфейса
- Названия дней недели (используется Calendar API для автоматической локализации)
- Названия типов лекарств (автоматическая локализация на основе текущей локали приложения)
- Сообщения об ошибках
- Тексты кнопок и диалогов
- Заголовки разделов ("Сегодня", "Завтра")
- Сообщения о пустых списках с подсказками

## Иконка приложения
- Исходный файл скопирован в `app/src/main/res/drawable/app_icon.png`
- Необходимо создать адаптивную иконку:
  - Создать foreground и background drawable на основе `app_icon.png`
  - Обновить `ic_launcher.xml` и `ic_launcher_round.xml` в `mipmap-anydpi-v26`
  - Создать иконки для всех размеров (hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi) или использовать адаптивную иконку
- Альтернативно: использовать `app_icon.png` напрямую в манифесте как `android:icon="@drawable/app_icon"`

## Дополнительные требования

### Производительность
- Оптимизация запросов к базе данных
- Использование Flow для реактивных обновлений UI
- Кэширование данных где необходимо

### Обработка ошибок
- Валидация пользовательского ввода
- Обработка ошибок базы данных
- Информативные сообщения об ошибках

### Тестирование
- Unit тесты для ViewModel и Repository
- UI тесты для критических экранов (опционально)

### Безопасность
- Защита данных пользователя
- Корректная обработка разрешений на уведомления

## Структура проекта

### Рекомендуемая структура пакетов
```
pro.osin.tools.medicare/
├── data/
│   ├── database/
│   │   ├── MedicineDao.kt
│   │   ├── ReminderDao.kt
│   │   ├── MedicineDatabase.kt
│   │   └── entities/
│   ├── repository/
│   └── model/
├── domain/
│   ├── usecase/
│   └── model/
├── ui/
│   ├── theme/
│   ├── screens/
│   │   ├── home/
│   │   ├── medicines/
│   │   ├── reminders/
│   │   ├── medicineform/
│   │   ├── reminderform/
│   │   ├── settings/
│   │   └── about/
│   ├── components/
│   └── navigation/
└── util/
```

## AndroidManifest.xml требования

### Разрешения
```xml
<!-- Для уведомлений (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Для работы в фоне -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Настройки приложения
- `android:name` - указать Application класс (если используется)
- `android:icon` - иконка приложения
- `android:label` - название приложения
- `android:theme` - тема приложения

### Сервисы
- WorkManager автоматически регистрирует свои сервисы
- При необходимости можно добавить ForegroundService для критических уведомлений

## Приоритеты разработки

1. **Высокий приоритет:**
   - Базовая структура проекта и навигация
   - Room база данных и entities
   - Главный экран с отображением лекарств
   - Экран создания/редактирования лекарства
   - Экран создания/редактирования напоминания
   - Система уведомлений

2. **Средний приоритет:**
   - Экран списка лекарств
   - Экран списка напоминаний
   - Настройки темы и языка
   - Локализация

3. **Низкий приоритет:**
   - Экран "О приложении"
   - Дополнительные улучшения UX
   - Оптимизация производительности

## Дополнительные замечания для разработчика

### Важные моменты
1. **Обработка поворота экрана:**
   - Использовать `rememberSaveable` для сохранения состояния UI
   - ViewModel автоматически сохраняется при повороте
   - Room база данных сохраняет данные автоматически

2. **Фоновые уведомления:**
   - Использовать WorkManager для планирования уведомлений
   - Создавать периодические задачи для проверки активных напоминаний
   - Восстанавливать уведомления после перезапуска устройства через BootReceiver или WorkManager

3. **Локализация:**
   - Все строки должны быть в `strings.xml` (ru) и `strings-en/strings.xml` (en)
   - Использовать `stringResource()` в Compose
   - Локализовать названия дней недели, типов лекарств и т.д.

4. **Валидация данных:**
   - Проверять максимальное количество напоминаний (5 в день) перед сохранением
   - Валидировать даты (дата окончания после даты начала)
   - Проверять обязательные поля перед сохранением

5. **UX рекомендации:**
   - Использовать Snackbar для подтверждения действий (удаление, сохранение)
   - Показывать диалоги подтверждения для критических действий (удаление)
   - Использовать индикаторы загрузки при работе с базой данных
   - Обеспечить плавные анимации переходов между экранами

## Реализованные улучшения

### UX улучшения
- ✅ Сообщения о пустых списках с подсказками для пользователя
- ✅ Горизонтальная прокрутка палитры цветов (LazyRow)
- ✅ Уникальные цвета в палитре (12 цветов, без дубликатов)
- ✅ Визуальная индикация прошедших напоминаний (красный цвет)
- ✅ Разделители "Сегодня" и "Завтра" на главном экране
- ✅ Расширяемое меню FloatingActionButton на главном экране
- ✅ Кнопка настроек в TopAppBar для быстрого доступа
- ✅ Кликабельные ссылки на экране "О нас" (email и сайт)

### Навигация
- ✅ Настройки вынесены в TopAppBar (кнопка шестеренки на всех основных экранах)
- ✅ Менеджер уведомлений (Reminders) добавлен в нижнюю навигацию вместо настроек
- ✅ Убрана возможность редактирования с главного экрана (карточки только для просмотра)
- ✅ Создан переиспользуемый компонент AppTopBar

### Локализация
- ✅ Автоматическая локализация дней недели через Calendar API
- ✅ Автоматическая локализация типов лекарств на основе текущей локали приложения
- ✅ Переименование "О приложении" в "О нас" для русской версии
- ✅ Все комментарии в коде переведены на английский язык

### Главный экран
- ✅ Отображение списка будущих напоминаний с группировкой по дням
- ✅ Прошедшие напоминания сегодня отображаются красным цветом
- ✅ Сортировка: прошедшие сегодня (по убыванию), будущие сегодня (по возрастанию), завтра (по возрастанию)
- ✅ Компактные карточки напоминаний с цветовым индикатором лекарства

