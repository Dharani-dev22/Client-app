package com.example.jaybalajisupermarket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

data class GroceryItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val inStock: Boolean = true,
    val imageUrl: String = ""
)

data class CartItem(val product: GroceryItem, val quantity: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCatalogScreen(onLogout: () -> Unit) {
    var list by remember { mutableStateOf(listOf<GroceryItem>()) }
    var cart by remember { mutableStateOf(listOf<CartItem>()) }
    var screen by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var phone by remember { mutableStateOf("") }
    var addr by remember { mutableStateOf("") }
    var wish by remember { mutableStateOf(setOf<String>()) }
    var showSuccess by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val email = auth.currentUser?.email ?: ""
    val navy = Color(0xFF0D1B2A)
    val categories = listOf("All", "Staples", "Snacks", "Beverages", "Dairy", "Others")

    LaunchedEffect(Unit) {
        db.collection("Products").addSnapshotListener { snp, _ ->
            list = snp?.documents?.mapNotNull { it.toObject(GroceryItem::class.java)?.copy(id = it.id) } ?: emptyList()
        }
        if (uid.isNotEmpty()) {
            db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
                phone = doc.getString("phone") ?: ""
                addr = doc.getString("address") ?: ""
                wish = (doc.get("wishlist") as? List<String>)?.toSet() ?: emptySet()
            }
        }
    }

    Scaffold(
        topBar = {
            if (!showSuccess && screen != "PaymentProcessing" && screen != "RazorpayAuth" && screen != "UpiPin") {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(navy, Color(0xFF1B263B)))).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("JAY BALA JI", fontWeight = FontWeight.Black, letterSpacing = 6.sp, fontSize = 28.sp, color = Color.White)
                        Text("SUPER MARKET", fontWeight = FontWeight.Light, letterSpacing = 2.sp, fontSize = 12.sp, color = Color(0xFFE0E1DD))
                    }
                }
            }
        },
        bottomBar = {
            if (!showSuccess && (screen == "Home" || screen == "Wishlist" || screen == "Orders" || screen == "Profile")) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = screen == "Home", onClick = { screen = "Home" }, icon = { Icon(if (screen == "Home") Icons.Filled.Home else Icons.Outlined.Home, null) }, label = { Text("Shop") })
                    NavigationBarItem(selected = screen == "Wishlist", onClick = { screen = "Wishlist" }, icon = { Icon(if (screen == "Wishlist") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null) }, label = { Text("Wishlist") })
                    NavigationBarItem(selected = screen == "Orders", onClick = { screen = "Orders" }, icon = { Icon(if (screen == "Orders") Icons.Filled.List else Icons.Outlined.List, null) }, label = { Text("Orders") })
                    NavigationBarItem(selected = screen == "Profile", onClick = { screen = "Profile" }, icon = { Icon(if (screen == "Profile") Icons.Filled.Person else Icons.Outlined.Person, null) }, label = { Text("Account") })
                }
            }
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p).background(Color(0xFFF1F2F6))) {
            when {
                showSuccess -> SuccessUI(navy) { showSuccess = false; screen = "Home" }
                screen == "Orders" -> OrderHistoryUI(email, navy) { screen = "Home" }
                screen == "Cart" -> CartUI(cart, navy, { cart = it }, { screen = "ConfirmOrder" }, { screen = "Home" })
                screen == "ConfirmOrder" -> ConfirmOrderUI(cart, phone, addr, navy, { ph, ad -> phone = ph; addr = ad }, { screen = "PaymentSelection" }, { screen = "Cart" })
                screen == "PaymentSelection" -> PaymentSelectionUI(cart, phone, addr, navy, { screen = "RazorpayAuth" }, { screen = "ProcessingCOD" }, { screen = "ConfirmOrder" })
                screen == "RazorpayAuth" -> RazorpayAuthUI(navy, { screen = "UpiPin" }, { screen = "PaymentSelection" })
                screen == "UpiPin" -> UpiPinUI(navy, { screen = "PaymentProcessing" }, { screen = "RazorpayAuth" })
                screen == "ProcessingCOD" -> PaymentProcessingUI(navy, "Placing Order...") { showSuccess = true; screen = "Home" }
                screen == "PaymentProcessing" -> PaymentProcessingUI(navy, "Verifying Payment...") { showSuccess = true; screen = "Home" }
                screen == "Profile" -> ProfileUI(email, phone, addr, navy, { ph, ad -> phone = ph; addr = ad }, {
                    db.collection("Users").document(uid).set(mapOf("phone" to phone, "address" to addr, "wishlist" to wish.toList()))
                }, onLogout)
                else -> {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Search products...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat -> FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) }, shape = RoundedCornerShape(20.dp)) }
                        }
                    }
                    val filtered = list.filter { (selectedCategory == "All" || it.category == selectedCategory) && it.name.contains(query, true) && (screen != "Wishlist" || wish.contains(it.id)) }
                    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                        items(filtered) { item ->
                            ProductGridItem(item, wish.contains(item.id), navy, {
                                val n = if (wish.contains(item.id)) wish - item.id else wish + item.id
                                wish = n
                                db.collection("Users").document(auth.currentUser?.uid ?: "").set(mapOf("wishlist" to n.toList()), com.google.firebase.firestore.SetOptions.merge())
                            }) {
                                if (item.inStock) {
                                    val ex = cart.find { it.product.id == item.id }
                                    cart = if (ex != null) cart.map { if (it.product.id == item.id) it.copy(quantity = it.quantity + 1) else it } else cart + CartItem(item, 1)
                                }
                            }
                        }
                    }
                    if (cart.isNotEmpty()) Button(onClick = { screen = "Cart" }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(60.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = navy)) { Text("VIEW CART (₹${cart.sumOf { it.product.price * it.quantity }})", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(item: GroceryItem, isWish: Boolean, theme: Color, onWish: () -> Unit, onAdd: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Box {
                AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Crop, alpha = if(item.inStock) 1f else 0.5f)
                if (!item.inStock) {
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Text("SOLD OUT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
                IconButton(onClick = onWish, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.White.copy(0.8f), CircleShape).size(30.dp)) {
                    Icon(if (isWish) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (isWish) Color.Red else Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${item.price.toInt()}", fontWeight = FontWeight.Black, color = if(item.inStock) theme else Color.Gray, fontSize = 18.sp)
                    IconButton(onClick = onAdd, enabled = item.inStock, modifier = Modifier.background(if(item.inStock) theme else Color.LightGray, CircleShape).size(32.dp)) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun CartUI(cart: List<CartItem>, theme: Color, onCart: (List<CartItem>) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("YOUR BAG", fontWeight = FontWeight.Black, fontSize = 24.sp, color = theme)
        LazyColumn(Modifier.weight(1f).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cart) { item ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(item.product.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if(item.quantity > 1) onCart(cart.map { if(it.product.id == item.product.id) it.copy(quantity = it.quantity - 1) else it }) else onCart(cart.filter { it.product.id != item.product.id }) }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                            Text("${item.quantity}", fontWeight = FontWeight.Black)
                            IconButton(onClick = { onCart(cart.map { if(it.product.id == item.product.id) it.copy(quantity = it.quantity + 1) else it }) }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                }
            }
        }
        Text("Subtotal: ₹${cart.sumOf { it.product.price * it.quantity }}", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = onNext, Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = theme)) { Text("NEXT", fontWeight = FontWeight.Black) }
        TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text("Add More Items") }
    }
}

@Composable
fun ConfirmOrderUI(cart: List<CartItem>, ph: String, ad: String, theme: Color, onInfo: (String, String) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    var errorMessage by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("DELIVERY DETAILS", fontWeight = FontWeight.Black, fontSize = 24.sp, color = theme)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = ph,
                    onValueChange = { onInfo(it, ad); errorMessage = "" },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = ad,
                    onValueChange = { onInfo(ph, it); errorMessage = "" },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )
        }

        Button(
            onClick = {
                if (ph.length < 10) {
                    errorMessage = "Please enter a valid phone number (min 10 digits)."
                } else if (ad.trim().isEmpty()) {
                    errorMessage = "Delivery address cannot be empty."
                } else {
                    errorMessage = ""
                    onNext()
                }
            },
            Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme)
        ) {
            Text("PROCEED TO PAYMENT", fontWeight = FontWeight.Black)
        }
        TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text("Edit Cart") }
    }
}

