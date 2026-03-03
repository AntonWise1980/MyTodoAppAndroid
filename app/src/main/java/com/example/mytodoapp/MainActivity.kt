package com.example.mytodoapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

// 1. Veri Modeli
data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val name: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TodoAppScreen()
                }
            }
        }
    }
}

// --- KAYIT VE YÜKLEME FONKSİYONLARI ---
fun saveItems(context: Context, items: List<TodoItem>) {
    val sharedPreferences = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = gson.toJson(items)
    sharedPreferences.edit().putString("item_list", json).apply()
}

fun loadItems(context: Context): List<TodoItem> {
    val sharedPreferences = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("item_list", null) ?: return emptyList()
    val type = object : TypeToken<List<TodoItem>>() {}.type
    return Gson().fromJson(json, type)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoAppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State Yönetimi
    var itemList by remember { mutableStateOf(loadItems(context)) }
    var inputText by remember { mutableStateOf("") }
    var filterText by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Otomatik Kayıt
    LaunchedEffect(itemList) {
        saveItems(context, itemList)
    }

    val filteredList = itemList.filter { it.name.contains(filterText, ignoreCase = true) }

    // Snackbar Mesaj Fonksiyonu
    val showMsg: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hepsini Sil?") },
            text = { Text("Tüm listeyi temizlemek istediğinizden emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemList = emptyList()
                        showDeleteDialog = false
                        inputText = ""
                        isEditMode = false
                        showMsg("Tüm liste temizlendi")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
                ) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("To Do App", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(20.dp))

            // Input Alanı
            OutlinedTextField(
                value = inputText,
                onValueChange = { if (it.length <= 15) inputText = it },
                label = { Text(if (isEditMode) "Öğeyi Düzenle" else "Yeni Öğe Ekle") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = ""; isEditMode = false }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                supportingText = {
                    Text("${inputText.length} / 15", color = if (inputText.length >= 15) Color.Red else Color.Gray)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ekle/Güncelle Butonu
            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        showMsg("Lütfen bir öğe adı girin!")
                        return@Button
                    }
                    if (isEditMode) {
                        itemList = itemList.map { if (it.id == editingItemId) it.copy(name = inputText) else it }
                        isEditMode = false
                        editingItemId = null
                        showMsg("Öğe güncellendi")
                    } else {
                        if (itemList.size >= 10) {
                            showMsg("Maksimum 10 öğe ekleyebilirsiniz!")
                            return@Button
                        }
                        if (itemList.any { it.name.equals(inputText, ignoreCase = true) }) {
                            showMsg("Bu öğe zaten mevcut!")
                            return@Button
                        }
                        itemList = itemList + TodoItem(name = inputText)
                        showMsg("Öğe eklendi")
                    }
                    inputText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isEditMode) Color(0xFF2E7D32) else Color(0xFF333333))
            ) {
                Icon(if (isEditMode) Icons.Default.Edit else Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditMode) "UPDATE ITEM" else "ADD ITEM")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Liste ve Filtreleme
            if (itemList.isEmpty()) {
                // BOŞ LİSTE GÖRÜNÜMÜ
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Henüz bir şey eklemediniz.\nListeniz boş görünüyor.", textAlign = TextAlign.Center, color = Color.Gray)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Filtrele...") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                        modifier = Modifier.height(52.dp)
                    ) { Text("CLEAR") }
                }

                Text("${itemList.size}/10 Item", modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp), fontSize = 12.sp, color = Color.Gray)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredList, key = { it.id }) { item ->
                        // KAYDIRARAK SİLME (Swipe-to-Dismiss)
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    itemList = itemList.filter { i -> i.id != item.id }
                                    showMsg("${item.name} silindi")
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                                Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        isEditMode = true
                                        editingItemId = item.id
                                        inputText = item.name
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (editingItemId == item.id) Color(0xFFE8F5E9) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}