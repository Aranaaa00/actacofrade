import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Badge } from '../../shared/components/badge/badge';
import { ActEditor } from '../act-editor/act-editor';

interface MockAct {
  id: number;
  reference: string;
  title: string;
  eventType: string;
  eventDate: string;
  eventIso: string;
  responsibleName: string;
  status: string;
  statusLabel: string;
  currentStep: number;
}

@Component({
  selector: 'app-act-list',
  imports: [RouterLink, Badge, LucideAngularModule, ActEditor],
  templateUrl: './act-list.html',
})
export class ActList {
  readonly stepLabels = ['Planificación', 'Preparación', 'Confirmación', 'Cierre'];
  readonly pageSize = 5;
  currentPage = 1;
  showNewActModal = false;
  openDropdown: string | null = null;
  filterType = '';
  filterStatus = '';
  filterDate = '';
  searchQuery = '';

  readonly acts: MockAct[] = [
    {
      id: 1,
      reference: '2026/0012',
      title: 'Cabildo General Ordinario de Cuentas',
      eventType: 'CABILDO',
      eventDate: '15 Mar 2026',
      eventIso: '2026-03-15',
      responsibleName: 'Mayordomía',
      status: 'EN_PREPARACION',
      statusLabel: 'En preparación',
      currentStep: 2
    },
    {
      id: 2,
      reference: '2026/0015',
      title: 'Solemne Quinario al Stmo. Cristo',
      eventType: 'CULTOS',
      eventDate: '18-22 Mar 2026',
      eventIso: '2026-03-18',
      responsibleName: 'Priostra',
      status: 'COMPLETADO',
      statusLabel: 'Completado',
      currentStep: 4
    },
    {
      id: 3,
      reference: '2026/0012',
      title: 'Estación de Penitencia Jueves Santo',
      eventType: 'PROCESION',
      eventDate: '02 Abril 2026',
      eventIso: '2026-04-02',
      responsibleName: 'Dip. Mayor',
      status: 'ABIERTO',
      statusLabel: 'Abierto',
      currentStep: 3
    },
    {
      id: 4,
      reference: '2026/0012',
      title: 'Mudá y Primer Ensayo de Costaleros',
      eventType: 'ENSAYO',
      eventDate: '20 Feb 2026',
      eventIso: '2026-02-20',
      responsibleName: 'Mayordomía',
      status: 'CERRADO',
      statusLabel: 'Cerrado',
      currentStep: 4
    }
  ];

  get minEventDate(): string {
    const dates = this.acts.map(a => a.eventIso).sort();
    return dates[0] || '';
  }

  get maxEventDate(): string {
    const dates = this.acts.map(a => a.eventIso).sort();
    return dates[dates.length - 1] || '';
  }

  get filteredActs(): MockAct[] {
    return this.acts.filter(act => {
      const matchesType = !this.filterType || act.eventType === this.filterType;
      const matchesStatus = !this.filterStatus || act.statusLabel === this.filterStatus;
      const matchesDate = !this.filterDate || act.eventIso === this.filterDate;
      const query = this.searchQuery.toLowerCase();
      const matchesSearch = !query || act.title.toLowerCase().includes(query)
        || act.reference.toLowerCase().includes(query)
        || act.responsibleName.toLowerCase().includes(query)
        || act.eventType.toLowerCase().includes(query);
      return matchesType && matchesStatus && matchesDate && matchesSearch;
    });
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredActs.length / this.pageSize));
  }

  get paginatedActs(): MockAct[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredActs.slice(start, start + this.pageSize);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get activeTypeLabel(): string {
    const map: Record<string, string> = {
      'CABILDO': 'Cabildo',
      'CULTOS': 'Cultos',
      'PROCESION': 'Procesión',
      'ENSAYO': 'Ensayo',
      'OTRO': 'Otro',
    };
    return this.filterType ? (map[this.filterType] || this.filterType) : 'Tipo de acto';
  }

  get activeStatusLabel(): string {
    return this.filterStatus || 'Estado';
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  openNewActModal(): void {
    this.showNewActModal = true;
  }

  closeNewActModal(): void {
    this.showNewActModal = false;
  }

  toggleDropdown(name: string): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectFilter(type: string, value: string): void {
    if (type === 'type') {
      this.filterType = value;
    } else if (type === 'status') {
      this.filterStatus = value;
    }
    this.currentPage = 1;
    this.openDropdown = null;
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.currentPage = 1;
  }

  onDateChange(date: string): void {
    this.filterDate = date;
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.filterType = '';
    this.filterStatus = '';
    this.filterDate = '';
    this.searchQuery = '';
    this.currentPage = 1;
    this.openDropdown = null;
  }

  getStatusBadgeVariant(status: string): string {
    const variantMap: Record<string, string> = {
      'EN_PREPARACION': 'pending',
      'COMPLETADO': 'confirmed',
      'ABIERTO': 'neutral',
      'CERRADO': 'neutral',
    };
    return variantMap[status] || 'neutral';
  }

  isStepDone(currentStep: number, stepIndex: number): boolean {
    return stepIndex < currentStep;
  }
}
