import { Component, OnInit, inject } from '@angular/core';

import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../../services/event.service';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../models/user.model';

@Component({
  selector: 'app-event-form',
  imports: [ReactiveFormsModule],
  templateUrl: './event-form.html',
  styleUrl: './event-form.scss',
})
export class EventForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly eventService = inject(EventService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);

  form!: FormGroup;
  users: UserResponse[] = [];
  isEditMode = false;
  eventId: number | null = null;
  loading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
      eventType: ['', [Validators.required]],
      eventDate: ['', [Validators.required]],
      location: ['', [Validators.maxLength(255)]],
      responsibleId: [null],
      observations: ['']
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

  private loadEvent(id: number): void {
    this.loading = true;
    this.eventService.findById(id).subscribe({
      next: (event) => {
        this.form.patchValue(event);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar el evento.';
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

      const request$ = this.isEditMode
        ? this.eventService.update(this.eventId!, this.form.value)
        : this.eventService.create(this.form.value);

      request$.subscribe({
        next: () => {
          this.successMessage = this.isEditMode
            ? 'Evento actualizado correctamente.'
            : 'Evento creado correctamente.';
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Ha ocurrido un error. Inténtalo de nuevo.';
          this.loading = false;
        }
      });
    }
  }

  hasError(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && control.touched);
  }

  getError(field: string): string {
    const control = this.form.get(field);
    let message = '';
    if (control?.errors) {
      if (control.errors['required']) {
        message = 'Este campo es obligatorio.';
      } else if (control.errors['maxlength']) {
        message = `Máximo ${control.errors['maxlength'].requiredLength} caracteres.`;
      }
    }
    return message;
  }
}
