package com.actacofrade.backend.service;

import com.actacofrade.backend.dto.ExportRequest;
import com.actacofrade.backend.entity.Event;
import com.actacofrade.backend.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EventExportService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color GOLD = new Color(0xC9, 0xA9, 0x61);
    private static final Color INK = new Color(0x0D, 0x0D, 0x0D);
    private static final Color LINE = new Color(0xE8, 0xE8, 0xE8);
    private static final Color CREAM = new Color(0xF5, 0xF3, 0xF0);
    private static final Color MUTED = new Color(0x73, 0x68, 0x56);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INK);
    private static final Font META_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, INK);
    private static final Font EMPTY_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font OBSERVATIONS_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);

    private static final char CSV_DELIMITER = ';';
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static String eventTypeLabel(String raw) {
        return switch (raw.toUpperCase()) {
            case "CABILDO_GENERAL_ORDINARIO" -> "Cabildo General Ordinario";
            case "CABILDO_GENERAL_EXTRAORDINARIO" -> "Cabildo General Extraordinario";
            case "JUNTA_DE_GOBIERNO" -> "Junta de Gobierno";
            case "ASAMBLEA" -> "Asamblea";
            case "COMISION" -> "Comisión";
            default -> raw;
        };
    }

    private static String eventStatusLabel(String raw) {
        return switch (raw.toUpperCase()) {
            case "PLANIFICACION" -> "Planificación";
            case "PREPARACION" -> "Preparación";
            case "CONFIRMACION" -> "Confirmación";
            case "CERRADO" -> "Cerrado";
            default -> raw;
        };
    }

    private static String taskStatusLabel(String raw) {
        return switch (raw.toUpperCase()) {
            case "PLANNED" -> "Planificada";
            case "ACCEPTED" -> "Aceptada";
            case "IN_PREPARATION" -> "En preparación";
            case "CONFIRMED" -> "Confirmada";
            case "COMPLETED" -> "Completada";
            case "REJECTED" -> "Rechazada";
            default -> raw;
        };
    }

    private static String decisionStatusLabel(String raw) {
        return switch (raw.toUpperCase()) {
            case "PENDING" -> "Pendiente";
            case "ACCEPTED" -> "Aprobada";
            case "REJECTED" -> "Rechazada";
            default -> raw;
        };
    }

    private static String incidentStatusLabel(String raw) {
        return switch (raw.toUpperCase()) {
            case "ABIERTA" -> "Abierta";
            case "RESUELTA" -> "Resuelta";
            default -> raw;
        };
    }

    private static String areaLabel(String raw) {
        return switch (raw.toUpperCase()) {
            case "ECONOMICA" -> "Económica";
            case "LITURGICA" -> "Litúrgica";
            case "ORGANIZACION" -> "Organización";
            case "PATRIMONIO" -> "Patrimonio";
            case "COMUNICACION" -> "Comunicación";
            default -> raw;
        };
    }

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final DecisionRepository decisionRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public EventExportService(EventRepository eventRepository, TaskRepository taskRepository,
                               DecisionRepository decisionRepository, IncidentRepository incidentRepository,
                               UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.decisionRepository = decisionRepository;
        this.incidentRepository = incidentRepository;
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
        Set<String> sections = normalizeSections(request.selectedSections());
        return "CSV".equalsIgnoreCase(request.format()) ? buildCsv(event, sections) : buildPdf(event, sections);
    }

    private Set<String> normalizeSections(List<String> raw) {
        Set<String> set = new LinkedHashSet<>();
        for (String s : raw) {
            if (s != null && !s.isBlank()) {
                set.add(s.trim().toUpperCase());
            }
        }
        return set;
    }

    private byte[] buildPdf(Event event, Set<String> sections) {
        try {
            var out = new ByteArrayOutputStream();
            var doc = new Document(PageSize.A4, 48f, 48f, 56f, 56f);
            var writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PdfFooter());
            doc.open();

            doc.add(buildPdfHeader(event));

            for (String section : sections) {
                doc.add(Chunk.NEWLINE);
                switch (section) {
                    case "OBSERVATIONS" -> renderObservations(doc, event);
                    case "TASKS" -> renderTasks(doc, event);
                    case "DECISIONS" -> renderDecisions(doc, event);
                    case "INCIDENTS" -> renderIncidents(doc, event);
                    default -> { }
                }
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando el PDF", e);
        }
    }

    private PdfPTable buildPdfHeader(Event event) {
        var table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8f);

        var title = new PdfPCell(new Phrase(event.getTitle().toUpperCase(), TITLE_FONT));
        title.setBorder(Rectangle.NO_BORDER);
        title.setPaddingBottom(4f);
        table.addCell(title);

        String meta = "Ref. " + event.getReference()
                + "  ·  " + eventTypeLabel(event.getEventType().name())
                + "  ·  " + event.getEventDate().format(DATE_FMT)
                + "  ·  " + eventStatusLabel(event.getStatus().name());
        var metaCell = new PdfPCell(new Phrase(meta, META_FONT));
        metaCell.setBorder(Rectangle.BOTTOM);
        metaCell.setBorderColor(GOLD);
        metaCell.setBorderWidthBottom(1.2f);
        metaCell.setPaddingBottom(8f);
        table.addCell(metaCell);

        return table;
    }

    private void renderObservations(Document doc, Event event) throws DocumentException {
        addSectionTitle(doc, "OBSERVACIONES");
        String text = event.getObservations() == null || event.getObservations().isBlank()
                ? "Sin observaciones registradas."
                : event.getObservations();
        var p = new Paragraph(text, OBSERVATIONS_FONT);
        p.setSpacingBefore(2f);
        p.setSpacingAfter(8f);
        doc.add(p);
    }

    private void renderTasks(Document doc, Event event) throws DocumentException {
        addSectionTitle(doc, "TAREAS");
        var rows = taskRepository.findByEventId(event.getId());
        var table = createTable(new float[]{4f, 2.4f, 1.8f, 1.8f},
                "Título", "Responsable", "Estado", "Fecha límite");
        if (rows.isEmpty()) {
            addEmptyRow(table, 4);
        } else {
            for (var t : rows) {
                addBodyCell(table, t.getTitle());
                addBodyCell(table, t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : "—");
                addBodyCell(table, taskStatusLabel(t.getStatus().name()));
                addBodyCell(table, t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : "—");
            }
        }
        doc.add(table);
    }

    private void renderDecisions(Document doc, Event event) throws DocumentException {
        addSectionTitle(doc, "DECISIONES");
        var rows = decisionRepository.findByEventId(event.getId());
        var table = createTable(new float[]{1.8f, 4f, 1.8f}, "Área", "Título", "Estado");
        if (rows.isEmpty()) {
            addEmptyRow(table, 3);
        } else {
            for (var d : rows) {
                addBodyCell(table, areaLabel(d.getArea().name()));
                addBodyCell(table, d.getTitle());
                addBodyCell(table, decisionStatusLabel(d.getStatus().name()));
            }
        }
        doc.add(table);
    }

    private void renderIncidents(Document doc, Event event) throws DocumentException {
        addSectionTitle(doc, "INCIDENCIAS");
        var rows = incidentRepository.findByEventId(event.getId());
        var table = createTable(new float[]{4.5f, 1.8f}, "Descripción", "Estado");
        if (rows.isEmpty()) {
            addEmptyRow(table, 2);
        } else {
            for (var i : rows) {
                addBodyCell(table, i.getDescription());
                addBodyCell(table, incidentStatusLabel(i.getStatus().name()));
            }
        }
        doc.add(table);
    }



    private void addSectionTitle(Document doc, String label) throws DocumentException {
        var bar = new PdfPTable(1);
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(4f);
        bar.setSpacingAfter(4f);
        var cell = new PdfPCell(new Phrase(label, SECTION_FONT));
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColor(GOLD);
        cell.setBorderWidthLeft(3f);
        cell.setBackgroundColor(CREAM);
        cell.setPadding(6f);
        bar.addCell(cell);
        doc.add(bar);
    }

    private PdfPTable createTable(float[] widths, String... headers) {
        var table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        table.setSpacingAfter(8f);
        table.setHeaderRows(1);
        for (String h : headers) {
            var cell = new PdfPCell(new Phrase(h.toUpperCase(), HEADER_FONT));
            cell.setBackgroundColor(INK);
            cell.setBorderColor(INK);
            cell.setPadding(6f);
            table.addCell(cell);
        }
        return table;
    }

    private void addBodyCell(PdfPTable table, String text) {
        var cell = new PdfPCell(new Phrase(text == null ? "—" : text, CELL_FONT));
        cell.setBorderColor(LINE);
        cell.setPadding(5f);
        int rowIndex = (table.size() - 1) / table.getNumberOfColumns();
        if (rowIndex % 2 == 1) {
            cell.setBackgroundColor(CREAM);
        }
        table.addCell(cell);
    }

    private void addEmptyRow(PdfPTable table, int colspan) {
        var cell = new PdfPCell(new Phrase("Sin registros para esta sección.", EMPTY_FONT));
        cell.setColspan(colspan);
        cell.setBorderColor(LINE);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static class PdfFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String left = "ActaCofrade  ·  Generado " + LocalDateTime.now().format(DATETIME_FMT);
            String right = "Página " + writer.getPageNumber();
            float y = document.bottom() - 18f;
            var cb = writer.getDirectContent();
            cb.setColorStroke(GOLD);
            cb.setLineWidth(0.6f);
            cb.moveTo(document.left(), y + 10f);
            cb.lineTo(document.right(), y + 10f);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(left, FOOTER_FONT), document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(right, FOOTER_FONT), document.right(), y, 0);
        }
    }

    private byte[] buildCsv(Event event, Set<String> sections) {
        var sb = new StringBuilder();
        appendRow(sb, "Acto", "Referencia", "Tipo", "Fecha", "Estado");
        appendRow(sb, event.getTitle(), event.getReference(),
                eventTypeLabel(event.getEventType().name()),
                event.getEventDate().format(DATE_FMT),
                eventStatusLabel(event.getStatus().name()));
        sb.append("\r\n");

        for (String section : sections) {
            switch (section) {
                case "OBSERVATIONS" -> {
                    appendRow(sb, "OBSERVACIONES");
                    appendRow(sb, event.getObservations() == null || event.getObservations().isBlank()
                            ? "Sin observaciones registradas."
                            : event.getObservations());
                    sb.append("\r\n");
                }
                case "TASKS" -> {
                    appendRow(sb, "TAREAS");
                    appendRow(sb, "Título", "Responsable", "Estado", "Fecha límite");
                    var rows = taskRepository.findByEventId(event.getId());
                    if (rows.isEmpty()) {
                        appendRow(sb, "Sin registros para esta sección.");
                    } else {
                        rows.forEach(t -> appendRow(sb,
                                t.getTitle(),
                                t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : "",
                                taskStatusLabel(t.getStatus().name()),
                                t.getDeadline() != null ? t.getDeadline().format(DATE_FMT) : ""));
                    }
                    sb.append("\r\n");
                }
                case "DECISIONS" -> {
                    appendRow(sb, "DECISIONES");
                    appendRow(sb, "Área", "Título", "Estado");
                    var rows = decisionRepository.findByEventId(event.getId());
                    if (rows.isEmpty()) {
                        appendRow(sb, "Sin registros para esta sección.");
                    } else {
                        rows.forEach(d -> appendRow(sb,
                                areaLabel(d.getArea().name()),
                                d.getTitle(),
                                decisionStatusLabel(d.getStatus().name())));
                    }
                    sb.append("\r\n");
                }
                case "INCIDENTS" -> {
                    appendRow(sb, "INCIDENCIAS");
                    appendRow(sb, "Descripción", "Estado");
                    var rows = incidentRepository.findByEventId(event.getId());
                    if (rows.isEmpty()) {
                        appendRow(sb, "Sin registros para esta sección.");
                    } else {
                        rows.forEach(i -> appendRow(sb,
                                i.getDescription(),
                                incidentStatusLabel(i.getStatus().name())));
                    }
                    sb.append("\r\n");
                }
                default -> { }
            }
        }

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, result, UTF8_BOM.length, body.length);
        return result;
    }

    private void appendRow(StringBuilder sb, String... values) {
        sb.append(Arrays.stream(values).map(this::esc).collect(Collectors.joining(String.valueOf(CSV_DELIMITER))));
        sb.append("\r\n");
    }

    private String esc(String value) {
        if (value == null) return "";
        if (value.indexOf(CSV_DELIMITER) >= 0 || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
