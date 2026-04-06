import { Component, OnInit, inject } from '@angular/core';

import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DecisionService } from '../../services/decision.service';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../models/user.model';

@Component({
  selector: 'app-decision-form',
  imports: [ReactiveFormsModule],
  templateUrl: './decision-form.html',
  styleUrl: './decision-form.scss',
})
export class DecisionForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly decisionService = inject(DecisionService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);

  form!: FormGroup;
  users: UserResponse[] = [];
  isEditMode = false;
  eventId!: number;
  decisionId: number | null = null;
  loading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('eventId'));

    this.form = this.fb.group({
      area: ['', [Validators.required]],
      title: ['', [Validators.required, Validators.maxLength(255)]],
      reviewedById: [null]
    });

    this.userService.findAll().subscribe({
      next: (users) => this.users = users
    });

    const decisionId = this.route.snapshot.paramMap.get('decisionId');
    if (decisionId) {
      this.isEditMode = true;
      this.decisionId = Number(decisionId);
      this.loadDecision();
    }
  }

  private loadDecision(): void {
    this.loading = true;
    this.decisionService.findById(this.eventId, this.decisionId!).subscribe({
      next: (decision) => {
        this.form.patchValue(decision);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar la decisión.';
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
        ? this.decisionService.update(this.eventId, this.decisionId!, this.form.value)
        : this.decisionService.create(this.eventId, this.form.value);

      request$.subscribe({
        next: () => {
          this.successMessage = this.isEditMode
            ? 'Decisión actualizada correctamente.'
            : 'Decisión creada correctamente.';
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

  goBack(): void {
    this.router.navigate(['/events', this.eventId]);
  }
}
