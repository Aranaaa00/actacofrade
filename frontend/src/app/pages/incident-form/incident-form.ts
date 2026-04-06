import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { IncidentService } from '../../services/incident.service';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../models/user.model';
import { Banner } from '../../shared/components/banner/banner';
import { FormField } from '../../shared/components/form-field/form-field';
import { hasFieldError, getFieldError } from '../../shared/utils/form-validation.utils';

@Component({
  selector: 'app-incident-form',
  imports: [ReactiveFormsModule, Banner, FormField],
  templateUrl: './incident-form.html',
})
export class IncidentForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly incidentService = inject(IncidentService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);

  form!: FormGroup;
  users: UserResponse[] = [];
  eventId!: number;
  loading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('eventId'));

    this.form = this.fb.group({
      description: ['', [Validators.required]],
      reportedById: [null]
    });

    this.userService.findAll().subscribe({
      next: (users) => this.users = users
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
    } else {
      this.loading = true;
      this.errorMessage = '';
      this.successMessage = '';

      this.incidentService.create(this.eventId, this.form.value).subscribe({
        next: () => {
          this.successMessage = 'Incidencia registrada correctamente.';
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
    return hasFieldError(this.form, field);
  }

  getError(field: string): string {
    return getFieldError(this.form, field);
  }

  goBack(): void {
    this.router.navigate(['/events', this.eventId]);
  }
}