@Composable
fun PaymentSelectionUI(cart: List<CartItem>, ph: String, ad: String, theme: Color, onOnline: () -> Unit, onCOD: () -> Unit, onBack: () -> Unit) {
    var selectedMethod by remember { mutableStateOf("Online") }
    val db = FirebaseFirestore.getInstance()
    val userEmail = FirebaseAuth.getInstance().currentUser?.email

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("SELECT PAYMENT", fontWeight = FontWeight.Black, fontSize = 24.sp, color = theme)
        Spacer(Modifier.height(32.dp))
        Card(Modifier.fillMaxWidth().clickable { selectedMethod = "Online" }, border = if(selectedMethod == "Online") BorderStroke(2.dp, theme) else null) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedMethod == "Online", onClick = { selectedMethod = "Online" })
                Text("Online Payment (UPI/Cards)", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth().clickable { selectedMethod = "COD" }, border = if(selectedMethod == "COD") BorderStroke(2.dp, theme) else null) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedMethod == "COD", onClick = { selectedMethod = "COD" })
                Text("Cash on Delivery", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                val data = mapOf("items" to cart.map { "${it.product.name} x${it.quantity}" }, "total" to cart.sumOf { it.product.price * it.quantity }, "address" to ad, "phone" to ph, "status" to "Pending", "paymentMethod" to selectedMethod, "timestamp" to System.currentTimeMillis(), "userEmail" to userEmail)
                db.collection("Orders").add(data).addOnSuccessListener { if (selectedMethod == "Online") onOnline() else onCOD() }
            },
            Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = theme)
        ) { Text("CONTINUE", fontWeight = FontWeight.Black) }
        TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}

