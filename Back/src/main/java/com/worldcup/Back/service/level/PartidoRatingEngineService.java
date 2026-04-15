package com.worldcup.Back.service.level;

import com.worldcup.Back.entity.PartidoCompaneroValoradoEmbeddable;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoIncidenciaEntity;
import com.worldcup.Back.entity.PartidoVotacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.repository.PartidoIncidenciaRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartidoRatingEngineService {

    private static final BigDecimal DEFAULT_MU = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_SIGMA = new BigDecimal("8.33");
    private static final BigDecimal MIN_SIGMA = new BigDecimal("2.00");
    private static final BigDecimal MAX_SIGMA = new BigDecimal("8.33");
    private static final BigDecimal BASE_K = new BigDecimal("2.40");
    private static final BigDecimal SIGMA_DECAY_FACTOR = new BigDecimal("0.95");

    @Autowired
    private PartidoVotacionRepository partidoVotacionRepository;

    @Autowired
    private PartidoIncidenciaRepository partidoIncidenciaRepository;

    public EngineResult procesar(PartidoEntity partido) {
        List<PartidoVotacionEntity> votos = partidoVotacionRepository.findByPartido(partido);
        List<VoteSnapshot> snapshots = construirSnapshotsDeVotos(votos);
        OutlierResult outlierResult = filtrarOutliers(snapshots);

        ResultadoPartido resultado = resolverResultado(partido, outlierResult.votosFiltrados());
        if (resultado == ResultadoPartido.SIN_DATOS) {
            return new EngineResult(resultado.name(), 0, outlierResult.votosFiltrados().size(), outlierResult.votosAtipicos(), outlierResult.usuariosImpactados());
        }

        List<UsuarioEntity> equipoA = partido.getEquipoA() == null ? List.of() : partido.getEquipoA();
        List<UsuarioEntity> equipoB = partido.getEquipoB() == null ? List.of() : partido.getEquipoB();

        if (equipoA.isEmpty() || equipoB.isEmpty()) {
            return new EngineResult(ResultadoPartido.SIN_EQUIPOS.name(), 0, outlierResult.votosFiltrados().size(), outlierResult.votosAtipicos(), outlierResult.usuariosImpactados());
        }

        BigDecimal nivelVisibleA = sumarNivelVisible(equipoA);
        BigDecimal nivelVisibleB = sumarNivelVisible(equipoB);

        double expectedA = expectedScore(nivelVisibleA, nivelVisibleB);
        double expectedB = 1.0 - expectedA;

        double scoreA = switch (resultado) {
            case GANA_A -> 1.0;
            case GANA_B -> 0.0;
            case EMPATE -> 0.5;
            default -> 0.0;
        };
        double scoreB = 1.0 - scoreA;

        BigDecimal wCalidad = pesoPorEstadoCalidad(partido.getEstadoCalidad(), partido.getScoreCalidad());
        BigDecimal wParticipacion = calcularPesoParticipacion(partido, outlierResult.votosFiltrados().size());
        BigDecimal wModo = "AUTO".equalsIgnoreCase(partido.getModoEquipos()) ? BigDecimal.ONE : new BigDecimal("0.75");
        BigDecimal wIncidencias = calcularPesoIncidencias(partido);
        BigDecimal wIntensidad = calcularPesoIntensidadContextual(partido, outlierResult.votosFiltrados(), resultado, expectedA, expectedB);
        BigDecimal wEquilibrio = calcularPesoEquilibrioVotado(outlierResult.votosFiltrados());
        BigDecimal wMarcador = calcularPesoMarcadorPercibido(outlierResult.votosFiltrados());

        BigDecimal factorGlobal = wCalidad
            .multiply(wParticipacion)
            .multiply(wModo)
            .multiply(wIncidencias)
            .multiply(wIntensidad)
            .multiply(wEquilibrio)
            .multiply(wMarcador);
        Map<Long, Double> senialSocial = calcularSenialSocial(partido, outlierResult.votosFiltrados());

        List<UsuarioEntity> actualizados = new ArrayList<>();
        for (UsuarioEntity jugador : equipoA) {
            double social = senialSocial.getOrDefault(jugador.getId(), 0.0d);
            actualizarJugador(jugador, scoreA, expectedA, factorGlobal, resultado == ResultadoPartido.EMPATE, social);
            actualizados.add(jugador);
        }

        for (UsuarioEntity jugador : equipoB) {
            double social = senialSocial.getOrDefault(jugador.getId(), 0.0d);
            actualizarJugador(jugador, scoreB, expectedB, factorGlobal, resultado == ResultadoPartido.EMPATE, social);
            actualizados.add(jugador);
        }

        Map<Long, UsuarioEntity> deduplicados = new HashMap<>();
        for (UsuarioEntity usuario : actualizados) {
            if (usuario != null && usuario.getId() != null) {
                deduplicados.put(usuario.getId(), usuario);
            }
        }
        for (UsuarioEntity usuario : outlierResult.usuariosImpactados()) {
            if (usuario != null && usuario.getId() != null) {
                deduplicados.put(usuario.getId(), usuario);
            }
        }

        return new EngineResult(
                resultado.name(),
                actualizados.size(),
                outlierResult.votosFiltrados().size(),
                outlierResult.votosAtipicos(),
                new ArrayList<>(deduplicados.values())
        );
    }

    private void actualizarJugador(UsuarioEntity jugador,
                                   double score,
                                   double expected,
                                   BigDecimal factorGlobal,
                                   boolean empate,
                                   double socialSignal) {
        BigDecimal mu = jugador.getRatingMu() == null ? DEFAULT_MU : jugador.getRatingMu();
        BigDecimal sigma = jugador.getRatingSigma() == null ? DEFAULT_SIGMA : jugador.getRatingSigma();

        BigDecimal sigmaUncertaintyFactor = sigma.divide(DEFAULT_SIGMA, 4, RoundingMode.HALF_UP);
        sigmaUncertaintyFactor = normalizarRango(sigmaUncertaintyFactor, new BigDecimal("0.60"), new BigDecimal("1.40"));

        BigDecimal delta = BASE_K
                .multiply(sigmaUncertaintyFactor)
                .multiply(BigDecimal.valueOf(score - expected))
                .multiply(factorGlobal)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal deltaSocial = BigDecimal.valueOf(socialSignal)
            .multiply(new BigDecimal("0.80"))
            .multiply(factorGlobal)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal deltaFinal = delta.add(deltaSocial).setScale(4, RoundingMode.HALF_UP);

        BigDecimal nuevoMu = mu.add(deltaFinal).setScale(2, RoundingMode.HALF_UP);
        if (nuevoMu.signum() < 0) {
            nuevoMu = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sigmaFactor = SIGMA_DECAY_FACTOR;
        if (factorGlobal.compareTo(new BigDecimal("0.55")) < 0) {
            sigmaFactor = new BigDecimal("0.985");
        }
        if (empate && factorGlobal.compareTo(new BigDecimal("0.75")) < 0) {
            sigmaFactor = new BigDecimal("0.995");
        }
        BigDecimal nuevaSigma = sigma.multiply(sigmaFactor).setScale(2, RoundingMode.HALF_UP);
        nuevaSigma = normalizarRango(nuevaSigma, MIN_SIGMA, MAX_SIGMA);

        jugador.setRatingMu(nuevoMu);
        jugador.setRatingSigma(nuevaSigma);
        jugador.setRatingVersion("trueskill-adapted-v1");

        jugador.setPartidosJugados((jugador.getPartidosJugados() == null ? 0 : jugador.getPartidosJugados()) + 1);
        if (empate) {
            jugador.setEmpates((jugador.getEmpates() == null ? 0 : jugador.getEmpates()) + 1);
        } else if (score > expected) {
            jugador.setVictorias((jugador.getVictorias() == null ? 0 : jugador.getVictorias()) + 1);
        } else {
            jugador.setDerrotas((jugador.getDerrotas() == null ? 0 : jugador.getDerrotas()) + 1);
        }
    }

    private ResultadoPartido resolverResultado(PartidoEntity partido, List<VoteSnapshot> votosFiltrados) {
        if ("EQUIPO_A".equalsIgnoreCase(partido.getGanador())) {
            return ResultadoPartido.GANA_A;
        }
        if ("EQUIPO_B".equalsIgnoreCase(partido.getGanador())) {
            return ResultadoPartido.GANA_B;
        }
        if ("EMPATE".equalsIgnoreCase(partido.getGanador())) {
            return ResultadoPartido.EMPATE;
        }

        if (partido.getGolesEquipoA() != null && partido.getGolesEquipoB() != null) {
            if (partido.getGolesEquipoA() > partido.getGolesEquipoB()) {
                return ResultadoPartido.GANA_A;
            }
            if (partido.getGolesEquipoB() > partido.getGolesEquipoA()) {
                return ResultadoPartido.GANA_B;
            }
            return ResultadoPartido.EMPATE;
        }

        if (votosFiltrados == null || votosFiltrados.isEmpty()) {
            return ResultadoPartido.SIN_DATOS;
        }

        double sumaPeso = votosFiltrados.stream().mapToDouble(VoteSnapshot::peso).sum();
        if (sumaPeso <= 0.0001) {
            return ResultadoPartido.SIN_DATOS;
        }

        double promedioA = votosFiltrados.stream()
                .mapToDouble(v -> v.voto().getGolesEquipoAPropuesto() * v.peso())
                .sum() / sumaPeso;
        double promedioB = votosFiltrados.stream()
                .mapToDouble(v -> v.voto().getGolesEquipoBPropuesto() * v.peso())
                .sum() / sumaPeso;

        if (promedioA > promedioB) {
            return ResultadoPartido.GANA_A;
        }
        if (promedioB > promedioA) {
            return ResultadoPartido.GANA_B;
        }
        return ResultadoPartido.EMPATE;
    }

    private BigDecimal sumarNivelVisible(List<UsuarioEntity> equipo) {
        return equipo.stream()
                .map(u -> {
                    BigDecimal mu = u.getRatingMu() == null ? DEFAULT_MU : u.getRatingMu();
                    BigDecimal sigma = u.getRatingSigma() == null ? DEFAULT_SIGMA : u.getRatingSigma();
                    BigDecimal visible = mu.subtract(sigma.multiply(new BigDecimal("3.00")));
                    return visible.max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double expectedScore(BigDecimal muA, BigDecimal muB) {
        double diff = muB.subtract(muA).doubleValue();
        return 1.0 / (1.0 + Math.exp(diff / 10.0));
    }

    private BigDecimal calcularPesoParticipacion(PartidoEntity partido, int votosValidos) {
        int participantes = Math.max(1, partido.getTotalJugadoresEnEquipos() != null && partido.getTotalJugadoresEnEquipos() > 0
                ? partido.getTotalJugadoresEnEquipos()
                : partido.getTotalJugadores());

        BigDecimal participacionCalculada = BigDecimal.valueOf(votosValidos)
                .divide(BigDecimal.valueOf(participantes), 3, RoundingMode.HALF_UP);
        BigDecimal participacion = participacionCalculada;
        if (partido.getParticipacionVotacion() != null && partido.getParticipacionVotacion().compareTo(participacionCalculada) > 0) {
            participacion = partido.getParticipacionVotacion();
        }

        if (participacion.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.45");
        }
        if (participacion.compareTo(new BigDecimal("0.30")) < 0) {
            return new BigDecimal("0.50");
        }
        if (participacion.compareTo(new BigDecimal("0.70")) < 0) {
            return new BigDecimal("0.75");
        }
        return BigDecimal.ONE;
    }

    private BigDecimal calcularPesoEquilibrioVotado(List<VoteSnapshot> votosFiltrados) {
        if (votosFiltrados == null || votosFiltrados.isEmpty()) {
            return new BigDecimal("0.90");
        }

        long votosParejo = votosFiltrados.stream()
                .filter(v -> Boolean.TRUE.equals(v.voto().getPartidoFueParejo()))
                .count();
        double ratioParejo = votosParejo / (double) votosFiltrados.size();

        if (ratioParejo >= 0.65) {
            return BigDecimal.ONE;
        }
        if (ratioParejo >= 0.45) {
            return new BigDecimal("0.88");
        }
        return new BigDecimal("0.75");
    }

    private BigDecimal calcularPesoMarcadorPercibido(List<VoteSnapshot> votosFiltrados) {
        if (votosFiltrados == null || votosFiltrados.isEmpty()) {
            return new BigDecimal("0.90");
        }

        double promedioA = votosFiltrados.stream().mapToDouble(v -> v.voto().getGolesEquipoAPropuesto()).average().orElse(0.0);
        double promedioB = votosFiltrados.stream().mapToDouble(v -> v.voto().getGolesEquipoBPropuesto()).average().orElse(0.0);
        double diferencia = Math.abs(promedioA - promedioB);
        double total = promedioA + promedioB;

        BigDecimal peso = BigDecimal.ONE;
        if (diferencia >= 6.0) {
            peso = peso.multiply(new BigDecimal("0.82"));
        } else if (diferencia >= 3.0) {
            peso = peso.multiply(new BigDecimal("0.92"));
        }

        if (total >= 12.0 || total <= 4.0) {
            peso = peso.multiply(new BigDecimal("0.92"));
        }

        return normalizarRango(peso, new BigDecimal("0.70"), BigDecimal.ONE);
    }

    private BigDecimal calcularPesoIntensidadContextual(PartidoEntity partido,
                                                        List<VoteSnapshot> votosFiltrados,
                                                        ResultadoPartido resultado,
                                                        double expectedA,
                                                        double expectedB) {
        if (votosFiltrados == null || votosFiltrados.isEmpty()) {
            return new BigDecimal("0.90");
        }

        Set<Long> equipoAIds = partido.getEquipoA() == null
                ? Set.of()
                : partido.getEquipoA().stream().map(UsuarioEntity::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> equipoBIds = partido.getEquipoB() == null
                ? Set.of()
                : partido.getEquipoB().stream().map(UsuarioEntity::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        double avgA = promedioIntensidadPorEquipo(votosFiltrados, equipoAIds);
        double avgB = promedioIntensidadPorEquipo(votosFiltrados, equipoBIds);
        double avgGlobal = votosFiltrados.stream().mapToDouble(v -> intensidadToDouble(v.voto().getIntensidadPartido())).average().orElse(3.0);

        double promedioGolesA = votosFiltrados.stream().mapToDouble(v -> v.voto().getGolesEquipoAPropuesto()).average().orElse(0.0);
        double promedioGolesB = votosFiltrados.stream().mapToDouble(v -> v.voto().getGolesEquipoBPropuesto()).average().orElse(0.0);
        boolean paliza = Math.abs(promedioGolesA - promedioGolesB) >= 6.0;

        double intensidadReferencia;
        boolean upsetA = resultado == ResultadoPartido.GANA_A && expectedA < expectedB;
        boolean upsetB = resultado == ResultadoPartido.GANA_B && expectedB < expectedA;

        if (resultado == ResultadoPartido.EMPATE) {
            intensidadReferencia = avgGlobal;
        } else if (paliza) {
            intensidadReferencia = resultado == ResultadoPartido.GANA_A ? avgB : avgA;
        } else if (upsetA) {
            intensidadReferencia = avgA;
        } else if (upsetB) {
            intensidadReferencia = avgB;
        } else {
            intensidadReferencia = avgGlobal;
        }

        return seleccionarPesoPorIntensidad(intensidadReferencia);
    }

    private double promedioIntensidadPorEquipo(List<VoteSnapshot> votosFiltrados, Set<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return votosFiltrados.stream().mapToDouble(v -> intensidadToDouble(v.voto().getIntensidadPartido())).average().orElse(3.0);
        }

        double promedio = votosFiltrados.stream()
                .filter(v -> v.voto().getVotante() != null && teamIds.contains(v.voto().getVotante().getId()))
                .mapToDouble(v -> intensidadToDouble(v.voto().getIntensidadPartido()))
                .average()
                .orElse(Double.NaN);

        if (Double.isNaN(promedio)) {
            return votosFiltrados.stream().mapToDouble(v -> intensidadToDouble(v.voto().getIntensidadPartido())).average().orElse(3.0);
        }
        return promedio;
    }

    private double intensidadToDouble(String intensidad) {
        if (intensidad == null) {
            return 3.0;
        }
        return switch (intensidad.trim().toUpperCase()) {
            case "BAJO" -> 2.0;
            case "ALTO" -> 4.0;
            case "MUY_ALTO" -> 5.0;
            case "MEDIO", "MEDIA" -> 3.0;
            default -> 3.0;
        };
    }

    private BigDecimal seleccionarPesoPorIntensidad(double intensidadReferencia) {
        if (intensidadReferencia >= 4.5) {
            return new BigDecimal("1.15");
        }
        if (intensidadReferencia >= 3.5) {
            return new BigDecimal("1.05");
        }
        if (intensidadReferencia >= 2.5) {
            return BigDecimal.ONE;
        }
        if (intensidadReferencia >= 1.5) {
            return new BigDecimal("0.88");
        }
        return new BigDecimal("0.78");
    }

    private List<VoteSnapshot> construirSnapshotsDeVotos(List<PartidoVotacionEntity> votos) {
        if (votos == null || votos.isEmpty()) {
            return List.of();
        }

        List<VoteSnapshot> snapshots = new ArrayList<>();
        for (PartidoVotacionEntity voto : votos) {
            UsuarioEntity votante = voto.getVotante();
            if (votante == null) {
                continue;
            }

            double fiabilidad = normalizarFiabilidad01(votante.getFiabilidadScore());
            double peso = 0.40 + fiabilidad * 0.60;

            votante.setVotosEmitidos((votante.getVotosEmitidos() == null ? 0 : votante.getVotosEmitidos()) + 1);
            snapshots.add(new VoteSnapshot(voto, peso));
        }
        return snapshots;
    }

    private OutlierResult filtrarOutliers(List<VoteSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return new OutlierResult(List.of(), 0, List.of());
        }

        if (snapshots.size() < 4) {
            List<UsuarioEntity> usuariosImpactados = marcarVotosValidos(snapshots);
            return new OutlierResult(snapshots, 0, usuariosImpactados);
        }

        double sumaPesos = snapshots.stream().mapToDouble(VoteSnapshot::peso).sum();
        if (sumaPesos <= 0.0001) {
            List<UsuarioEntity> usuariosImpactados = marcarVotosValidos(snapshots);
            return new OutlierResult(snapshots, 0, usuariosImpactados);
        }

        double mediaMargen = snapshots.stream()
                .mapToDouble(s -> (s.voto().getGolesEquipoAPropuesto() - s.voto().getGolesEquipoBPropuesto()) * s.peso())
                .sum() / sumaPesos;

        double varianza = snapshots.stream()
                .mapToDouble(s -> {
                    double margen = s.voto().getGolesEquipoAPropuesto() - s.voto().getGolesEquipoBPropuesto();
                    double diff = margen - mediaMargen;
                    return diff * diff * s.peso();
                })
                .sum() / sumaPesos;

        double desviacion = Math.sqrt(Math.max(0.0001, varianza));
        double umbral = Math.max(2.0, desviacion * 1.50);

        List<VoteSnapshot> validos = new ArrayList<>();
        List<UsuarioEntity> usuariosImpactados = new ArrayList<>();
        int atipicos = 0;

        for (VoteSnapshot snapshot : snapshots) {
            double margen = snapshot.voto().getGolesEquipoAPropuesto() - snapshot.voto().getGolesEquipoBPropuesto();
            boolean esAtipico = Math.abs(margen - mediaMargen) > umbral;

            UsuarioEntity votante = snapshot.voto().getVotante();
            if (votante != null) {
                if (esAtipico) {
                    votante.setVotosAtipicos((votante.getVotosAtipicos() == null ? 0 : votante.getVotosAtipicos()) + 1);
                    ajustarFiabilidad(votante, -0.04);
                    atipicos++;
                } else {
                    votante.setVotosValidos((votante.getVotosValidos() == null ? 0 : votante.getVotosValidos()) + 1);
                    ajustarFiabilidad(votante, +0.02);
                    validos.add(snapshot);
                }
                usuariosImpactados.add(votante);
            }
        }

        if (validos.isEmpty()) {
            validos.addAll(snapshots);
            atipicos = 0;
            usuariosImpactados = marcarVotosValidos(snapshots);
        }

        return new OutlierResult(validos, atipicos, usuariosImpactados);
    }

    private List<UsuarioEntity> marcarVotosValidos(List<VoteSnapshot> snapshots) {
        List<UsuarioEntity> usuarios = new ArrayList<>();
        for (VoteSnapshot snapshot : snapshots) {
            UsuarioEntity votante = snapshot.voto().getVotante();
            if (votante == null) {
                continue;
            }
            votante.setVotosValidos((votante.getVotosValidos() == null ? 0 : votante.getVotosValidos()) + 1);
            ajustarFiabilidad(votante, +0.01);
            usuarios.add(votante);
        }
        return usuarios;
    }

    private void ajustarFiabilidad(UsuarioEntity votante, double delta) {
        BigDecimal actual = votante.getFiabilidadScore() == null ? new BigDecimal("1.00") : votante.getFiabilidadScore();
        if (actual.compareTo(BigDecimal.ONE) > 0) {
            actual = actual.divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP);
        }
        BigDecimal siguiente = actual.add(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.HALF_UP);
        if (siguiente.compareTo(BigDecimal.ZERO) < 0) {
            siguiente = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (siguiente.compareTo(BigDecimal.ONE) > 0) {
            siguiente = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        votante.setFiabilidadScore(siguiente);
    }

    private double normalizarFiabilidad01(BigDecimal score) {
        if (score == null) {
            return 1.0;
        }
        double value = score.doubleValue();
        if (value > 1.0) {
            value = value / 100.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private Map<Long, Double> calcularSenialSocial(PartidoEntity partido, List<VoteSnapshot> votosFiltrados) {
        if (votosFiltrados == null || votosFiltrados.isEmpty()) {
            return Map.of();
        }

        double pesoTotal = votosFiltrados.stream().mapToDouble(VoteSnapshot::peso).sum();
        if (pesoTotal <= 0.0001) {
            return Map.of();
        }

        int participantes = Math.max(1, partido.getTotalJugadoresEnEquipos() != null && partido.getTotalJugadoresEnEquipos() > 0
                ? partido.getTotalJugadoresEnEquipos()
                : partido.getTotalJugadores());
        double participacion = votosFiltrados.size() / (double) participantes;

        BigDecimal factorParticipacionDiferenciales;
        if (votosFiltrados.size() <= 1) {
            factorParticipacionDiferenciales = BigDecimal.ZERO;
        } else if (participacion >= 0.70) {
            factorParticipacionDiferenciales = BigDecimal.ONE;
        } else if (participacion >= 0.30) {
            factorParticipacionDiferenciales = new BigDecimal("0.60");
        } else {
            factorParticipacionDiferenciales = new BigDecimal("0.25");
        }

        Set<Long> equipoAIds = partido.getEquipoA() == null
                ? Set.of()
                : partido.getEquipoA().stream().map(UsuarioEntity::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> equipoBIds = partido.getEquipoB() == null
                ? Set.of()
                : partido.getEquipoB().stream().map(UsuarioEntity::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Integer> votosDifDesdeA = new HashMap<>();
        Map<Long, Integer> votosDifDesdeB = new HashMap<>();
        Map<Long, Integer> votosDifTotales = new HashMap<>();

        Map<Long, Double> acumulado = new HashMap<>();
        for (VoteSnapshot snapshot : votosFiltrados) {
            PartidoVotacionEntity voto = snapshot.voto();
            double pesoNormalizado = snapshot.peso() / pesoTotal;
            Long votanteId = voto.getVotante() == null ? null : voto.getVotante().getId();
            boolean votanteEnA = votanteId != null && equipoAIds.contains(votanteId);
            boolean votanteEnB = votanteId != null && equipoBIds.contains(votanteId);

            List<Long> diferenciales = voto.getJugadoresDiferenciales() == null ? List.of() : voto.getJugadoresDiferenciales();
            for (Long jugadorId : diferenciales.stream().filter(Objects::nonNull).distinct().toList()) {
                votosDifTotales.merge(jugadorId, 1, Integer::sum);
                if (votanteEnA) {
                    votosDifDesdeA.merge(jugadorId, 1, Integer::sum);
                }
                if (votanteEnB) {
                    votosDifDesdeB.merge(jugadorId, 1, Integer::sum);
                }
            }

            List<PartidoCompaneroValoradoEmbeddable> valoraciones = voto.getValoracionesCompaneros() == null ? List.of() : voto.getValoracionesCompaneros();
            for (PartidoCompaneroValoradoEmbeddable valoracion : valoraciones) {
                if (valoracion == null || valoracion.getJugadorId() == null || valoracion.getPuntuacion() == null) {
                    continue;
                }
                int puntuacion = Math.max(-1, Math.min(1, valoracion.getPuntuacion()));
                acumulado.merge(valoracion.getJugadorId(), 0.04 * puntuacion * pesoNormalizado, Double::sum);
            }
        }

        if (factorParticipacionDiferenciales.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<Long, Integer> entry : votosDifTotales.entrySet()) {
                Long jugadorId = entry.getKey();
                int totalVotosJugador = entry.getValue();
                if (totalVotosJugador <= 0) {
                    continue;
                }

                int desdeA = votosDifDesdeA.getOrDefault(jugadorId, 0);
                int desdeB = votosDifDesdeB.getOrDefault(jugadorId, 0);
                boolean hayCruceEquipos = desdeA > 0 && desdeB > 0;
                BigDecimal factorOrigen = hayCruceEquipos ? BigDecimal.ONE : new BigDecimal("0.60");

                double fuerza = 0.08
                        * (totalVotosJugador / (double) votosFiltrados.size())
                        * factorParticipacionDiferenciales.multiply(factorOrigen).doubleValue();
                acumulado.merge(jugadorId, fuerza, Double::sum);
            }
        }

        return acumulado.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.max(-0.18, Math.min(0.18, e.getValue()))
                ));
    }

    private BigDecimal calcularPesoIncidencias(PartidoEntity partido) {
        List<PartidoIncidenciaEntity> incidencias = partidoIncidenciaRepository.findByPartidoOrderByCreadaEnDesc(partido);
        if (incidencias.isEmpty()) {
            return BigDecimal.ONE;
        }

        double impacto = incidencias.stream()
                .mapToDouble(i -> {
                    double severidad = i.getSeveridad() == null ? 2.0 : i.getSeveridad();
                    return Math.max(1.0, Math.min(3.0, severidad));
                })
                .sum();

        double factor = 1.0 / (1.0 + (impacto * 0.08));
        return normalizarRango(BigDecimal.valueOf(factor), new BigDecimal("0.50"), BigDecimal.ONE);
    }

    private BigDecimal pesoPorEstadoCalidad(String estadoCalidad, BigDecimal scoreCalidad) {
        String estado = estadoCalidad == null ? "NORMAL" : estadoCalidad.trim().toUpperCase();
        BigDecimal score = scoreCalidad == null ? BigDecimal.ONE : scoreCalidad;

        return switch (estado) {
            case "ALTERADO" -> normalizarRango(score.multiply(new BigDecimal("0.20")), new BigDecimal("0.10"), new BigDecimal("0.20"));
            case "SEMI_ALTERADO" -> normalizarRango(score.multiply(new BigDecimal("0.60")), new BigDecimal("0.35"), new BigDecimal("0.70"));
            default -> normalizarRango(score, new BigDecimal("0.70"), BigDecimal.ONE);
        };
    }

    private BigDecimal normalizarRango(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    private enum ResultadoPartido {
        GANA_A,
        GANA_B,
        EMPATE,
        SIN_DATOS,
        SIN_EQUIPOS
    }

    private record VoteSnapshot(PartidoVotacionEntity voto, double peso) {
    }

    private record OutlierResult(List<VoteSnapshot> votosFiltrados, int votosAtipicos, List<UsuarioEntity> usuariosImpactados) {
    }

    public record EngineResult(String resultadoResolucion,
                               int jugadoresActualizados,
                               int votosConsiderados,
                               int votosAtipicos,
                               List<UsuarioEntity> usuariosActualizados) {
    }
}
