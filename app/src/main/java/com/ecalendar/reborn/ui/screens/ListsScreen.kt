package com.ecalendar.reborn.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecalendar.reborn.core.lunar.LunarCalendar
import com.ecalendar.reborn.data.BirthdayEntity
import com.ecalendar.reborn.data.EventEntity
import com.ecalendar.reborn.data.NoteEntity
import com.ecalendar.reborn.ui.AppActions
import com.ecalendar.reborn.ui.components.EmptyHint
import com.ecalendar.reborn.ui.components.SectionCard
import com.ecalendar.reborn.ui.components.SectionTitle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 清单页：日程 / 待办 / 记事 / 生日。
 * 对应原包 `tools.notebook`（记事）、`tools.task`（待办）、`tools.notice`（生日）、
 * `module.ugc.ui.BirthDayActivity`、`remind.AlarmRemindActivity`。
 */
@Composable
fun ListsScreen(actions: AppActions) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("日程", "待办", "记事", "生日")
    Column(Modifier.fillMaxWidth()) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        when (tab) {
            0 -> ScheduleTab(actions)
            1 -> TodoTab(actions)
            2 -> NoteTab(actions)
            else -> BirthdayTab(actions)
        }
    }
}

@Composable
private fun ScheduleTab(actions: AppActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val events by actions.db.eventDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList<EventEntity>())
    var showAdd by remember { mutableStateOf(false) }

    val schedules = events.filter { it.kind == "schedule" }.sortedBy { it.startAt }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("共 ${schedules.size} 条日程")
                Button(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("新建")
                }
            }
        }
        if (schedules.isEmpty()) item { EmptyHint("还没有日程。新建一条，到点会响铃提醒。") }
        items(schedules) { event ->
            SectionCard(title = event.title) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatWhen(event), style = MaterialTheme.typography.bodyMedium)
                    Row {
                        if (event.repeatRule != "none") AssistChip(onClick = {}, label = { Text(repeatLabel(event.repeatRule)) })
                        IconButton(onClick = {
                            actions.scheduler.cancel(event.id)
                            scope.launch { actions.db.eventDao().delete(event.id) }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                    }
                }
                if (event.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(event.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (event.remindBefore >= 0) "提前 ${event.remindBefore} 分钟提醒" else "不提醒",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showAdd) {
        EventEditorDialog(
            defaultRemind = 10,
            onDismiss = { showAdd = false },
            onConfirm = { title, note, millis, remind ->
                scope.launch {
                    val id = actions.db.eventDao().upsert(
                        EventEntity(title = title, note = note, startAt = millis, remindBefore = remind, kind = "schedule"),
                    )
                    actions.db.eventDao().find(id)?.let { actions.scheduler.schedule(it) }
                }
                showAdd = false
            },
        )
    }
    // 引用 context 以保持平台对话框可用性
    context
}

@Composable
private fun TodoTab(actions: AppActions) {
    val scope = rememberCoroutineScope()
    val todos by actions.db.eventDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList<EventEntity>())
    var showAdd by remember { mutableStateOf(false) }
    val list = todos.filter { it.kind == "todo" }.sortedWith(compareBy({ it.done }, { it.startAt }))

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("待办 ${list.count { !it.done }} 项未完成")
                Button(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("添加")
                }
            }
        }
        if (list.isEmpty()) item { EmptyHint("待办清单是空的。加一件小事开始。") }
        items(list) { todo ->
            SectionCard(title = todo.title) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = todo.done, onCheckedChange = { checked ->
                        scope.launch {
                            actions.db.eventDao().setDone(todo.id, checked)
                            if (checked) actions.scheduler.cancel(todo.id)
                        }
                    })
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (todo.done) "已完成" else "截止 " + formatWhen(todo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (todo.note.isNotBlank()) Text(todo.note, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = {
                        actions.scheduler.cancel(todo.id)
                        scope.launch { actions.db.eventDao().delete(todo.id) }
                    }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showAdd) {
        EventEditorDialog(
            defaultRemind = -1,
            onDismiss = { showAdd = false },
            onConfirm = { title, note, millis, remind ->
                scope.launch {
                    actions.db.eventDao().upsert(
                        EventEntity(title = title, note = note, startAt = millis, remindBefore = remind, kind = "todo"),
                    )
                }
                showAdd = false
            },
        )
    }
}

