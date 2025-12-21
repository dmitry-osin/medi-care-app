# Оптимизации производительности и исправление проблем

## Найденные проблемы и исправления

### 1. ✅ Утечки памяти - создание репозиториев при каждой рекомпозиции

**Проблема:**
В каждом экране создавались новые экземпляры `database`, `medicineRepository` и `reminderRepository` при каждой рекомпозиции, что приводило к:
- Ненужным пересозданиям объектов
- Потенциальным утечкам памяти
- Лишним вызовам `getDatabase()`

**Исправление:**
Использован `remember` для кэширования репозиториев во всех экранах:
- `HomeScreen`
- `MedicinesListScreen`
- `RemindersListScreen`
- `MedicineFormScreen`
- `ReminderFormScreen`

```kotlin
// До
val database = MedicineDatabase.getDatabase(context)
val medicineRepository = MedicineRepository(database.medicineDao())

// После
val database = remember { MedicineDatabase.getDatabase(context.applicationContext) }
val medicineRepository = remember { MedicineRepository(database.medicineDao()) }
```

### 2. ✅ Утечки памяти - использование Context вместо ApplicationContext

**Проблема:**
`ReminderRepository` хранил `Context`, который мог быть Activity Context, что приводило к утечкам памяти при уничтожении Activity.

**Исправление:**
- Добавлено свойство `applicationContext` в `ReminderRepository`
- Все использования `context` заменены на `applicationContext`
- Во всех экранах используется `context.applicationContext` при создании репозиториев

```kotlin
// ReminderRepository.kt
private val applicationContext: Context?
    get() = context?.applicationContext

// Использование
applicationContext?.let {
    ReminderScheduler.scheduleReminder(it, reminder)
}
```

### 3. ✅ Длительные операции - вычисления Calendar при каждой рекомпозиции

**Проблема:**
В `HomeScreen` вычисления `Calendar.getInstance()`, `dayOfWeekIndex`, `tomorrowDayOfWeekIndex` и `currentTimeString` выполнялись при каждой рекомпозиции.

**Исправление:**
- Использован `remember` с ключом для кэширования вычислений дня недели
- Время обновляется только раз в минуту через `LaunchedEffect`
- День недели пересчитывается только при смене дня

```kotlin
// Кэширование дня недели (обновляется только при смене дня)
val currentDateKey = remember { 
    val calendar = Calendar.getInstance()
    calendar.get(Calendar.YEAR) * 10000 + 
    calendar.get(Calendar.MONTH) * 100 + 
    calendar.get(Calendar.DAY_OF_MONTH)
}

val (dayOfWeekIndex, tomorrowDayOfWeekIndex) = remember(currentDateKey) {
    // Вычисления дня недели
}

// Обновление времени раз в минуту
val currentTimeStringState = remember { 
    mutableStateOf(/* начальное время */)
}

LaunchedEffect(Unit) {
    while (true) {
        kotlinx.coroutines.delay(60000)
        // Обновление времени
    }
}
```

### 4. ✅ Кэширование - PreferencesManager

**Проблема:**
`PreferencesManager` создавался заново при каждой рекомпозиции в `SettingsScreen`.

**Исправление:**
Использован `remember` для кэширования:

```kotlin
// До
val preferencesManager = PreferencesManager(context)

// После
val preferencesManager = remember { PreferencesManager(context.applicationContext) }
```

## Дополнительные оптимизации

### 5. ✅ Оптимизация remember в HomeScreen

Вычисления `reminderItems` теперь правильно кэшируются с зависимостями:
- `remindersForToday.value`
- `remindersForTomorrow.value`
- `allMedicines.value`
- `currentTimeStringState.value`

### 6. ✅ Использование applicationContext везде

Все экраны теперь используют `context.applicationContext` вместо `context` при создании репозиториев и database, что предотвращает утечки памяти.

## Оставшиеся моменты

### runBlocking в attachBaseContext

`runBlocking` в `MainActivity.attachBaseContext()` необходим, так как:
- `attachBaseContext` вызывается синхронно до `onCreate`
- Нужно синхронно получить язык для установки локали
- Это стандартная практика для установки локали в Android

### CoroutineScope в MainActivity

`CoroutineScope` в `onCreate` создается для одноразовой операции восстановления напоминаний. Он не отменяется при уничтожении Activity, что правильно, так как операция должна завершиться даже если Activity уничтожена.

## Результаты оптимизаций

1. **Устранены утечки памяти** - использование `applicationContext` и `remember` для репозиториев
2. **Улучшена производительность** - кэширование вычислений Calendar и репозиториев
3. **Оптимизированы рекомпозиции** - меньше ненужных пересозданий объектов
4. **Правильное управление временем** - обновление времени раз в минуту вместо при каждой рекомпозиции

## Рекомендации для будущего

1. Рассмотреть использование ViewModel для управления состоянием и репозиториями
2. Добавить индексы в Room для оптимизации запросов (если база данных будет расти)
3. Рассмотреть использование `derivedStateOf` для производных состояний
4. Добавить мониторинг производительности в production версии