@Composable
fun RazorpayAuthUI(theme: Color, onNext: () -> Unit, onCancel: () -> Unit) {
    var pass by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, null, Modifier.size(64.dp), Color(0xFF3399FF))
        Text("Razorpay Secure", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF3399FF))
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Razorpay Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = { if(pass.isNotEmpty()) onNext() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3399FF))) { Text("PROCEED", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onCancel) { Text("Cancel Payment", color = Color.Red) }
    }
}

@Composable
fun UpiPinUI(theme: Color, onComplete: () -> Unit, onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFF1F2F6)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("ENTER UPI PIN", fontWeight = FontWeight.Black)
            Icon(Icons.Default.Info, null, tint = Color.Gray)
        }
        Spacer(Modifier.height(48.dp))
        Text("Verifying Transaction...", fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Text(pin.replace(Regex("."), "*"), fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
        Spacer(Modifier.weight(1f))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "X", "0", "OK")
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(keys) { key ->
                Card(modifier = Modifier.height(60.dp).clickable {
                    when(key) {
                        "OK" -> if(pin.length >= 4) onComplete()
                        "X" -> if(pin.isNotEmpty()) pin = pin.dropLast(1)
                        else -> if(pin.length < 6) pin += key
                    }
                }, colors = CardDefaults.cardColors(containerColor = if(key == "OK") Color(0xFF2D6A4F) else Color.White)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(key, fontWeight = FontWeight.Black, color = if(key == "OK") Color.White else Color.Black) }
                }
            }
        }
    }
}

@Composable
fun PaymentProcessingUI(theme: Color, msg: String, onComplete: () -> Unit) {
    LaunchedEffect(Unit) { delay(3000); onComplete() }
    Column(Modifier.fillMaxSize().background(Color.White), Arrangement.Center, Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = theme)
        Spacer(Modifier.height(24.dp))
        Text(msg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SuccessUI(color: Color, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(120.dp), Color(0xFF2D6A4F))
        Text("ORDER CONFIRMED", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(30.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = color)) { Text("CONTINUE SHOPPING") }
    }
}

@Composable
fun ProfileUI(em: String, ph: String, ad: String, theme: Color, onInfo: (String, String) -> Unit, onSave: () -> Unit, onLog: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp).background(theme.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(50.dp), theme) }
        Text(em, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = ph, onValueChange = { onInfo(it, ad) }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = ad, onValueChange = { onInfo(ph, it) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)
        Button(onSave, Modifier.fillMaxWidth().padding(top = 20.dp), colors = ButtonDefaults.buttonColors(containerColor = theme)) { Text("SAVE PROFILE") }
        Spacer(Modifier.weight(1f))
        Button(onLog, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("LOGOUT") }
    }
}

@Composable
fun OrderHistoryUI(email: String, theme: Color, onBack: () -> Unit) {
    var orders by remember { mutableStateOf(listOf<OrderItem>()) }
    val db = FirebaseFirestore.getInstance()
    LaunchedEffect(Unit) {
        db.collection("Orders").whereEqualTo("userEmail", email).get().addOnSuccessListener { snp ->
            orders = snp.documents.mapNotNull { doc ->
                val items = doc.get("items") as? List<String> ?: emptyList()
                val total = doc.getDouble("total") ?: 0.0
                val address = doc.getString("address") ?: ""
                val phone = doc.getString("phone") ?: ""
                val status = doc.getString("status") ?: ""
                val paymentMethod = doc.getString("paymentMethod") ?: "COD"
                val timestamp = doc.getLong("timestamp") ?: 0L
                OrderItem(doc.id, items, total, address, phone, status, paymentMethod, timestamp, email)
            }.sortedByDescending { it.timestamp }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("ORDER HISTORY", fontWeight = FontWeight.Black, fontSize = 20.sp, color = theme)
        }
        Spacer(Modifier.height(16.dp))
        if (orders.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No orders yet", color = Color.Gray) } }
        else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(orders) { order ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("ORD-${order.id.take(4).uppercase()}", fontWeight = FontWeight.Black)
                                Text(order.status, color = if(order.status == "Pending") Color.Red else Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
                            }
                            Text(order.items.joinToString(", "), fontSize = 13.sp, color = Color.Gray)
                            Text("Total: ₹${order.total}", fontWeight = FontWeight.Black, color = theme)
                        }
                    }
                }
            }
        }
    }
}