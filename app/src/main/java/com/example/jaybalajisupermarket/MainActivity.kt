package com.example.jaybalajisupermarket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Context.observeConnectivityAsFlow(): Flow<Boolean> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network) { trySend(false) }
        override fun onUnavailable() { trySend(false) }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(request, callback)

    val currentState = connectivityManager.activeNetwork?.let { network ->
        connectivityManager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } ?: false
    trySend(currentState)

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}

@Composable
fun NetworkAwareApp(context: Context, content: @Composable () -> Unit) {
    val isConnected by context.observeConnectivityAsFlow().collectAsState(initial = true)

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFFFFB703)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "NO INTERNET CONNECTION",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Please check your Wi-Fi or mobile data and try again.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NetworkAwareApp(context = this) {
                    AuthNavigator()
                }
            }
        }
    }
}

@Composable
fun AuthNavigator() {
    val auth = FirebaseAuth.getInstance()
    var currentScreen by remember { mutableStateOf("Splash") }

    when (currentScreen) {
        "Splash" -> SplashScreen(
            onTimeout = {
                val user = auth.currentUser
                if (user != null) {
                    currentScreen = if (user.email == "admin@jaybalaji.com") "AdminDashboard" else "CustomerCatalog"
                } else {
                    currentScreen = "Login"
                }
            }
        )
        "Login" -> UnifiedLoginScreen(
            onNavigateToSignUp = { currentScreen = "CustomerSignUp" },
            onCustomerLoginSuccess = { currentScreen = "CustomerCatalog" },
            onAdminLoginSuccess = { currentScreen = "AdminDashboard" }
        )
        "CustomerSignUp" -> CustomerSignUpScreen(
            onNavigateToLogin = { currentScreen = "Login" }
        )
        "AdminDashboard" -> AdminDashboardScreen(
            onLogout = {
                auth.signOut()
                currentScreen = "Login"
            }
        )
        "CustomerCatalog" -> CustomerCatalogScreen(
            onLogout = {
                auth.signOut()
                currentScreen = "Login"
            }
        )
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val navy = Color(0xFF0D1B2A)
    val gold = Color(0xFFE0E1DD)

    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(navy),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "JAY BALA JI",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SUPER MARKET",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = gold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = gold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun UnifiedLoginScreen(
    onNavigateToSignUp: () -> Unit,
    onCustomerLoginSuccess: () -> Unit,
    onAdminLoginSuccess: () -> Unit
) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val brandColor = Color(0xFF0D1B2A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE0E1DD))))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JAY BALA JI",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = brandColor,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "SUPER MARKET",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = brandColor,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = loginId,
            onValueChange = { loginId = it },
            label = { Text("Email or Admin ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (loginId.isNotEmpty() && password.isNotEmpty()) {
                    val actualEmail = if (loginId == "JAYBALAJI") "admin@jaybalaji.com" else loginId

                    auth.signInWithEmailAndPassword(actualEmail, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (actualEmail == "admin@jaybalaji.com") {
                                onAdminLoginSuccess()
                            } else {
                                onCustomerLoginSuccess()
                            }
                        } else {
                            message = "Login Failed: Check Credentials"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = brandColor)
        ) {
            Text("LOGIN TO STORE", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text("NEW CUSTOMER? REGISTER HERE", color = brandColor, fontWeight = FontWeight.Bold)
        }
        Text(message, color = Color.Red)
    }
}

@Composable
fun CustomerSignUpScreen(onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CREATE ACCOUNT", fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if(email.isNotEmpty() && password.length >= 6) {
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            message = "Success! Please click Back to Login"
                        } else {
                            message = task.exception?.localizedMessage ?: "Registration Failed"
                        }
                    }
                } else {
                    message = "Password must be at least 6 characters"
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B2A))
        ) {
            Text("REGISTER", fontWeight = FontWeight.Bold, color = Color.White)
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("BACK TO LOGIN", color = Color(0xFF0D1B2A))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = if (message.contains("Success")) Color(0xFF4CAF50) else Color.Red,
            textAlign = TextAlign.Center
        )
    }
}