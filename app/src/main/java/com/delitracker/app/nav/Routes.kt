package com.delitracker.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.delitracker.app.R

object Routes {
    const val REGISTRO = "registro"
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val MAPA = "mapa"
    const val BUSCAR = "buscar"
    const val FILTROS = "filtros"
    const val DETALLE = "detalle"
    const val RUTA = "ruta"
    const val RESENA = "resena"
    const val AR = "ar"
    const val FAVORITOS = "favoritos"
    const val COMPARTIR = "compartir"
    const val INVITAR = "invitar"
    const val PERFIL = "perfil"
    const val EDITAR_PERFIL = "editarPerfil"
    const val AJUSTES = "ajustes"
    const val PREMIUM = "premium"
    const val CONTEXTO = "contexto"
    const val LOGOUT = "logout"

    /** Pantallas con bottom tab bar (equivalente a tabbar() en el prototipo). */
    val TAB_ROUTES = setOf(MAPA, FAVORITOS, PERFIL)
}

enum class TopBarStyle { NONE, BACK, CLOSE }

/** Título y estilo de topbar por ruta, tal como decide cada SCREENS.xxx() en el prototipo. */
fun topBarStyleFor(route: String): TopBarStyle = when (route) {
    Routes.REGISTRO, Routes.LOGIN, Routes.ONBOARDING,
    Routes.MAPA, Routes.FAVORITOS, Routes.PERFIL,
    Routes.RUTA, Routes.AR, Routes.COMPARTIR, Routes.INVITAR -> TopBarStyle.NONE

    Routes.FILTROS, Routes.RESENA, Routes.LOGOUT -> TopBarStyle.CLOSE

    else -> TopBarStyle.BACK
}

/**
 * @Composable porque el título sale de strings.xml vía stringResource(),
 * que solo puede llamarse en contexto @Composable.
 */
@Composable
fun topBarTitleFor(route: String): String = when (route) {
    Routes.BUSCAR -> stringResource(R.string.titulo_buscar)
    Routes.FILTROS -> stringResource(R.string.filtros)
    Routes.DETALLE -> stringResource(R.string.titulo_detalle)
    Routes.RESENA -> stringResource(R.string.rese_a)
    Routes.EDITAR_PERFIL -> stringResource(R.string.titulo_editar_perfil)
    Routes.AJUSTES -> stringResource(R.string.ajustes_de_cuenta)
    Routes.PREMIUM -> stringResource(R.string.titulo_premium)
    Routes.CONTEXTO -> stringResource(R.string.titulo_contexto)
    Routes.LOGOUT -> stringResource(R.string.titulo_eliminar_cuenta)
    else -> ""
}
