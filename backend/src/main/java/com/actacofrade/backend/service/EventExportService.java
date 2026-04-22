package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.ExportRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class EventExportService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final DecisionRepository decisionRepository;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public EventExportService(EventRepository eventRepository, TaskRepository taskRepository,
                               DecisionRepository decisionRepository, IncidentRepository incidentRepository,
                               AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.decisionRepository = decisionRepository;
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public byte[] export(Integer eventId, ExportRequest request, String authenticatedEmail) {
        var user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.getHermandad() == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna hermandad");
        }
        Event event = eventRepository.findByIdAndHermandadId(eventId, user.getHermandad().getId())
                .orElseThrow(() -> new AccessDeniedException("Acto no encontrado o sin acceso"));
        return "CSV".equalsIgnoreCase(request.format()) ? buildCsv(event, request) : buildPdf(event, request);
    }

    private byte[] buildPdf(Event event, ExportRequest request) {
        try {
            var out = new ByteArrayOutputStream();
            var doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            var bold16 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            var bold12 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            var bold10 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            doc.add(new Paragraph(event.getTitle(), bold16));
            doc.add(new Paragraph("Ref: " + event.getReference() + "  |  "
                    + event.getEventType().name() + "  |  "
                    + event.getEventDate().format(DATE_FMT) + "  |  " + event.getStatus().name()));
            for (String section : request.selectedSections()) {
                doc.add(Chunk.NEWLINE);
                switch (section.toUpperCase()) {
                    case "OBSERVATIONS" -> {
                        doc.add(new Paragraph("OBSERVACIONES", bold12));
                        doc.add(new Paragraph(event.getObservations() != null ? event.getObservations() : "—"));
                    }
                    case "TASKS" -> {
                        doc.add(new Paragraph("TAREAS", bold12));
                        var t = new PdfPTable(new float[]{3f, 2f, 1.5f});
                        t.setWidthPercentage(100);
                        Stream.of("Título", "Responsable", "Estado")
                                .forEach(h -> t.addCell(new Phrase(h, bold10)));
                        taskRepository.findByEventId(event.getId()).forEach(task -> {
                            t.addCell(task.getTitle());
                            t.addCell(task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "—");
                            t.addCell(task.getStatus().name());
                        });
                        doc.add(t);
                    }
                    case "DECISIONS" -> {
                        doc.add(new Paragraph("DECISIONES", bold12));
                        var d = new PdfPTable(new float[]{1.5f, 3f, 1.5f});
                        d.setWidthPercentage(100);
                        Stream.of("Área", "Título", "Estado")
                                .forEach(h -> d.addCell(new Phrase(h, bold10)));
                        decisionRepository.findByEventId(event.getId()).forEach(dec -> {
                            d.addCell(dec.getArea().name());
                            d.addCell(dec.getTitle());
                            d.addCell(dec.getStatus().name());
                        });
                        doc.add(d);
                    }
                    case "INCIDENTS" -> {
                        doc.add(new Paragraph("INCIDENCIAS", bold12));
                        var i = new PdfPTable(new float[]{4f, 1.5f});
                        i.setWidthPercentage(100);
                        Stream.of("Descripción", "Estado")
                                .forEach(h -> i.addCell(new Phrase(h, bold10)));
                        incidentRepository.findByEventId(event.getId()).forEach(inc -> {
                            i.addCell(inc.getDescription());
                            i.addCell(inc.getStatus().name());
                        });
                        doc.add(i);
                    }
                    case "HISTORY" -> {
                        doc.add(new Paragraph("HISTORIAL", bold12));
                        var h = new PdfPTable(new float[]{2f, 3f, 2f});
                        h.setWidthPercentage(100);
                        Stream.of("Fecha", "Acción", "Realizado por")
                                .forEach(hdr -> h.addCell(new Phrase(hdr, bold10)));
                        auditLogRepository.findByEventIdOrderByPerformedAtDesc(event.getId(), Pageable.unpaged())
                                .forEach(log -> {
                                    h.addCell(log.getPerformedAt().format(DATETIME_FMT));
                                    h.addCell(log.getAction());
                                    h.addCell(log.getPerformedBy() != null ? log.getPerformedBy().getFullName() : "—");
                                });
                        doc.add(h);
                    }
                    default -> { }
                }
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF", e);
        }
    }

    private byte[] buildCsv(Event event, ExportRequest request) {
        var sb = new StringBuilder();
        sb.append("Acto,Referencia,Tipo,Fecha,Estado\n");
        sb.append(row(event.getTitle(), event.getReference(), event.getEventType().name(),
                event.getEventDate().format(DATE_FMT), event.getStatus().name())).append("\n\n");
        for (String section : request.selectedSections()) {
            switch (section.toUpperCase()) {
                case "OBSERVATIONS" -> {
                    sb.append("OBSERVACIONES\n");
                    sb.append(esc(event.getObservations())).append("\n\n");
                }
                case "TASKS" -> {
                    sb.append("TAREAS\nTítulo,Responsable,Estado\n");
                    taskRepository.findByEventId(event.getId()).forEach(t -> sb.append(
                            row(t.getTitle(), t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : "", t.getStatus().name())).append('\n'));
                    sb.append('\n');
                }
                case "DECISIONS" -> {
                    sb.append("DECISIONES\nÁrea,Título,Estado\n");
                    decisionRepository.findByEventId(event.getId()).forEach(d -> sb.append(
                            row(d.getArea().name(), d.getTitle(), d.getStatus().name())).append('\n'));
                    sb.append('\n');
                }
                case "INCIDENTS" -> {
                    sb.append("INCIDENCIAS\nDescripción,Estado\n");
                    incidentRepository.findByEventId(event.getId()).forEach(i -> sb.append(
                            row(i.getDescription(), i.getStatus().name())).append('\n'));
                    sb.append('\n');
                }
                case "HISTORY" -> {
                    sb.append("HISTORIAL\nFecha,Acción,Realizado por\n");
                    auditLogRepository.findByEventIdOrderByPerformedAtDesc(event.getId(), Pageable.unpaged())
                            .forEach(log -> sb.append(row(
                                    log.getPerformedAt().format(DATETIME_FMT),
                                    log.getAction(),
                                    log.getPerformedBy() != null ? log.getPerformedBy().getFullName() : "")).append('\n'));
                    sb.append('\n');
                }
                default -> { }
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String row(String... values) {
        return Arrays.stream(values).map(this::esc).collect(Collectors.joining(","));
    }

    private String esc(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
