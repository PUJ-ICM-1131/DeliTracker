package com.delitracker.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.components.DtCaption
import com.delitracker.app.components.DtChipRow
import com.delitracker.app.components.DtPillRow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.delitracker.app.R
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtAccent

// Historia #1 · Registro de nuevo usuario
@Composable
fun RegistroScreen(vm: AppViewModel, onCrearCuenta: () -> Unit, onIrALogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        BrandLogo()
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.crear_cuenta), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.unete_a_delitracker), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it; error = null },
            label = { Text(stringResource(R.string.nombre_completo)) },
            placeholder = { Text(stringResource(R.string.ej_carol_robayo)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text(stringResource(R.string.correo)) },
            placeholder = { Text(stringResource(R.string.nombre_correo_com)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.contrase_a)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(error!!), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !vm.authBusy,
            onClick = {
                vm.register(name = name, email = email, password = password) { success, message ->
                    if (success) {
                        onCrearCuenta()
                    } else {
                        error = message
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(stringResource(R.string.crear_cuenta))
        }
        Spacer(Modifier.height(10.dp))
        TextButton(enabled = !vm.authBusy, onClick = onIrALogin) {
            Text(stringResource(R.string.ya_tienes_cuenta_inicia_sesi_n))
        }
    }
}

// Historia #2 · Inicio de sesión
@Composable
fun LoginScreen(vm: AppViewModel, onIniciarSesion: () -> Unit, onIrARegistro: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        BrandLogo()
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.inicia_sesi_n), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.bienvenido_de_nuevo), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text(stringResource(R.string.correo)) },
            placeholder = { Text(stringResource(R.string.nombre_correo_com)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.contrase_a)) },
            visualTransformation = PasswordVisualTransformation(),
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(error!!), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !vm.authBusy,
            onClick = {
                vm.login(email = email, password = password) { success, message ->
                    if (success) {
                        onIniciarSesion()
                    } else {
                        error = message
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(stringResource(R.string.iniciar_sesi_n))
        }
        Spacer(Modifier.height(6.dp))
        TextButton(enabled = !vm.authBusy, onClick = { vm.resetPassword(email) }) {
            Text(stringResource(R.string.olvidaste_tu_contrase_a))
        }
        vm.authMessage?.let {
            Text(stringResource(it), color = if (it == R.string.auth_password_reset_sent)
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        if (vm.authBusy) CircularProgressIndicator()
        if (com.delitracker.app.BuildConfig.FIREBASE_AUTH_EMULATOR) {
            Text("Firebase local: cuentas de prueba", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))
        TextButton(enabled = !vm.authBusy, onClick = onIrARegistro) { Text(stringResource(R.string.crear_una_cuenta_nueva)) }
    }
}

// Historia #14 · Onboarding de preferencias iniciales
@Composable
fun OnboardingScreen(vm: AppViewModel, onContinuar: () -> Unit, onOmitir: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(stringResource(R.string.cu_ntanos_qu_te_gusta), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.puedes_omitir_esto_y_completarlo_despu_s),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DtCaption(stringResource(R.string.tipo_de_cocina))
        DtChipRow(
            options = listOf(stringResource(R.string.italiana),
                stringResource(R.string.mexicana),
                stringResource(R.string.vegana), stringResource(R.string.sushi),
                stringResource(R.string.sin_gluten)
            ),
            isSelected = { vm.cuisine[it] == true },
            onToggle = { vm.toggleCuisine(it) }
        )
        DtCaption(stringResource(R.string.rango_de_precio))
        DtPillRow(
            options = listOf(stringResource(R.string.precio_bajo),
                stringResource(R.string.precio_medio), stringResource(R.string.precio_alto)
            ),
            selected = vm.price,
            onSelect = { vm.price = it }
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onContinuar, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.continuar))
        }
        TextButton(onClick = onOmitir, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.omitir_por_ahora))
        }
    }
}

@Composable
fun BrandLogo() {
    val deli = stringResource(R.string.brand_deli)
    val tracker = stringResource(R.string.brand_tracker)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DtAccent, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(6.dp))
        Text(buildAnnotatedBrand(deli, tracker))
    }
}

private fun buildAnnotatedBrand(deli: String, tracker: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(deli) }
    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = DtAccent)) { append(tracker) }
}
