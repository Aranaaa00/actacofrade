import { Component, OnInit, inject, Input, Output, EventEmitter } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TaskService } from '../../services/task.service';
import { DecisionService } from '../../services/decision.service';
import { IncidentService } from '../../services/incident.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { UserResponse } from '../../models/user.model';
import { ModalOverlay } from '../../shared/components/modal-overlay/modal-overlay';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { Datepicker } from '../../shared/components/datepicker/datepicker';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { noHtmlValidator, sanitizeText } from '../../shared/utils/sanitize.utils';

type ElementTab = 'task' | 'decision' | 'incident';

interface EditData {
  type: ElementTab;
  id: number;
  title: string;
  assignedToId: number | null;
  deadline: string;
  notes: string;
  area: string;
}

@Component({
  selector: 'app-element-form',
  imports: [ReactiveFormsModule, ModalOverlay, Banner, FormField, Datepicker],
  templateUrl: './element-form.html',
})
export class ElementForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly taskService = inject(TaskService);
  private readonly decisionService = inject(DecisionService);
  private readonly incidentService = inject(IncidentService);
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);

  @Input() eventId!: number;
  @Input() initialTab: ElementTab = 'task';
  @Input() editData: EditData | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  form!: FormGroup;
  users: UserResponse[] = [];
  activeTab: ElementTab = 'task';
  loading = false;
  errorMessage = '';
  successMessage = '';

  readonly areaOptions = [
    { value: 'MAYORDOMIA', label: 'Mayordomía' },
    { value: 'SECRETARIA', label: 'Secretaría' },
    { value: 'PRIOSTIA', label: 'Priostía' },
    { value: 'TESORERIA', label: 'Tesorería' },
    { value: 'DIPUTACION_MAYOR', label: 'Diputación Mayor' }
  ];

  get isEditMode(): boolean {
    return this.editData !== null;
  }

  get titleLabel(): string {
    const labels: Record<ElementTab, string> = {
      task: 'Descripción de la tarea',
      decision: 'Título de la decisión',
      incident: 'Descripción de la incidencia'
    };
    return labels[this.activeTab];
  }

  get titlePlaceholder(): string {
    const placeholders: Record<ElementTab, string> = {
      task: 'Escriba el título de la tarea...',
      decision: 'Escriba el título de la decisión...',
      incident: 'Describa la incidencia...'
    };
    return placeholders[this.activeTab];
  }

  get submitLabel(): string {
    if (this.isEditMode) {
      return 'Guardar cambios';
    }
    const labels: Record<ElementTab, string> = {
      task: 'Añadir tarea',
      decision: 'Añadir decisión',
      incident: 'Añadir incidencia'
    };
    return labels[this.activeTab];
  }

  ngOnInit(): void {
    this.activeTab = this.editData?.type || this.initialTab;

    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255), noHtmlValidator()]],
      assignedToId: [null],
      deadline: [''],
      notes: ['', [Validators.maxLength(1000), noHtmlValidator()]],
      area: ['MAYORDOMIA']
    });

    if (this.editData) {
      this.form.patchValue({
        title: this.editData.title,
        assignedToId: this.editData.assignedToId,
        deadline: this.editData.deadline,
        notes: this.editData.notes,
        area: this.editData.area || 'MAYORDOMIA'
      });
    }

    this.loadUsers();
  }

  private loadUsers(): void {
    if (this.auth.hasAnyRole('ADMINISTRADOR', 'RESPONSABLE')) {
      this.userService.findAll().subscribe({
        next: (users) => this.users = users
      });
    } else {
      const authUser = this.auth.getUser();
      if (authUser) {
        this.users = [{
          id: authUser.userId,
          fullName: authUser.fullName,
          email: authUser.email,
          roles: authUser.roles,
          active: true,
          lastLogin: null
        }];
        if (!this.isEditMode) {
          this.form.patchValue({ assignedToId: authUser.userId });
        }
      }
    }
  }

  switchTab(tab: ElementTab): void {
    if (!this.isEditMode) {
      this.activeTab = tab;
      this.form.reset({ title: '', assignedToId: null, deadline: '', notes: '', area: 'MAYORDOMIA' });
      this.errorMessage = '';
      this.successMessage = '';
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';
      this.submitByTab();
    }
  }

  onDeadlineSelected(date: string): void {
    this.form.patchValue({ deadline: date });
    this.form.get('deadline')?.markAsTouched();
  }

  hasError(field: string): boolean {
    return hasFieldError(this.form, field);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }

  private submitByTab(): void {
    const val = this.form.value;

    switch (this.activeTab) {
      case 'task':
        this.submitTask(val);
        break;
      case 'decision':
        this.submitDecision(val);
        break;
      case 'incident':
        this.submitIncident(val);
        break;
    }
  }

  private submitTask(val: Record<string, unknown>): void {
    const payload = {
      title: sanitizeText(val['title'] as string),
      description: sanitizeText(val['notes'] as string || ''),
      assignedToId: val['assignedToId'] as number | null,
      deadline: val['deadline'] as string || null
    };

    const request$ = this.isEditMode
      ? this.taskService.update(this.eventId, this.editData!.id, payload)
      : this.taskService.create(this.eventId, payload);

    request$.subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err)
    });
  }

  private submitDecision(val: Record<string, unknown>): void {
    const payload = {
      area: val['area'] as string,
      title: sanitizeText(val['title'] as string),
      reviewedById: val['assignedToId'] as number | null
    };

    const request$ = this.isEditMode
      ? this.decisionService.update(this.eventId, this.editData!.id, payload)
      : this.decisionService.create(this.eventId, payload);

    request$.subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err)
    });
  }

  private submitIncident(val: Record<string, unknown>): void {
    const payload = {
      description: sanitizeText(val['title'] as string),
      reportedById: val['assignedToId'] as number | null
    };

    const request$ = this.incidentService.create(this.eventId, payload);

    request$.subscribe({
      next: () => this.onSuccess(),
      error: (err) => this.onError(err)
    });
  }

  private onSuccess(): void {
    this.loading = false;
    this.saved.emit();
  }

  private onError(err: { error?: { message?: string } }): void {
    this.errorMessage = err.error?.message || 'Ha ocurrido un error. Inténtalo de nuevo.';
    this.loading = false;
  }
}
