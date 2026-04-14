package com.example.jaybalajisupermarket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Note: Assuming GroceryItem data class is defined in another file

data class OrderItem(
    val id: String = "",
    val items: List<String> = emptyList(),
    val total: Double = 0.0,
    val address: String = "",
    val phone: String = "",
    val status: String = "",
    val paymentMethod: String = "",
    val timestamp: Long = 0L,
    val userEmail: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Inventory") }
    val navy = Color(0xFF0D1B2A)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("OWNER CONSOLE", fontWeight = FontWeight.Black, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = navy),
                actions = { IconButton(onClick = onLogout) { Icon(Icons.Filled.ExitToApp, null, tint = Color.White) } }
            )
        }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p).background(Color(0xFFF1F2F6))) {
            TabRow(selectedTabIndex = if (selectedTab == "Inventory") 0 else 1, containerColor = navy, contentColor = Color.White) {
                Tab(selected = selectedTab == "Inventory", onClick = { selectedTab = "Inventory" }, text = { Text("Stock") })
                Tab(selected = selectedTab == "Orders", onClick = { selectedTab = "Orders" }, text = { Text("Orders") })
            }
            if (selectedTab == "Inventory") InventoryUI(navy) else OrdersUI(navy)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryUI(theme: Color) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Staples") }
    var exp by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf(listOf<GroceryItem>()) }
    var errorMessage by remember { mutableStateOf("") } // Added error state
    val db = FirebaseFirestore.getInstance()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        db.collection("Products").addSnapshotListener { snp, _ ->
            list = snp?.documents?.mapNotNull { it.toObject(GroceryItem::class.java)?.copy(id = it.id) } ?: emptyList()
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add New Product", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = theme)

                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(value = name, onValueChange = { name = it; errorMessage = "" }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = price, onValueChange = { price = it; errorMessage = "" }, label = { Text("Price") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                        ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = cat, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp))
                            ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                listOf("Staples", "Snacks", "Beverages", "Dairy", "Others").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { cat = it; exp = false }) }
                            }
                        }
                    }
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Image URL / Base64") }, modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp), shape = RoundedCornerShape(12.dp), maxLines = 5)
                    Button(
                        onClick = {
                            // UPGRADE: Prevent publishing blank products
                            if (name.trim().isNotEmpty() && price.trim().isNotEmpty()) {
                                val item = hashMapOf("name" to name, "price" to (price.toDoubleOrNull() ?: 0.0), "category" to cat, "imageUrl" to url, "inStock" to true)
                                db.collection("Products").add(item).addOnSuccessListener {
                                    name = ""; price = ""; url = ""; errorMessage = ""; focusManager.clearFocus()
                                }
                            } else {
                                errorMessage = "Name and Price cannot be empty!"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme)
                    ) {
                        Text("PUBLISH PRODUCT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        items(list) { item ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        Text(if(item.inStock) "In Stock" else "Out of Stock", color = if(item.inStock) Color(0xFF2D6A4F) else Color.Red, fontSize = 12.sp)
                    }
                    Switch(checked = item.inStock, onCheckedChange = { db.collection("Products").document(item.id).update("inStock", it) })
                    IconButton(onClick = { db.collection("Products").document(item.id).delete() }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFE63946)) }
                }
            }
        }
    }
}

@Composable
fun OrdersUI(theme: Color) {
    var orders by remember { mutableStateOf(listOf<OrderItem>()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("Orders").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener { snp, _ ->
            orders = snp?.documents?.mapNotNull { doc ->
                val items = doc.get("items") as? List<String> ?: emptyList()
                val total = doc.getDouble("total") ?: 0.0
                val address = doc.getString("address") ?: ""
                val phone = doc.getString("phone") ?: ""
                val status = doc.getString("status") ?: ""
                val paymentMethod = doc.getString("paymentMethod") ?: "COD"
                val timestamp = doc.getLong("timestamp") ?: 0L
                val email = doc.getString("userEmail") ?: ""
                OrderItem(doc.id, items, total, address, phone, status, paymentMethod, timestamp, email)
            } ?: emptyList()
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(orders) { order ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (order.status == "Pending") Color(0xFFFFF9C4) else Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ID: ${order.id.take(5).uppercase()}", fontWeight = FontWeight.Black, color = theme)
                        Text(order.paymentMethod, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // UPGRADE: Actually show the order contents!
                    Text("Items: ${order.items.joinToString(", ")}", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                    Spacer(Modifier.height(8.dp))

                    // UPGRADE: Show delivery details so you can fulfill the order!
                    Text("Customer: ${order.userEmail}", fontSize = 13.sp, color = Color.DarkGray)
                    Text("Phone: ${order.phone}", fontSize = 13.sp, color = Color.DarkGray)
                    Text("Address: ${order.address}", fontSize = 13.sp, color = Color.DarkGray)

                    Spacer(Modifier.height(8.dp))

                    Text("Total: ₹${order.total}", fontWeight = FontWeight.Black, fontSize = 18.sp)

                    if (order.status == "Pending") {
                        Button(
                            onClick = { db.collection("Orders").document(order.id).update("status", "Delivered") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
                        ) {
                            Text("MARK AS DELIVERED", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}