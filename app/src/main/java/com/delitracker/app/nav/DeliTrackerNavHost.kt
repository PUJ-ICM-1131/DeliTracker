package com.delitracker.app.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.delitracker.app.components.DtBottomBar
import com.delitracker.app.components.DtTopBar
import com.delitracker.app.screens.*
import com.delitracker.app.state.AppViewModel

/**
 * Equivalente al render() + nav()/goBack()/tab() del prototipo JS:
 * - nav(id)  -> navController.navigate(id)                    (apila, permite volver)
 * - goBack() -> navController.popBackStack()                   (desapila; si no hay nada, cae en Mapa)
 * - tab(id)  -> navController.navigate(id) { popUpTo(inicio, inclusive = true) }
 *               (limpia TODA la pila, incluida la pantalla de inicio de sesión,
 *               para que el botón atrás desde un tab no la revele)
 */
@Composable
fun DeliTrackerApp() {
    val navController = rememberNavController()
    val vm: AppViewModel = viewModel()
    if (!vm.isReady) {
        // Splash mínimo mientras DataStore lee del disco
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.MAPA

    val startDestination = remember {
        if (vm.isLoggedIn) Routes.MAPA else Routes.LOGIN
    }

    fun goTo(route: String) = navController.navigate(route)
    fun goBack() {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.MAPA)
        }
    }

    fun goToTab(route: String) {
        navController.navigate(route) {
            // inclusive = true saca también la pantalla de inicio (login) de la pila,
            // así el botón atrás desde Mapa/Favoritos/Perfil ya no cae en Login.
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }




    Scaffold(
        topBar = {
            DtTopBar(
                title = topBarTitleFor(currentRoute),
                style = topBarStyleFor(currentRoute),
                onBack = { goBack() })
        },
        bottomBar = {
            if (currentRoute in Routes.TAB_ROUTES) DtBottomBar(
                currentRoute = currentRoute,
                onTabSelected = { goToTab(it) })
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Autenticación
            composable(Routes.REGISTRO) {
                RegistroScreen(
                    vm = vm,
                    onCrearCuenta = { goTo(Routes.ONBOARDING) },
                    onIrALogin = { goTo(Routes.LOGIN) })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    vm = vm,
                    onIniciarSesion = { goToTab(Routes.MAPA) },
                    onIrARegistro = { goTo(Routes.REGISTRO) })
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    vm = vm,
                    onContinuar = { goToTab(Routes.MAPA) },
                    onOmitir = { goToTab(Routes.MAPA) })
            }

            // Descubrimiento
            composable(Routes.MAPA) {
                MapaScreen(
                    vm = vm,
                    onBuscar = { goTo(Routes.BUSCAR) },
                    onFiltros = { goTo(Routes.FILTROS) },
                    onAbrirDetalle = { goTo(Routes.DETALLE) }
                )
            }
            composable(Routes.BUSCAR) {
                BuscarScreen(onAbrirDetalle = { goTo(Routes.DETALLE) })
            }
            composable(Routes.FILTROS) {
                FiltrosScreen(vm = vm, onAplicar = { goBack() })
            }

            // Detalle y acciones
            composable(Routes.DETALLE) {
                DetalleScreen(
                    vm = vm,
                    onVerAr = { goTo(Routes.AR) },
                    onResena = { goTo(Routes.RESENA) },
                    onTrazarRuta = { goTo(Routes.RUTA) }
                )
            }
            composable(Routes.RUTA) {
                RutaScreen(vm = vm, onFinalizar = { goBack() })
            }
            composable(Routes.RESENA) {
                ResenaScreen(vm = vm, onPublicar = { goBack() })
            }
            composable(Routes.AR) {
                ArScreen(onCerrar = { goBack() })
            }

            // Favoritos y social
            composable(Routes.FAVORITOS) {
                FavoritosScreen(vm = vm, onAbrirDetalle = { goTo(Routes.DETALLE) })
            }
            composable(Routes.COMPARTIR) {
                CompartirScreen(
                    vm = vm,
                    onInvitar = { goTo(Routes.INVITAR) },
                    onDetener = { goBack() })
            }
            composable(Routes.INVITAR) {
                InvitarScreen(onContinuar = { goBack() })
            }

            // Perfil y cuenta
            composable(Routes.PERFIL) {
                PerfilScreen(
                    vm = vm,
                    onEditarPerfil = { goTo(Routes.EDITAR_PERFIL) },
                    onFavoritos = { goToTab(Routes.FAVORITOS) },
                    onCompartir = { goTo(Routes.COMPARTIR) },
                    onAjustes = { goTo(Routes.AJUSTES) }
                )
            }
            composable(Routes.EDITAR_PERFIL) {
                EditarPerfilScreen(vm = vm, onGuardar = { goBack() })
            }
            composable(Routes.AJUSTES) {
                AjustesScreen(
                    onPremium = { goTo(Routes.PREMIUM) },
                    onContexto = { goTo(Routes.CONTEXTO) },
                    onCerrarSesion = {
                        vm.logout()
                        goToTab(Routes.LOGIN)
                    },
                    onEliminarCuenta = { goTo(Routes.LOGOUT) }
                )
            }
            composable(Routes.PREMIUM) {
                PremiumScreen(onSuscribirse = { goBack() })
            }
            composable(Routes.CONTEXTO) {
                ContextoScreen()
            }
            composable(Routes.LOGOUT) {
                LogoutScreen(
                    onEliminar = {
                        vm.logout()
                        goToTab(Routes.LOGIN)
                    },
                    onCancelar = { goBack() }
                )
            }
        }
    }
}
