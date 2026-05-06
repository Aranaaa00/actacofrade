import { Component, OnInit, inject, Input, Output, EventEmitter } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../../services/event.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { UserResponse } from '../../models/user.model';
import { EventResponse } from '../../models/event.model';
import { ModalOverlay } from '../../shared/components/modal-overlay/modal-overlay';
import { FormField } from '../../shared/components/form-field/form-field';
import { Datepicker } from '../../shared/components/datepicker/datepicker';
import { AutofocusDirective } from '../../shared/directives/autofocus.directive';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { noHtmlValidator, sanitizeFormValues } from '../../shared/utils/sanitize.utils';

@Component({
  selector: 'app-act-editor',
  imports: [ReactiveFormsModule, ModalOverlay, FormField, Datepicker, AutofocusDirective],
  templateUrl: './act-editor.html',
})
export class ActEditor implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly eventService = inject(EventService);
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  @Input() embedded = false;
  @Output() editorClosed = new EventEmitter<void>();
  @Output() actCreated = new EventEmitter<EventResponse>();

  form!: FormGroup;
  users: UserResponse[] = [];
  isEditMode = false;
  eventId: number | null = null;
  loading = false;

  ngOnInit(): void {
    // build the reactive form with shared validators that block html injection
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255), noHtmlValidator()]],
      eventType: ['', [Validators.required]],
      eventDate: ['', [Validators.required]],
      location: ['', [Validators.maxLength(255), noHtmlValidator()]],
      responsibleId: [null, [Validators.required]],
      observations: ['', [Validators.maxLength(1000), noHtmlValidator()]]
    });

    this.loadUsers();

    // when the route has an id we switch to edit mode and prefill the form
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.eventId = Number(id);
      this.loadEvent(this.eventId);
    }
  }

  get canPickResponsible(): boolean {
    return this.auth.isAdmin();
  }

  private loadUsers(): void {
    if (this.canPickResponsible) {
      this.userService.findAssignable().subscribe({
        next: (users) => this.users = users.filter(u => u.roles.some(r => r === 'ADMINISTRADOR' || r === 'RESPONSABLE')),
        error: (err) => {
          // Keep the form usable but warn the responsible user list could not be loaded.
          this.users = [];
          this.toast.fromHttpError(err, 'No se pudo cargar la lista de responsables.');
        }
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
        this.form.patchValue({ responsibleId: authUser.userId });
      }
    }
  }

  private loadEvent(id: number): void {
    this.loading = true;
    this.eventService.findById(id).subscribe({
      next: (event) => {
        this.form.patchValue(event);
        this.loading = false;
      },
      error: (err) => {
        this.toast.fromHttpError(err, 'No se pudo cargar el acto.');
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.warning('Revisa los campos marcados antes de continuar.');
    } else {
      this.loading = true;

      const sanitized = sanitizeFormValues(this.form.value);
      const request$ = this.isEditMode
        ? this.eventService.update(this.eventId!, sanitized)
        : this.eventService.create(sanitized);

      request$.subscribe({
        next: (event) => {
          this.loading = false;
          if (this.isEditMode) {
            this.toast.success('Acto actualizado correctamente.');
          } else {
            this.toast.success('Acto creado correctamente.');
            this.actCreated.emit(event);
          }
        },
        error: (err) => {
          this.toast.fromHttpError(err, 'Ha ocurrido un error. Inténtalo de nuevo.');
          this.loading = false;
        }
      });
    }
  }

  close(): void {
    if (this.embedded) {
      this.editorClosed.emit();
    } else {
      this.router.navigate(['/events']);
    }
  }

  onDateSelected(date: string): void {
    this.form.patchValue({ eventDate: date });
    this.form.get('eventDate')?.markAsTouched();
  }

  hasError(field: string): boolean {
    return hasFieldError(this.form, field);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }
}
