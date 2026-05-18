<template>
  <div v-if="partida" class="space-y-3">
    <!-- Panel superior compacto -->
    <section class="card-surface p-3 space-y-2">
      <div class="flex flex-wrap items-center gap-2 text-xs md:text-sm">
        <span class="inline-flex items-center gap-1 rounded-full px-2 py-1 bg-slate-100 text-slate-700 font-medium">
          <span>Fecha</span>
          <span>{{ formatearFecha(partida.fecha) }}</span>
        </span>

        <span class="inline-flex items-center gap-1 rounded-full px-2 py-1 bg-slate-100 text-slate-700 font-medium">
          <span>Lugar</span>
          <span>{{ partida.lugar }}</span>
        </span>

        <span class="inline-flex items-center gap-1 rounded-full px-2 py-1 bg-slate-100 text-slate-700 font-medium">
          <span>Personas</span>
          <span>{{ totalJugadoresCount }} / {{ (partida.jugadoresPorEquipo || 0) * 2 }}</span>
        </span>

        <span class="inline-flex items-center gap-1 rounded-full px-2 py-1 bg-amber-50 text-amber-800 font-medium border border-amber-200">
          <span>Resp.</span>
          <span>{{ ownersCount }}</span>
          <span>·</span>
          <span>Coorg. {{ coOrganizadoresCount }}</span>
        </span>
      </div>

      <!-- Botones de acción -->
      <div class="flex flex-wrap gap-2">
        <BaseButton
          variant="ghost"
          size="sm"
          @click="$emit('volver')"
        >
          Volver
        </BaseButton>

        <BaseButton
          v-if="partida.tipo === 'PRIVADO' && amigos.length > 0"
          variant="secondary"
          size="sm"
          @click="$emit('invitar')"
        >
          Invitar amigos
        </BaseButton>

        <BaseButton
          v-if="puedeMarcarPistaReservada"
          variant="success"
          size="sm"
          :loading="reservandoPista"
          :disabled="reservandoPista"
          @click="abrirModalReservaPista"
        >
          Marcar pista reservada
        </BaseButton>

        <BaseButton
          v-if="puedeGestionarOrganizadores"
          variant="primary"
          size="sm"
          @click="abrirModalEditar"
        >
          Editar fecha / lugar
        </BaseButton>

        <BaseButton
          v-if="totalJugadoresCount === 1"
          variant="danger"
          size="sm"
          @click="$emit('eliminar')"
        >
          Eliminar partido
        </BaseButton>

        <BaseButton
          v-else-if="totalJugadoresCount > 1"
          variant="secondary"
          size="sm"
          @click="mostrarModalAbandonar = true"
        >
          Salir de la organización
        </BaseButton>
      </div>
    </section>

    <section :class="['card-surface p-3 border', estadoActualOrganizador.containerClass]">
      <div class="flex items-center justify-between gap-2">
        <p :class="['text-xs uppercase tracking-wide font-semibold', estadoActualOrganizador.titleClass]">Tu estado actual</p>
        <span :class="['inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', estadoActualOrganizador.badgeClass]">
          <span :title="estadoActualOrganizador.roleTitle" class="inline-flex items-center">
            <AppIcon :name="estadoActualOrganizador.roleEmoji" :size="12" />
          </span>
          <span>{{ estadoActualOrganizador.label }}</span>
        </span>
      </div>
      <p :class="['text-sm mt-1', estadoActualOrganizador.descriptionClass]">{{ estadoActualOrganizador.description }}</p>
    </section>

    <!-- Modal para salida como organizador -->
    <div v-if="mostrarModalAbandonar" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
        <h3 class="text-xl font-bold text-slate-800 mb-4">Abandonar partido</h3>
        <p class="text-sm text-slate-600 mb-4">
          Si eres la única persona responsable, el partido se cancelará al salir. Si hay más personas en organización, puedes seleccionar a la nueva persona responsable.
        </p>

        <div class="space-y-2 mb-6 max-h-64 overflow-y-auto">
          <button
            v-for="org in organizadoresElegibles"
            :key="org.usuario.id"
            @click="nuevoCreadorSeleccionado = org.usuario.id"
            :class="[
              'w-full text-left p-3 rounded-lg border-2 transition-all',
              nuevoCreadorSeleccionado === org.usuario.id
                ? 'border-blue-500 bg-blue-50'
                : 'border-slate-200 hover:border-blue-300'
            ]"
          >
            <p class="font-semibold text-slate-800">{{ org.usuario.nombre }}</p>
            <p class="text-xs text-slate-600">
              {{ getRoleLabel(org.rol) }}
            </p>
          </button>
          <p v-if="organizadoresElegibles.length === 0" class="text-sm text-slate-600">
            No hay otra persona en organización disponible. Al confirmar, el partido será cancelado.
          </p>
        </div>

        <div class="flex gap-3">
          <BaseButton
            variant="ghost"
            :full-width="true"
            @click="cerrarModalAbandonar"
          >
            Cancelar
          </BaseButton>
          <BaseButton
            variant="danger"
            :full-width="true"
            :disabled="abandonando"
            :loading="abandonando"
            @click="confirmarAbandonar"
          >
            Confirmar y abandonar
          </BaseButton>
        </div>
      </div>
      </div>

      <div v-if="mostrarModalEditar" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 class="text-xl font-bold text-slate-800 mb-3">Editar fecha y lugar</h3>

          <label class="block text-sm font-medium text-slate-700 mb-1" for="fechaHora">Fecha y hora</label>
          <input
            id="fechaHora"
            v-model="nuevaFechaHora"
            type="datetime-local"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg mb-3"
          />

          <label class="block text-sm font-medium text-slate-700 mb-1" for="lugarEditar">Lugar</label>
          <input
            id="lugarEditar"
            v-model="nuevoLugar"
            type="text"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg"
            placeholder="Ej: Polideportivo Municipal"
          />

          <div class="mt-5 flex gap-3">
            <BaseButton variant="ghost" :full-width="true" :disabled="editando" @click="cerrarModalEditar">
              Cancelar
            </BaseButton>
            <BaseButton variant="primary" :full-width="true" :loading="editando" :disabled="editando" @click="confirmarEditar">
              Guardar cambios
            </BaseButton>
          </div>
        </div>
      </div>
    <div v-if="mostrarModalReservaPista" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
        <h3 class="text-xl font-bold text-slate-800 mb-3">Confirmar reserva de pista</h3>
        <p class="text-sm text-slate-600 mb-4">Indica el precio total de la pista. Se notificará a todas las personas (excepto tú) con el monto y la parte individual.</p>

        <label class="block text-sm font-medium text-slate-700 mb-1" for="precioPista">Precio total</label>
        <input
          id="precioPista"
          v-model="precioPistaTotal"
          type="number"
          min="0"
          step="0.01"
          class="w-full px-3 py-2 border border-slate-300 rounded-lg"
          placeholder="Ej: 60"
        />

        <div class="mt-5 flex gap-3">
          <BaseButton variant="ghost" :full-width="true" :disabled="reservandoPista" @click="cerrarModalReservaPista">
            Cancelar
          </BaseButton>
          <BaseButton variant="success" :full-width="true" :loading="reservandoPista" :disabled="reservandoPista" @click="marcarPistaReservada">
            Confirmar reserva
          </BaseButton>
        </div>
      </div>
    </div>

    <!-- Lista de personas sin equipo -->
    <section v-if="jugadoresSinEquipo.length > 0" class="card-surface p-4 space-y-3">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-bold text-slate-800">
          Personas sin equipo ({{ jugadoresSinEquipo.length }})
        </h3>
        <span v-if="puedeGestionarOrganizadores" class="text-xs text-slate-500">
          Delega desde cada persona
        </span>
      </div>

      <div class="space-y-2">
        <div
          v-for="jugador in jugadoresSinEquipo"
          :key="jugador.id"
          :class="[
            'flex items-center justify-between p-3 rounded-lg hover:shadow-md transition-shadow border-2',
            String(jugador.id) === String(currentUserId) ? 'bg-white border-black' : 'bg-white border-slate-200'
          ]"
        >
          <div class="flex items-center gap-3">
            <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pago confirmado">
              <AppIcon name="check" :size="12" />
            </span>
            <div class="w-10 h-10 rounded-full bg-slate-200 flex items-center justify-center">
              <AppIcon name="user" :size="16" />
            </div>
            <div>
              <p class="font-semibold text-slate-800">
                {{ jugador.nombre }}
                <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
              </p>
              <p class="text-xs text-slate-600" v-if="jugador.playTendency">
                Tendencia: {{ formatTendencia(jugador.playTendency) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2 flex-wrap justify-end">
            <span
              v-if="esOrganizadorJugador(jugador.id)"
              class="inline-flex items-center gap-1 rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-semibold text-amber-800"
              :title="getRoleLabel(rolOrganizadorJugador(jugador.id))"
            >
              <span class="inline-flex items-center"><AppIcon :name="getRoleIcon(rolOrganizadorJugador(jugador.id))" :size="12" /></span>
              <span>{{ getRoleShortLabel(rolOrganizadorJugador(jugador.id)) }}</span>
            </span>

            <button
              v-if="puedeQuitarCoorganizador(jugador.id)"
              type="button"
              @click="quitarCoorganizador(jugador)"
              :disabled="gestionandoOrganizador === `remove-${jugador.id}`"
              class="inline-flex items-center rounded-full border border-red-200 bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-700 hover:bg-red-100 disabled:opacity-50 disabled:cursor-not-allowed"
              title="Quitar rol de coorganización"
            >
              <span v-if="gestionandoOrganizador === `remove-${jugador.id}`">Procesando</span>
              <span v-else>Quitar coorganización</span>
            </button>

            <button
              v-else-if="puedeGestionarOrganizadores && !esOrganizadorJugador(jugador.id)"
              type="button"
              @click="delegarOrganizacion(jugador)"
              :disabled="gestionandoOrganizador === `add-${jugador.id}`"
              class="inline-flex items-center rounded-full border border-slate-300 bg-white px-2 py-0.5 text-[11px] font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
              title="Delegar responsabilidades de organización"
            >
              <span v-if="gestionandoOrganizador === `add-${jugador.id}`">Procesando</span>
              <span v-else>Delegar</span>
            </button>

            <button
              type="button"
              @click="asignarEquipo(jugador.id, 'A')"
              :disabled="asignando === `${jugador.id}-A`"
              :aria-label="`Asignar a equipo ${partida.colorEquipoA || 'A'}`"
              :class="[
                'px-3 py-1.5 text-sm font-semibold rounded-lg transition-all min-h-[44px] min-w-[44px] border',
                'disabled:opacity-50 disabled:cursor-not-allowed',
                'hover:opacity-90'
              ]"
              :style="teamAColor.chipStyle"
            >
              <span v-if="asignando === `${jugador.id}-A`">Procesando</span>
              <span v-else>Equipo {{ partida.colorEquipoA || 'A' }}</span>
            </button>
            <button
              type="button"
              @click="asignarEquipo(jugador.id, 'B')"
              :disabled="asignando === `${jugador.id}-B`"
              :aria-label="`Asignar a equipo ${partida.colorEquipoB || 'B'}`"
              :class="[
                'px-3 py-1.5 text-sm font-semibold rounded-lg transition-all min-h-[44px] min-w-[44px] border',
                'disabled:opacity-50 disabled:cursor-not-allowed',
                'hover:opacity-90'
              ]"
              :style="teamBColor.chipStyle"
            >
              <span v-if="asignando === `${jugador.id}-B`">Procesando</span>
              <span v-else>Equipo {{ partida.colorEquipoB || 'B' }}</span>
            </button>

            <button
              v-if="puedeExpulsarParticipante && !esOrganizadorJugador(jugador.id)"
              type="button"
              @click="expulsarParticipante(jugador)"
              :disabled="gestionandoExpulsion === `${jugador.id}`"
              class="inline-flex items-center rounded-full border border-red-200 bg-red-50 px-2 py-0.5 text-[11px] font-semibold text-red-700 hover:bg-red-100 disabled:opacity-50 disabled:cursor-not-allowed"
              title="Expulsar del partido"
            >
              <span v-if="gestionandoExpulsion === `${jugador.id}`">Procesando</span>
              <span v-else>Expulsar</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Botón balancear automáticamente -->
      <BaseButton
        v-if="totalJugadoresCount === (partida.jugadoresPorEquipo || 0) * 2"
        variant="success"
        :full-width="true"
        @click="balancearAutomaticamente"
        :loading="balanceando"
      >
        Asignar equipos automáticamente
      </BaseButton>
      <div v-else class="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-3">
        Se necesitan {{ (partida.jugadoresPorEquipo || 0) * 2 }} personas para asignar automáticamente (faltan {{ ((partida.jugadoresPorEquipo || 0) * 2) - totalJugadoresCount }})
      </div>
    </section>

    <!-- Vista de equipos -->
    <section class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- Equipo A -->
      <article class="p-4 border-2 space-y-3 rounded-lg shadow-sm card-surface" :style="teamAColor.cardStyle">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold flex items-center gap-2" :style="teamAColor.titleStyle">
            <span class="inline-flex h-8 w-8 items-center justify-center rounded-full" :style="teamAColor.chipStyle">
              <AppIcon name="soccer" :size="14" :style="teamAColor.iconStyle" />
            </span>
            <span>Equipo {{ partida.colorEquipoA || 'A' }}</span>
          </h2>
          <span class="px-3 py-1 text-sm font-bold rounded-full" :style="teamAColor.chipStyle">
            {{ partida.equipoA?.length || 0 }} / {{ partida.jugadoresPorEquipo }}
          </span>
        </div>

        <div v-if="!partida.equipoA || partida.equipoA.length === 0" class="text-sm italic p-4 rounded-lg text-center" :style="teamAColor.mutedTextStyle">
          Sin personas asignadas
        </div>

        <div v-else class="space-y-2">
          <div
            v-for="jugador in partida.equipoA"
            :key="`equipo-a-${jugador.id}`"
            :class="[
              'rounded-lg p-3 border-2 flex items-center justify-between gap-3',
              String(jugador.id) === String(currentUserId) ? 'border-black' : ''
            ]"
            :style="teamAColor.playerRowStyle"
          >
            <button
              type="button"
              @click="cambiarDeEquipo(jugador.id, 'B')"
              :disabled="asignando === `${jugador.id}-cambiar`"
              :aria-label="`Cambiar a equipo ${partida.colorEquipoB || 'B'}`"
              :class="['text-xs font-medium px-2 py-1 rounded min-h-[44px] min-w-[44px] border hover:opacity-80']"
              :style="teamAColor.chipStyle"
              :title="`Cambiar a equipo ${partida.colorEquipoB || 'B'}`"
            >
              <span v-if="asignando === `${jugador.id}-cambiar`">Procesando</span>
              <span v-else>⇄</span>
            </button>

            <div class="flex items-center gap-3 flex-1">
              <div class="w-10 h-10 rounded-full flex items-center justify-center" :style="teamAColor.avatarStyle">
                <AppIcon name="user" :size="16" :style="teamAColor.iconStyle" />
              </div>
              <div class="min-w-0">
                <p class="font-semibold" :style="teamAColor.playerTextStyle">
                  {{ jugador.nombre }}
                  <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
                </p>
                <p class="text-xs" :style="teamAColor.mutedTextStyle" v-if="jugador.playTendency">
                  Tendencia: {{ formatTendencia(jugador.playTendency) }}
                </p>
              </div>
            </div>

            <div class="ml-auto flex items-center gap-2">
              <span
                v-if="esOrganizadorJugador(jugador.id)"
                class="inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-semibold"
                :style="teamAColor.auraStyle"
                :title="getRoleLabel(rolOrganizadorJugador(jugador.id))"
              >
                <span class="inline-flex items-center"><AppIcon :name="getRoleIcon(rolOrganizadorJugador(jugador.id))" :size="12" :style="teamAColor.iconStyle" /></span>
                <span>{{ getRoleShortLabel(rolOrganizadorJugador(jugador.id)) }}</span>
              </span>

              <button
                v-if="puedeQuitarCoorganizador(jugador.id)"
                type="button"
                @click="quitarCoorganizador(jugador)"
                :disabled="gestionandoOrganizador === `remove-${jugador.id}`"
                class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold disabled:opacity-50 disabled:cursor-not-allowed border-[color:rgba(151,240,125,0.24)] bg-[color:rgba(102,63,77,0.22)] hover:bg-[color:rgba(102,63,77,0.34)]"
                :style="teamAColor.buttonTextStyle"
                title="Quitar rol de coorganización"
              >
                <span v-if="gestionandoOrganizador === `remove-${jugador.id}`">Procesando</span>
                <span v-else>Quitar</span>
              </button>

              <button
                v-else-if="puedeGestionarOrganizadores && !esOrganizadorJugador(jugador.id)"
                type="button"
                @click="delegarOrganizacion(jugador)"
                :disabled="gestionandoOrganizador === `add-${jugador.id}`"
                class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold disabled:opacity-50 disabled:cursor-not-allowed border-[color:rgba(151,240,125,0.24)] bg-[color:rgba(102,63,77,0.22)] hover:bg-[color:rgba(102,63,77,0.34)]"
                :style="teamAColor.buttonTextStyle"
                title="Delegar responsabilidades de organización"
              >
                <span v-if="gestionandoOrganizador === `add-${jugador.id}`">Procesando</span>
                <span v-else>Delegar</span>
              </button>

              <button
                v-if="puedeExpulsarParticipante && !esOrganizadorJugador(jugador.id)"
                type="button"
                @click="expulsarParticipante(jugador)"
                :disabled="gestionandoExpulsion === `${jugador.id}`"
                class="expel-button inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                title="Expulsar del partido"
              >
                <span v-if="gestionandoExpulsion === `${jugador.id}`">Procesando</span>
                <span v-else>Expulsar</span>
              </button>

              <button
                type="button"
                @click="moverASinEquipo(jugador.id)"
                :disabled="asignando === `${jugador.id}-remove`"
                :aria-label="`Mover a sin equipo`"
                :class="['text-xs font-medium px-2 py-1 min-h-[44px] min-w-[44px] border hover:opacity-80']"
                :style="teamAColor.auraStyle"
              >
                <span v-if="asignando === `${jugador.id}-remove`">Procesando</span>
                <span v-else>✕</span>
              </button>
            </div>
          </div>
        </div>
      </article>

      <!-- Equipo B -->
      <article class="p-4 border-2 space-y-3 rounded-lg shadow-sm card-surface" :style="teamBColor.cardStyle">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold flex items-center gap-2" :style="teamBColor.titleStyle">
            <span class="inline-flex h-8 w-8 items-center justify-center rounded-full" :style="teamBColor.chipStyle">
              <AppIcon name="soccer" :size="14" :style="teamBColor.iconStyle" />
            </span>
            <span>Equipo {{ partida.colorEquipoB || 'B' }}</span>
          </h2>
          <span class="px-3 py-1 text-sm font-bold rounded-full" :style="teamBColor.chipStyle">
            {{ partida.equipoB?.length || 0 }} / {{ partida.jugadoresPorEquipo }}
          </span>
        </div>

        <div v-if="!partida.equipoB || partida.equipoB.length === 0" class="text-sm italic p-4 rounded-lg text-center" :style="teamBColor.mutedTextStyle">
          Sin personas asignadas
        </div>

        <div v-else class="space-y-2">
          <div
            v-for="jugador in partida.equipoB"
            :key="`equipo-b-${jugador.id}`"
            :class="[
              'rounded-lg p-3 border-2 flex items-center justify-between gap-3',
              String(jugador.id) === String(currentUserId) ? 'border-black' : ''
            ]"
            :style="teamBColor.playerRowStyle"
          >
            <button
              type="button"
              @click="cambiarDeEquipo(jugador.id, 'A')"
              :disabled="asignando === `${jugador.id}-cambiar`"
              :aria-label="`Cambiar a equipo ${partida.colorEquipoA || 'A'}`"
              :class="['text-xs font-medium px-2 py-1 rounded min-h-[44px] min-w-[44px] border hover:opacity-80']"
              :style="teamBColor.chipStyle"
              :title="`Cambiar a equipo ${partida.colorEquipoA || 'A'}`"
            >
              <span v-if="asignando === `${jugador.id}-cambiar`">Procesando</span>
              <span v-else>⇄</span>
            </button>

            <div class="flex items-center gap-3 flex-1">
              <span v-if="usuarioPagoConfirmado(jugador.id)" class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-100 text-emerald-700" title="Pago confirmado">
                <AppIcon name="check" :size="12" />
              </span>
              <div class="w-10 h-10 rounded-full flex items-center justify-center" :style="teamBColor.avatarStyle">
                <AppIcon name="user" :size="16" :style="teamBColor.iconStyle" />
              </div>
              <div class="min-w-0">
                <p class="font-semibold" :style="teamBColor.playerTextStyle">
                  {{ jugador.nombre }}
                  <span v-if="String(jugador.id) === String(currentUserId)" class="ml-1.5 text-xs font-bold">(Tú)</span>
                </p>
                <p class="text-xs" :style="teamBColor.mutedTextStyle" v-if="jugador.playTendency">
                  Tendencia: {{ formatTendencia(jugador.playTendency) }}
                </p>
              </div>
            </div>

            <div class="ml-auto flex items-center gap-2">
              <span
                v-if="esOrganizadorJugador(jugador.id)"
                class="inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-semibold"
                :style="teamBColor.auraStyle"
                :title="getRoleLabel(rolOrganizadorJugador(jugador.id))"
              >
                <span class="inline-flex items-center"><AppIcon :name="getRoleIcon(rolOrganizadorJugador(jugador.id))" :size="12" :style="teamBColor.iconStyle" /></span>
                <span>{{ getRoleShortLabel(rolOrganizadorJugador(jugador.id)) }}</span>
              </span>

              <button
                v-if="puedeQuitarCoorganizador(jugador.id)"
                type="button"
                @click="quitarCoorganizador(jugador)"
                :disabled="gestionandoOrganizador === `remove-${jugador.id}`"
                class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold disabled:opacity-50 disabled:cursor-not-allowed border-[color:rgba(151,240,125,0.24)] bg-[color:rgba(102,63,77,0.22)] hover:bg-[color:rgba(102,63,77,0.34)]"
                :style="teamBColor.buttonTextStyle"
                title="Quitar rol de coorganización"
              >
                <span v-if="gestionandoOrganizador === `remove-${jugador.id}`">Procesando</span>
                <span v-else>Quitar</span>
              </button>

              <button
                v-else-if="puedeGestionarOrganizadores && !esOrganizadorJugador(jugador.id)"
                type="button"
                @click="delegarOrganizacion(jugador)"
                :disabled="gestionandoOrganizador === `add-${jugador.id}`"
                class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold disabled:opacity-50 disabled:cursor-not-allowed border-[color:rgba(151,240,125,0.24)] bg-[color:rgba(102,63,77,0.22)] hover:bg-[color:rgba(102,63,77,0.34)]"
                :style="teamBColor.buttonTextStyle"
                title="Delegar responsabilidades de organización"
              >
                <span v-if="gestionandoOrganizador === `add-${jugador.id}`">Procesando</span>
                <span v-else>Delegar</span>
              </button>

              <button
                v-if="puedeExpulsarParticipante && !esOrganizadorJugador(jugador.id)"
                type="button"
                @click="expulsarParticipante(jugador)"
                :disabled="gestionandoExpulsion === `${jugador.id}`"
                class="expel-button inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                title="Expulsar del partido"
              >
                <span v-if="gestionandoExpulsion === `${jugador.id}`">Procesando</span>
                <span v-else>Expulsar</span>
              </button>

              <button
                type="button"
                @click="moverASinEquipo(jugador.id)"
                :disabled="asignando === `${jugador.id}-remove`"
                :aria-label="`Mover a sin equipo`"
                :class="['text-xs font-medium px-2 py-1 min-h-[44px] min-w-[44px] border hover:opacity-80']"
                :style="teamBColor.auraStyle"
              >
                <span v-if="asignando === `${jugador.id}-remove`">Procesando</span>
                <span v-else>✕</span>
              </button>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import BaseButton from '../ui/BaseButton.vue'
