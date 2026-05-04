// Formats an ISO date as a short Spanish date or returns a dash placeholder.
export function formatDate(dateStr: string | null): string {
  if (!dateStr) {
    return '—';
  }
  const date = new Date(dateStr);
  return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
}

// Formats an ISO date including the time of day in 24h format.
export function formatDateTime(dateStr: string | null): string {
  if (!dateStr) {
    return '—';
  }
  const date = new Date(dateStr);
  return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