@Composable
private fun NoteTab(actions: AppActions) {
    val scope = rememberCoroutineScope()
    val notes by actions.db.noteDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList<NoteEntity>())
    var editing by remember { mutableStateOf<NoteEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("记事本 · ${notes.size} 条")
                Button(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("写一条")
                }
            }
        }
        if (notes.isEmpty()) item { EmptyHint("记点什么吧：随手灵感、账单、待查资料都行。") }
        items(notes) { note ->
            SectionCard(title = note.title) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        note.content.take(80).replace("\n", " "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Row {
                        TextButton(onClick = { editing = note }) { Text("编辑") }
                        IconButton(onClick = { scope.launch { actions.db.noteDao().delete(note.id) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                }
                Text(
                    java.time.Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (creating || editing != null) {
        val target = editing
        NoteEditorDialog(
            initial = target,
            onDismiss = { creating = false; editing = null },
            onConfirm = { title, content ->
                scope.launch {
                    actions.db.noteDao().upsert(
                        (target ?: NoteEntity(title = title, content = content)).copy(
                            title = title,
                            content = content,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun BirthdayTab(actions: AppActions) {
    val scope = rememberCoroutineScope()
    val birthdays by actions.db.birthdayDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList<BirthdayEntity>())
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("生日与纪念日")
                Button(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("添加")
                }
            }
        }
        if (birthdays.isEmpty()) item { EmptyHint("添加亲友生日，支持农历生日，会提前提醒。") }
        items(birthdays) { birthday ->
            val next = nextOccurrence(birthday)
            val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), next).toInt()
            SectionCard(title = "${birthday.name} · ${birthday.relation}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            "${birthday.month}月${birthday.day}日" + if (birthday.isLunar) "（农历）" else "（公历）",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            if (daysLeft == 0) "就是今天" else "还有 $daysLeft 天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { scope.launch { actions.db.birthdayDao().delete(birthday.id) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showAdd) {
        BirthdayEditorDialog(
            onDismiss = { showAdd = false },
            onConfirm = { entity ->
                scope.launch { actions.db.birthdayDao().upsert(entity) }
                showAdd = false
            },
        )
    }
}

private fun formatWhen(event: EventEntity): String {
    val dt = Instant.ofEpochMilli(event.startAt).atZone(ZoneId.systemDefault())
    return "%d月%d日 %02d:%02d".format(dt.monthValue, dt.dayOfMonth, dt.hour, dt.minute)
}

private fun repeatLabel(rule: String): String = when (rule) {
    "daily" -> "每天"
    "weekly" -> "每周"
    "monthly" -> "每月"
    "yearly" -> "每年"
    "lunarYearly" -> "每年农历"
    else -> "单次"
}

private fun nextOccurrence(birthday: BirthdayEntity): LocalDate {
    val today = LocalDate.now()
    fun candidate(year: Int): LocalDate = if (birthday.isLunar) {
        LunarCalendar.lunarToSolar(year, birthday.month, birthday.day) ?: LocalDate.of(year, birthday.month, birthday.day)
    } else {
        runCatching { LocalDate.of(year, birthday.month, birthday.day) }.getOrElse { LocalDate.of(year, birthday.month, 28) }
    }
    var year = today.year
    var date = candidate(year)
    if (date.isBefore(today)) {
        year += 1
        date = candidate(year)
    }
    return date
}

@Composable
private fun EventEditorDialog(
    defaultRemind: Int,
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String, millis: Long, remind: Int) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now()) }
    var remind by remember { mutableStateOf(defaultRemind) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") })
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year, date.monthValue - 1, date.dayOfMonth,
                        ).show()
                    }) { Text("日期 ${date.monthValue}月${date.dayOfMonth}日") }
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, h, mi -> time = LocalTime.of(h, mi) }, time.hour, time.minute, true).show()
                    }) { Text("时间 %02d:%02d".format(time.hour, time.minute)) }
                }
                Spacer(Modifier.height(10.dp))
                Text("提醒", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-1 to "不提醒", 0 to "准点", 10 to "提前10分", 30 to "提前30分").forEach { (value, label) ->
                        FilterChip(selected = remind == value, onClick = { remind = value }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val millis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(title.trim(), note.trim(), millis, remind)
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NoteEditorDialog(
    initial: NoteEntity?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建记事" else "编辑记事") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    modifier = Modifier.height(160.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onConfirm(title.trim(), content) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun BirthdayEditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (BirthdayEntity) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("家人") }
    var month by remember { mutableStateOf(LocalDate.now().monthValue) }
    var day by remember { mutableStateOf(LocalDate.now().dayOfMonth) }
    var lunar by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加生日") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("家人", "朋友", "同事", "客户").forEach { r ->
                        FilterChip(selected = relation == r, onClick = { relation = r }, label = { Text(r) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !lunar, onClick = { lunar = false }, label = { Text("公历") })
                    FilterChip(selected = lunar, onClick = { lunar = true }, label = { Text("农历") })
                }
                Spacer(Modifier.height(8.dp))
                Text("月 / 日：$month 月 $day 日", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = month.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 1..12) month = it } },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = day.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 1..31) day = it } },
                        label = { Text("日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(BirthdayEntity(name = name.trim(), relation = relation, month = month, day = day, isLunar = lunar, note = note.trim()))
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