import AppIcon from '../ui/AppIcon.vue'
import partidoService from '../../services/partidoService'
import { useUiStore } from '../../stores/ui'
import { useAuthStore } from '../../stores/auth'
import { formatDateTimeEs } from '../../utils/dateFormat'

const uiStore = useUiStore()
const authStore = useAuthStore()

const props = defineProps({
  partida: {
    type: Object,
    required: true,
  },
  amigos: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['actualizar', 'invitar', 'eliminar', 'abandonar', 'volver', 'cancelado'])

const asignando = ref('')
const balanceando = ref(false)
const mostrarModalAbandonar = ref(false)
const mostrarModalReservaPista = ref(false)
const nuevoCreadorSeleccionado = ref(null)
const abandonando = ref(false)
const gestionandoOrganizador = ref('')
const reservandoPista = ref(false)
const precioPistaTotal = ref('')
const estadoPagoReserva = ref({})
const mostrarModalEditar = ref(false)
const nuevaFechaHora = ref('')
const nuevoLugar = ref('')
const editando = ref(false)

const teamPalette = {
  Blanco: { jersey: '#ffffff', onJersey: '#111827', border: 'rgba(255, 255, 255, 0.62)', aura: 'rgba(255, 255, 255, 0.24)' },
  Negro: { jersey: '#050505', onJersey: '#f8fafc', border: 'rgba(255, 255, 255, 0.2)', aura: 'rgba(255, 255, 255, 0.08)' },
  Rojo: { jersey: '#ef4444', onJersey: '#ffffff', border: 'rgba(239, 68, 68, 0.55)', aura: 'rgba(239, 68, 68, 0.18)' },
  Azul: { jersey: '#2563eb', onJersey: '#ffffff', border: 'rgba(37, 99, 235, 0.55)', aura: 'rgba(37, 99, 235, 0.18)' },
  Verde: { jersey: '#16a34a', onJersey: '#ffffff', border: 'rgba(22, 163, 74, 0.55)', aura: 'rgba(22, 163, 74, 0.18)' },
  Amarillo: { jersey: '#facc15', onJersey: '#111827', border: 'rgba(250, 204, 21, 0.7)', aura: 'rgba(250, 204, 21, 0.2)' },
  Naranja: { jersey: '#f97316', onJersey: '#ffffff', border: 'rgba(249, 115, 22, 0.55)', aura: 'rgba(249, 115, 22, 0.18)' },
  Morado: { jersey: '#a855f7', onJersey: '#ffffff', border: 'rgba(168, 85, 247, 0.55)', aura: 'rgba(168, 85, 247, 0.18)' },
}

const normalizeTeamColorKey = (value) => {
  if (!value) return ''
  return String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()
}

const rgbStringToHex = (value) => {
  const match = String(value).match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/i)
  if (!match) return null
  const r = Math.max(0, Math.min(255, Number(match[1])))
  const g = Math.max(0, Math.min(255, Number(match[2])))
  const b = Math.max(0, Math.min(255, Number(match[3])))
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
}

