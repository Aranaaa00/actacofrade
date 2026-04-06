import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.html',
})
export class FormField {
  @Input() label = '';
  @Input() fieldId = '';
  @Input() hasError = false;
  @Input() errorMessage = '';
  @Input() styleClass = '';
}
