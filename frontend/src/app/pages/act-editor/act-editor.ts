import { Component, OnInit, inject, ElementRef, ViewChild, AfterViewInit, Input, Output, EventEmitter } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../../services/event.service';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../models/user.model';
import { EventResponse } from '../../models/event.model';
import { ModalOverlay } from '../../shared/components/modal-overlay/modal-overlay';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { Datepicker } from '../../shared/components/datepicker/datepicker';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';
import { noHtmlValidator, sanitizeFormValues } from '../../shared/utils/sanitize.utils';

@Component({
  selector: 'app-act-editor',
  imports: [ReactiveFormsModule, ModalOverlay, Banner, FormField, Datepicker],
  templateUrl: './act-editor.html',
})
export class ActEditor implements OnInit, AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly eventService = inject(EventService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  @ViewChild('titleInput') titleInput!: ElementRef<HTMLInputElement>;

  @Input() embedded = false;
  @Output() editorClosed = new EventEmitter<void>();
  @Output() actCreated = new EventEmitter<EventResponse>();

  form!: FormGroup;
  users: UserResponse[] = [];
  isEditMode = false;
  eventId: number | null = null;
  loading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255), noHtmlValidator()]],
      eventType: ['', [Validators.required]],
      eventDate: ['', [Validators.required]],
      location: ['', [Validators.maxLength(255), noHtmlValidator()]],
      responsibleId: [null],
      observations: ['', [Validators.maxLength(1000), noHtmlValidator()]]
    });

    this.userService.findAll().subscribe({
      next: (users) => this.users = users
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.eventId = Number(id);
      this.loadEvent(this.eventId);
    }
  }

  ngAfterViewInit(): void {
    this.titleInput?.nativeElement.focus();
  }

  private loadEvent(id: number): void {
    this.loading = true;
    this.eventService.findById(id).subscribe({
      next: (event) => {
        this.form.patchValue(event);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar el acto.';
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const sanitized = sanitizeFormValues(this.form.value);
      const request$ = this.isEditMode
        ? this.eventService.update(this.eventId!, sanitized)
        : this.eventService.create(sanitized);

      request$.subscribe({
        next: (event) => {
          if (this.isEditMode) {
            this.successMessage = 'Acto actualizado correctamente.';
            this.loading = false;
          } else {
            this.actCreated.emit(event);
          }
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Ha ocurrido un error. Inténtalo de nuevo.';
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