const toHexColor = (value) => {
  if (!value) return null
  const raw = String(value).trim().toLowerCase()
  if (/^#[0-9a-f]{6}$/i.test(raw)) return raw
  if (/^#[0-9a-f]{3}$/i.test(raw)) {
    return `#${raw[1]}${raw[1]}${raw[2]}${raw[2]}${raw[3]}${raw[3]}`
  }
  return rgbStringToHex(raw)
}

const isForcedBlackTextTeam = (value) => {
  const key = normalizeTeamColorKey(value)
  return key === 'blanco' || key === 'amarillo'
}

const isLightJersey = (hexColor) => {
  if (!hexColor || !hexColor.startsWith('#') || hexColor.length !== 7) return false
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
  return luminance >= 0.68
}

const foregroundForHex = (hexColor) => (isLightJersey(hexColor) ? '#111827' : '#f8fafc')

const resolveTeamTone = (name, fallback) => {
  const explicitHex = toHexColor(name)
  if (explicitHex) {
    return {
      jersey: explicitHex,
      onJersey: foregroundForHex(explicitHex),
      border: 'rgba(148, 163, 184, 0.48)',
      aura: 'rgba(148, 163, 184, 0.2)',
    }
  }

  const normalizedName = normalizeTeamColorKey(name)
  const normalizedFallback = normalizeTeamColorKey(fallback)

  const directTone = Object.entries(teamPalette).find(([key]) => normalizeTeamColorKey(key) === normalizedName)?.[1]
  if (directTone) return directTone

  const fallbackTone = Object.entries(teamPalette).find(([key]) => normalizeTeamColorKey(key) === normalizedFallback)?.[1]
  return fallbackTone || teamPalette.Blanco
}

const createTeamTone = (name, fallback) => {
  const tone = resolveTeamTone(name, fallback)
  const darkText = isLightJersey(tone.jersey) || isForcedBlackTextTeam(name)
  const cardForeground = darkText ? '#111827' : '#f8fafc'
  const mutedForeground = darkText ? '#334155' : '#cbd5e1'
  return {
    cardStyle: {
      backgroundColor: tone.jersey,
      borderColor: tone.border,
      color: cardForeground,
      boxShadow: '0 14px 30px rgba(12, 0, 5, 0.22)',
    },
    titleStyle: { color: cardForeground },
    playerTextStyle: { color: cardForeground },
    mutedTextStyle: { color: mutedForeground },
    buttonTextStyle: { color: cardForeground },
    chipStyle: {
      backgroundColor: tone.jersey,
      color: darkText ? '#111827' : tone.onJersey,
      boxShadow: `0 0 0 1px ${tone.border}, 0 0 16px ${tone.aura}`,
    },
    avatarStyle: {
      backgroundColor: tone.jersey,
      color: darkText ? '#111827' : tone.onJersey,
      boxShadow: `0 0 0 4px ${tone.aura}, 0 0 0 1px ${tone.border}`,
    },
    auraStyle: {
      backgroundColor: 'rgba(151, 240, 125, 0.14)',
      color: 'var(--color-secondary)',
      boxShadow: '0 0 0 1px var(--color-border)',
    },
    playerRowStyle: {
      backgroundColor: darkText ? 'rgba(255, 255, 255, 0.35)' : 'rgba(0, 0, 0, 0.14)',
      borderColor: tone.border,
    },
    iconStyle: {
      color: 'var(--color-secondary)',
    },
  }
}

const teamAColor = computed(() => {
  return createTeamTone(props.partida?.colorEquipoA, 'Blanco')
})

const teamBColor = computed(() => {
  return createTeamTone(props.partida?.colorEquipoB, 'Negro')
})

const jugadoresSinEquipo = computed(() => {
  return props.partida?.jugadoresInscritos || []
})

const totalJugadoresCount = computed(() => {
  const inscritos = props.partida?.jugadoresInscritos?.length || 0
  const equipoA = props.partida?.equipoA?.length || 0
  const equipoB = props.partida?.equipoB?.length || 0
  return inscritos + equipoA + equipoB
})

const currentUserId = computed(() => authStore.user?.id || null)
const gestionandoExpulsion = ref('')

const miRolOrganizador = computed(() => {
  const org = (props.partida?.organizadores || []).find(
    (o) => String(o?.usuario?.id) === String(currentUserId.value)
  )
  return org?.rol || null
})

const getRoleLabel = (role) => {
  if (role === 'OWNER') return 'Responsable principal'
  if (role === 'CO_ORGANIZER') return 'Coorganización'
  return 'Sin rol'
}

const getRoleShortLabel = (role) => {
  if (role === 'OWNER') return 'Responsable'
  if (role === 'CO_ORGANIZER') return 'Coorganización'
  return 'Sin rol'
}

const getRoleIcon = (role) => {
  if (role === 'OWNER') return 'crown'
  if (role === 'CO_ORGANIZER') return 'settings'
  return 'circle'
}

const estadoActualOrganizador = computed(() => {
  const uid = currentUserId.value
  const enEquipoA = (props.partida?.equipoA || []).some((j) => String(j?.id) === String(uid))
  const enEquipoB = (props.partida?.equipoB || []).some((j) => String(j?.id) === String(uid))
  const pendiente = (props.partida?.jugadoresInscritos || []).some((j) => String(j?.id) === String(uid))

  const roleEmoji = miRolOrganizador.value === 'OWNER' ? 'crown' : 'settings'
  const roleTitle = getRoleLabel(miRolOrganizador.value)

  if (enEquipoA) {
    return {
      label: `${roleTitle} · Equipo ${props.partida?.colorEquipoA || 'A'}`,
      description: `Eres ${roleTitle} y estás asignado al Equipo ${props.partida?.colorEquipoA || 'A'}.`,
      roleEmoji,
      roleTitle,
      containerClass: 'border-emerald-200 bg-emerald-50',
      badgeClass: 'bg-emerald-100 text-emerald-800',
      titleClass: 'text-emerald-700',
      descriptionClass: 'text-emerald-900',
    }
  }

  if (enEquipoB) {
    return {
      label: `${roleTitle} · Equipo ${props.partida?.colorEquipoB || 'B'}`,
      description: `Eres ${roleTitle} y estás asignado al Equipo ${props.partida?.colorEquipoB || 'B'}.`,
      roleEmoji,
      roleTitle,
      containerClass: 'border-emerald-200 bg-emerald-50',
      badgeClass: 'bg-emerald-100 text-emerald-800',
      titleClass: 'text-emerald-700',
      descriptionClass: 'text-emerald-900',
    }
  }

  if (pendiente) {
    return {
      label: `${roleTitle} · Pendiente de asignación`,
      description: `Eres ${roleTitle} y estás inscrita/o pendiente de equipo.`,
      roleEmoji,
      roleTitle,
      containerClass: 'border-amber-200 bg-amber-50',
      badgeClass: 'bg-amber-100 text-amber-800',
      titleClass: 'text-amber-700',
      descriptionClass: 'text-amber-900',
    }
  }

  return {
    label: `${roleTitle} · No inscrita/o`,
    description: `Eres ${roleTitle}. Aún no estás inscrita/o como persona jugadora en este partido.`,
    roleEmoji,
    roleTitle,
    containerClass: 'border-slate-200 bg-slate-50',
    badgeClass: 'bg-slate-100 text-slate-700',
    titleClass: 'text-slate-500',
    descriptionClass: 'text-slate-700',
  }
})

const ownerActualId = computed(() => {
  return props.partida?.owner?.id || props.partida?.creador?.id || null
})

const ownersCount = computed(() => {
  const organizadores = props.partida?.organizadores || []
  return organizadores.filter(o => o.rol === 'OWNER').length
})

const coOrganizadoresCount = computed(() => {
  const organizadores = props.partida?.organizadores || []
  return organizadores.filter(o => o.rol === 'CO_ORGANIZER').length
})

const puedeGestionarOrganizadores = computed(() => miRolOrganizador.value === 'OWNER')
const puedeExpulsarParticipante = computed(() => Boolean(miRolOrganizador.value))

const puedeMarcarPistaReservada = computed(() => {
  const estado = props.partida?.estado
  return estado !== 'CONFIRMADO' && estado !== 'FINALIZADO' && estado !== 'CANCELADO'
})

const usuarioPagoConfirmado = (jugadorId) => {
  return Boolean(estadoPagoReserva.value?.[jugadorId])
}

const abrirModalEditar = () => {
  mostrarModalEditar.value = true
  // inicializar valores con los actuales
  if (props.partida?.fecha) {
    // Expecting formato like '2023-05-18T20:00:00' → strip seconds
    nuevaFechaHora.value = String(props.partida.fecha).replace(/:00$/, '')
  } else {
    nuevaFechaHora.value = ''
  }
  nuevoLugar.value = props.partida?.lugar || ''
}

const cerrarModalEditar = () => {
  mostrarModalEditar.value = false
  nuevaFechaHora.value = ''
  nuevoLugar.value = ''
  editando.value = false
}

const confirmarEditar = async () => {
  if (!nuevaFechaHora.value && !nuevoLugar.value) {
    uiStore.showToast({ message: 'Indica una fecha o lugar válido.', type: 'warning' })
    return
  }
  editando.value = true
  try {
    const payload = {}
    if (nuevaFechaHora.value) {
      payload.fecha = nuevaFechaHora.value.includes(':00') ? nuevaFechaHora.value : `${nuevaFechaHora.value}:00`
    }
    if (nuevoLugar.value) payload.lugar = nuevoLugar.value.trim()

    const res = await partidoService.actualizarPartido(props.partida.id, payload)
    uiStore.showToast({ message: 'Partido actualizado.', type: 'success' })
    emit('actualizar')
    cerrarModalEditar()
  } catch (err) {
    uiStore.showToast({ message: err?.message || 'Error al actualizar partido.', type: 'error' })
  } finally {
    editando.value = false
  }
}

const cargarEstadoPagoReserva = async () => {
  if (!props.partida?.id) {
    estadoPagoReserva.value = {}
    return
  }

  try {
    estadoPagoReserva.value = await partidoService.obtenerEstadoPagoReserva(props.partida.id)
  } catch (error) {
    estadoPagoReserva.value = {}
  }
}

onMounted(async () => {
  await cargarEstadoPagoReserva()
})

watch(
  () => [props.partida?.id, props.partida?.actualizadoEn],
  async () => {
    await cargarEstadoPagoReserva()
  }
)

const organizadoresPorJugador = computed(() => {
  const map = new Map()
  ;(props.partida?.organizadores || []).forEach((organizador) => {
    const id = organizador?.usuario?.id
    if (id !== undefined && id !== null) {
      map.set(String(id), organizador?.rol || null)
    }
  })
  return map
})

const esOrganizadorJugador = (jugadorId) => organizadoresPorJugador.value.has(String(jugadorId))

const rolOrganizadorJugador = (jugadorId) => organizadoresPorJugador.value.get(String(jugadorId)) || null

const puedeQuitarCoorganizador = (jugadorId) => {
  if (!puedeGestionarOrganizadores.value) {
    return false
  }
  return rolOrganizadorJugador(jugadorId) === 'CO_ORGANIZER'
}

const organizadoresElegibles = computed(() => {
  const organizadores = props.partida?.organizadores || []
  return organizadores.filter(o => o.usuario?.id !== ownerActualId.value)
})

const formatearFecha = (fecha) => {
  return formatDateTimeEs(fecha)
}

const formatTendencia = (playTendency) => {
  if (!playTendency) return ''
  const tendencias = {
    'OFENSIVA': 'Ofensiva',
    'DEFENSIVA': 'Defensiva',
    'ADAPTABLE': 'Adaptable'
  }
  return tendencias[playTendency] || playTendency
}

const asignarEquipo = async (jugadorId, equipo) => {
  asignando.value = `${jugadorId}-${equipo}`
  try {
    await partidoService.moverJugadorAEquipo(props.partida.id, jugadorId, equipo)
    emit('actualizar')
    uiStore.showToast({ 
      message: `Persona asignada al equipo ${equipo}`, 
      type: 'success' 
    })
  } catch (error) {
    uiStore.showToast({ 
      message: error.message || 'Error al asignar persona', 
      type: 'error' 
    })
  } finally {
    asignando.value = ''
  }
}

const moverASinEquipo = async (jugadorId) => {
  asignando.value = `${jugadorId}-remove`
  try {
    await partidoService.moverJugadorASinEquipo(props.partida.id, jugadorId)
    emit('actualizar')
    uiStore.showToast({ 
      message: 'Persona movida a lista de inscritas/os', 
      type: 'success' 
    })
  } catch (error) {
    uiStore.showToast({ 
      message: error.message || 'Error al mover persona', 
      type: 'error' 
    })
  } finally {
    asignando.value = ''
  }
}

const expulsarParticipante = async (jugador) => {
  if (!puedeExpulsarParticipante.value || !jugador?.id || esOrganizadorJugador(jugador.id)) {
    return
  }

  gestionandoExpulsion.value = `${jugador.id}`
  try {
    await partidoService.expulsarJugador(props.partida.id, jugador.id)
    emit('actualizar')
    uiStore.showToast({
      message: `${jugador.nombre} ha sido expulsado/a del partido`,
      type: 'success',
    })
  } catch (error) {
    uiStore.showToast({
      message: error.message || 'Error al expulsar persona',
      type: 'error',
    })
  } finally {
    gestionandoExpulsion.value = ''
  }
}

const cambiarDeEquipo = async (jugadorId, equipoDestino) => {
  asignando.value = `${jugadorId}-cambiar`
  try {
    await partidoService.moverJugadorAEquipo(props.partida.id, jugadorId, equipoDestino)
    emit('actualizar')
    uiStore.showToast({ 
      message: `Persona cambiada al equipo ${equipoDestino}`, 
      type: 'success' 
    })
  } catch (error) {
    uiStore.showToast({ 
      message: error.message || 'Error al cambiar de equipo', 
      type: 'error' 
    })
  } finally {
    asignando.value = ''
  }
}

const construirMensajeBalance = (resultado) => {
  // Mensaje simple: solo muestra los niveles finales de cada equipo
  const nivelA = Number(resultado?.nivelTotalEquipoA) || 0
  const nivelB = Number(resultado?.nivelTotalEquipoB) || 0
  const balanceado = Boolean(resultado?.balanceadoDespues)

  const estado = balanceado
    ? 'Equipos equilibrados'
    : 'Equipos asignados'

  return `${estado}\nEquipo A: ${nivelA} | Equipo B: ${nivelB}`
}

const balancearAutomaticamente = async () => {
  balanceando.value = true
  try {
    const resultado = await partidoService.balancearEquiposAutomaticamente(props.partida.id)
    emit('actualizar')

    await uiStore.askConfirm({
      title: 'Balanceo aplicado',
      message: construirMensajeBalance(resultado),
      confirmLabel: 'Entendido',
      cancelLabel: 'Cerrar',
      variant: 'info',
    })

    uiStore.showToast({ 
      message: 'Equipos balanceados automáticamente', 
      type: 'success' 
    })
  } catch (error) {
    uiStore.showToast({ 
      message: error.message || 'Error al balancear equipos', 
      type: 'error' 
    })
  } finally {
    balanceando.value = false
  }
}

const delegarOrganizacion = async (jugador) => {
  if (!puedeGestionarOrganizadores.value || !jugador?.id) {
    return
  }

  if (esOrganizadorJugador(jugador.id)) {
    return
  }

  gestionandoOrganizador.value = `add-${jugador.id}`
  try {
    await partidoService.asignarOrganizador(props.partida.id, jugador.id)
    emit('actualizar')
    uiStore.showToast({
      message: `${jugador.nombre} ahora forma parte de la coorganización`,
      type: 'success',
    })
  } catch (error) {
    uiStore.showToast({
      message: error.message || 'Error al delegar organización',
      type: 'error',
    })
  } finally {
    gestionandoOrganizador.value = ''
  }
}

const quitarCoorganizador = async (jugador) => {
  if (!puedeQuitarCoorganizador(jugador?.id) || !jugador?.id) {
    return
  }

  gestionandoOrganizador.value = `remove-${jugador.id}`
  try {
    await partidoService.revocarOrganizador(props.partida.id, jugador.id)
    emit('actualizar')
    uiStore.showToast({
      message: `${jugador.nombre} ya no forma parte de la coorganización`,
      type: 'success',
    })
  } catch (error) {
    uiStore.showToast({
      message: error.message || 'Error al quitar rol de coorganización',
      type: 'error',
    })
  } finally {
    gestionandoOrganizador.value = ''
  }
}

const cerrarModalAbandonar = () => {
  mostrarModalAbandonar.value = false
  nuevoCreadorSeleccionado.value = null
}

const confirmarAbandonar = async () => {
  abandonando.value = true
  try {
    const esUnicoResponsable = ownersCount.value === 1 && organizadoresElegibles.value.length === 0

    if (esUnicoResponsable) {
      await partidoService.eliminarPartido(props.partida.id)
      uiStore.showToast({
        message: 'El partido se canceló al salir de la organización',
        type: 'success',
      })
      cerrarModalAbandonar()
      emit('cancelado')
      return
    }

    const payload = {}
    if (nuevoCreadorSeleccionado.value) {
      payload.nuevoOwnerId = nuevoCreadorSeleccionado.value
    }

    await partidoService.salirComoOrganizador(props.partida.id, payload)
    uiStore.showToast({ 
      message: 'Ya no eres persona organizadora del partido', 
      type: 'success' 
    })
    cerrarModalAbandonar()
    emit('abandonar')
  } catch (error) {
    uiStore.showToast({ 
      message: error.message || 'Error al abandonar el partido', 
      type: 'error' 
    })
  } finally {
    abandonando.value = false
  }
}

const abrirModalReservaPista = () => {
  precioPistaTotal.value = ''
  mostrarModalReservaPista.value = true
}

const cerrarModalReservaPista = () => {
  mostrarModalReservaPista.value = false
  precioPistaTotal.value = ''
}

const marcarPistaReservada = async () => {
  const precioNormalizado = Number(String(precioPistaTotal.value || '').replace(',', '.'))
  if (!Number.isFinite(precioNormalizado) || precioNormalizado <= 0) {
    uiStore.showToast({
      message: 'Debes indicar un precio válido de pista',
      type: 'error',
    })
    return
  }

  reservandoPista.value = true
  try {
    await partidoService.reservarPista(props.partida.id, precioNormalizado)
    emit('actualizar')
    cerrarModalReservaPista()
    uiStore.showToast({
      message: 'Pista reservada. Se notificó el precio y la parte individual a cada persona.',
      type: 'success',
    })
  } catch (error) {
    uiStore.showToast({
      message: error.message || 'No se pudo marcar la pista como reservada',
      type: 'error',
    })
  } finally {
    reservandoPista.value = false
  }
}
</script>

<style scoped>
.expel-button {
  background-color: #fef2f2 !important;
  border-color: #b91c1c !important;
  color: #b91c1c !important;
}

.expel-button:hover:not(:disabled) {
  background-color: #fee2e2 !important;
  border-color: #991b1b !important;
  color: #991b1b !important;
}
</style>
